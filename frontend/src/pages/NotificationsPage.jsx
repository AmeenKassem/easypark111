import { useEffect, useState } from 'react'
import Layout from '../components/layout/layout'
import { getCurrentUser } from '../services/session'
import {
    getMyNotifications,
    markAllNotificationsRead,
    clearNotifications,
    notifyNotificationsChanged,
    subscribeNotificationReceived,
} from '../services/notifications'
import { updateBookingStatus } from '../services/booking'

const formatDate = (iso) => {
    try {
        return new Date(iso).toLocaleString([], {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
        })
    } catch {
        return iso
    }
}

function IconCheck({ size = 20 }) {
    return (
        <svg width={size} height={size} viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <path d="M5 13l4 4L19 7" stroke="currentColor" strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
    )
}

function IconX({ size = 20 }) {
    return (
        <svg width={size} height={size} viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <path d="M6 6l12 12M18 6L6 18" stroke="currentColor" strokeWidth="2.6" strokeLinecap="round" />
        </svg>
    )
}

const STATUS_LABEL = { APPROVED: 'Approved', REJECTED: 'Rejected', CANCELLED: 'Cancelled', PENDING: 'Pending' }

const statusChipStyle = (status) => {
    const s = String(status || '').toUpperCase()
    if (s === 'APPROVED') return { background: '#DCFCE7', color: '#166534' }
    if (s === 'REJECTED') return { background: '#FEE2E2', color: '#991B1B' }
    if (s === 'CANCELLED') return { background: '#E2E8F0', color: '#475569' }
    return { background: '#FEF3C7', color: '#92400E' }
}

const actionBtnBase = (disabled) => ({
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    height: 44,
    padding: '0 18px',
    borderRadius: 12,
    border: 0,
    cursor: disabled ? 'not-allowed' : 'pointer',
    opacity: disabled ? 0.6 : 1,
    fontWeight: 800,
    fontSize: 14,
    color: 'white',
    flex: '1 1 130px', // grow to fill nicely on narrow phones, side-by-side on wider screens
    minWidth: 120,
})

export default function NotificationsPage() {
    const user = getCurrentUser()
    const [notifications, setNotifications] = useState([])
    const [loading, setLoading] = useState(true)
    const [clearLoading, setClearLoading] = useState(false)
    const [error, setError] = useState('')
    // Which notification's booking action is in flight (prevents double-clicks).
    const [actioningId, setActioningId] = useState(null)
    // Per-notification action error messages, keyed by notification id.
    const [actionErrors, setActionErrors] = useState({})

    useEffect(() => {
        if (!user?.id) return
        const loadNotifications = async () => {
            setError('')
            setLoading(true)
            try {
                const list = await getMyNotifications()
                setNotifications(list)
                await markAllNotificationsRead()
                notifyNotificationsChanged()
            } catch {
                setError('Failed to load notifications. Please try again.')
            } finally {
                setLoading(false)
            }
        }

        loadNotifications()
    }, [user?.id])

    // Real-time: while the page is open, prepend newly arrived notifications and
    // keep them read (the panel is open) so the badge stays at 0.
    useEffect(() => {
        if (!user?.id) return
        const unsubscribe = subscribeNotificationReceived((incoming) => {
            if (!incoming) return
            setNotifications((prev) => {
                if (prev.some((n) => n.id === incoming.id)) return prev
                return [{ ...incoming, read: true }, ...prev]
            })
            // Persist the read state for the just-arrived notification and let the
            // badge drop back to 0 in real time.
            markAllNotificationsRead()
                .then(() => notifyNotificationsChanged())
                .catch(() => {})
        })
        return unsubscribe
    }, [user?.id])

    const handleClear = async () => {
        if (clearLoading) return
        setError('')
        setClearLoading(true)
        try {
            await clearNotifications()
            setNotifications([])
            notifyNotificationsChanged()
        } catch {
            setError('Failed to clear notifications. Please try again.')
        } finally {
            setClearLoading(false)
        }
    }

    // Approve / reject a booking request straight from the notification.
    // Reuses the exact same /status endpoint as Manage Spots (services/booking.js),
    // so the driver still gets the existing "Booking Approved/Rejected" notification
    // and any email logic runs unchanged.
    const handleBookingAction = async (item, status) => {
        if (actioningId || !item.bookingId) return
        setActioningId(item.id)
        setActionErrors((prev) => ({ ...prev, [item.id]: '' }))
        try {
            await updateBookingStatus(item.bookingId, status)
            // Immediately reflect the result: hide the buttons, show the status label.
            setNotifications((prev) =>
                prev.map((n) =>
                    n.id === item.id ? { ...n, bookingStatus: status, actionable: false } : n,
                ),
            )
            notifyNotificationsChanged()
        } catch (e) {
            const msg =
                e?.response?.data?.message ||
                e?.response?.data ||
                e?.message ||
                'Action failed. Please try again.'
            setActionErrors((prev) => ({ ...prev, [item.id]: String(msg) }))
        } finally {
            setActioningId(null)
        }
    }

    const renderBookingActions = (item) => {
        if (item.type !== 'BOOKING_REQUEST') return null

        const busy = actioningId === item.id
        const itemError = actionErrors[item.id]

        return (
            <div style={{ marginTop: 14 }}>
                {item.actionable ? (
                    <div style={{ display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' }}>
                        <button
                            type="button"
                            onClick={() => handleBookingAction(item, 'APPROVED')}
                            disabled={busy}
                            aria-label="Approve booking request"
                            title="Approve"
                            style={{ ...actionBtnBase(busy), background: '#16a34a' }}
                        >
                            <IconCheck />
                            <span>{busy ? 'Working…' : 'Approve'}</span>
                        </button>
                        <button
                            type="button"
                            onClick={() => handleBookingAction(item, 'REJECTED')}
                            disabled={busy}
                            aria-label="Reject booking request"
                            title="Reject"
                            style={{ ...actionBtnBase(busy), background: '#ef4444' }}
                        >
                            <IconX />
                            <span>{busy ? 'Working…' : 'Reject'}</span>
                        </button>
                    </div>
                ) : item.bookingStatus ? (
                    <span
                        style={{
                            display: 'inline-flex',
                            alignItems: 'center',
                            padding: '6px 14px',
                            borderRadius: 999,
                            fontWeight: 800,
                            fontSize: 13,
                            ...statusChipStyle(item.bookingStatus),
                        }}
                    >
                        {STATUS_LABEL[String(item.bookingStatus).toUpperCase()] || item.bookingStatus}
                    </span>
                ) : null}

                {itemError && (
                    <div style={{ marginTop: 8, color: '#991b1b', fontWeight: 600, fontSize: 13 }}>{itemError}</div>
                )}
            </div>
        )
    }

    return (
        <Layout title="Notifications">
            <div className="ep-card" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 10 }}>
                <div>
                    <div style={{ fontSize: 18, fontWeight: 700 }}>Your latest notifications</div>
                    <div style={{ color: '#64748b', marginTop: 4 }}>All new notifications are shown here.</div>
                </div>
                <button className="ep-btn" type="button" onClick={handleClear} disabled={!notifications.length || clearLoading}>
                    {clearLoading ? 'Clearing…' : 'Clear all'}
                </button>
            </div>

            {error && (
                <div style={{ marginTop: 12, padding: 10, borderRadius: 10, background: '#fee2e2', color: '#991b1b', fontWeight: 600 }}>
                    {error}
                </div>
            )}

            {loading ? (
                <div style={{ marginTop: 24, padding: 24, borderRadius: 16, background: '#f8fafc', color: '#475569', fontWeight: 600, textAlign: 'center' }}>
                    Loading notifications...
                </div>
            ) : notifications.length === 0 ? (
                <div style={{ marginTop: 24, padding: 24, borderRadius: 16, background: '#f8fafc', color: '#475569', fontWeight: 600, textAlign: 'center' }}>
                    No notifications yet. New notifications will appear here.
                </div>
            ) : (
                <div style={{ marginTop: 24, display: 'grid', gap: 14 }}>
                    {notifications.map((item) => (
                        <div key={item.id} style={{ borderRadius: 18, background: item.read ? '#f8fafc' : '#eff6ff', border: '1px solid #e2e8f0', padding: 18 }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12, alignItems: 'center', marginBottom: 8 }}>
                                <div style={{ fontSize: 16, fontWeight: 700, color: '#0f172a' }}>{item.title}</div>
                                <div style={{ fontSize: 13, color: '#64748b' }}>{formatDate(item.createdAt)}</div>
                            </div>
                            <div style={{ color: '#334155', whiteSpace: 'pre-wrap', lineHeight: 1.6 }}>{item.message || 'No details available.'}</div>
                            {renderBookingActions(item)}
                        </div>
                    ))}
                </div>
            )}
        </Layout>
    )
}
