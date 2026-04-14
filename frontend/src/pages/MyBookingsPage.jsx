import { useEffect, useState, useMemo } from 'react'
import Layout from '../components/layout/layout'
import { cancelBooking, getMyBookings, rateParking } from '../services/booking'
import '../styles/auth.css'

function fmt(dt) {
    if (!dt) return ''
    return dt.replace('T', ' ').slice(0, 16)
}

function statusBadgeStyle(status) {
    const s = String(status || '').toUpperCase()

    if (s === 'PENDING') return { background: '#FEF3C7', color: '#92400E' }
    if (s === 'APPROVED') return { background: '#DCFCE7', color: '#166534' }
    if (s === 'REJECTED') return { background: '#FEE2E2', color: '#991B1B' }
    if (s === 'CANCELLED') return { background: '#E2E8F0', color: '#0f172a' }

    return { background: '#E2E8F0', color: '#0f172a' }
}

function extractErrorMessage(e, fallback) {
    const data = e?.response?.data
    if (typeof data === 'string') return data
    if (data && typeof data === 'object') {
        if (data.message) return String(data.message)
        if (data.error) return String(data.error)
    }
    if (e?.message) return String(e.message)
    return fallback
}

function canCancelBooking(b) {
    const status = String(b?.status || '').toUpperCase()
    if (status !== 'PENDING') return false
    if (!b?.startTime) return false

    const start = new Date(b.startTime)
    if (Number.isNaN(start.getTime())) return false

    return start > new Date()
}

export default function MyBookingsPage() {
    const [loading, setLoading] = useState(true)
    const [savingId, setSavingId] = useState(null)
    const [items, setItems] = useState([])
    const [feedback, setFeedback] = useState({ message: '', isError: false })
    const [ratingByBooking, setRatingByBooking] = useState({})
    const [ratingSavingId, setRatingSavingId] = useState(null)
    const [showSuccessModal, setShowSuccessModal] = useState(false)

    const upcomingCount = useMemo(() => {
        const now = new Date()
        return items.filter(b => {
            const isFuture = b.startTime && new Date(b.startTime) > now
            const isActive = b.status === 'APPROVED' || b.status === 'PENDING'
            return isFuture && isActive
        }).length
    }, [items])

    const load = async () => {
        setLoading(true)
        setFeedback({ message: '', isError: false })
        try {
            const data = await getMyBookings()
            setItems(Array.isArray(data) ? data : [])
        } catch (e) {
            setFeedback({
                message: extractErrorMessage(e, 'Failed to load bookings.'),
                isError: true,
            })
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => {
        load()
    }, [])

    const doCancel = async (id) => {
        setSavingId(id)
        setFeedback({ message: '', isError: false })
        try {
            await cancelBooking(id)
            await load()
            setFeedback({ message: 'Booking cancelled.', isError: false })
        } catch (e) {
            setFeedback({
                message: extractErrorMessage(e, 'Failed to cancel booking.'),
                isError: true,
            })
        } finally {
            setSavingId(null)
        }
    }

    const isApprovedBooking = (b) =>
        String(b?.status || '').toUpperCase() === 'APPROVED'

    const handleRatingChange = (bookingId, value) => {
        setRatingByBooking((prev) => ({
            ...prev,
            [bookingId]: value,
        }))
    }

    const doRate = async (booking) => {
        const selected = Number(ratingByBooking[booking.id])

        if (!selected || selected < 1 || selected > 5) {
            setFeedback({
                message: 'Please choose a rating between 1 and 5.',
                isError: true,
            })
            return
        }

        setRatingSavingId(booking.id)
        setFeedback({ message: '', isError: false })

        try {
            await rateParking(booking.parkingId, selected)

            setShowSuccessModal(true)


            setTimeout(() => {
                setShowSuccessModal(false)
            }, 3000)
        } catch (e) {
            setFeedback({
                message: extractErrorMessage(e, 'Failed to submit rating.'),
                isError: true,
            })
        } finally {
            setRatingSavingId(null)
        }
    }

    return (
        <Layout title="">
            <div className="auth-wrap" style={{ minHeight: 'calc(100vh - 80px)', position: 'relative' }}>
                <div className="auth-card" style={{ maxWidth: 860, textAlign: 'left' }}>

                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
                        <h1 className="auth-title" style={{ margin: 0 }}>
                            My Bookings
                        </h1>
                    </div>

                    <div style={{ display: 'flex', gap: '16px', marginBottom: '24px' }}>
                        <div style={{ flex: 1, background: '#f8fafc', border: '1px solid #e2e8f0', borderRadius: '12px', padding: '16px', display: 'flex', flexDirection: 'column', gap: '8px' }}>
                            <span style={{ fontSize: '12px', fontWeight: '800', color: '#64748b', textTransform: 'uppercase' }}>Upcoming Bookings</span>
                            <span style={{ fontSize: '32px', fontWeight: '900', color: '#0f172a' }}>{upcomingCount}</span>
                        </div>

                        <div style={{ flex: 1, background: '#f8fafc', border: '1px solid #e2e8f0', borderRadius: '12px', padding: '16px', display: 'flex', flexDirection: 'column', gap: '8px' }}>
                            <span style={{ fontSize: '12px', fontWeight: '800', color: '#64748b', textTransform: 'uppercase' }}>Total Bookings</span>
                            <span style={{ fontSize: '32px', fontWeight: '900', color: '#0f172a' }}>{items.length}</span>
                        </div>
                    </div>

                    {feedback.message && (
                        <div
                            style={{
                                marginTop: 14,
                                padding: 10,
                                borderRadius: 10,
                                background: feedback.isError ? '#fee2e2' : '#dcfce7',
                                color: feedback.isError ? '#991b1b' : '#166534',
                                fontWeight: 700,
                            }}
                        >
                            {feedback.message}
                        </div>
                    )}

                    {loading ? (
                        <div style={{ padding: 10 }}>Loading...</div>
                    ) : items.length === 0 ? (
                        <div style={{ padding: 10, fontWeight: 700, color: '#334155' }}>
                            No bookings yet.
                        </div>
                    ) : (
                        <div style={{ marginTop: 12, display: 'grid', gap: 10 }}>
                            {items.map((b) => {
                                const canCancel = canCancelBooking(b)
                                const isApproved = String(b.status || '').toUpperCase() === 'APPROVED'

                                return (
                                    <div
                                        key={b.id}
                                        style={{
                                            border: '1px solid rgba(0,0,0,0.12)',
                                            borderRadius: 12,
                                            padding: 16,
                                            display: 'grid',
                                            gap: 8,
                                        }}
                                    >
                                        <div style={{ display: 'flex', justifyContent: 'space-between', gap: 10 }}>
                                            <div style={{ fontWeight: 900, display: 'flex', alignItems: 'center', gap: 10 }}>
                                                <span style={{ color: '#0f172a', fontSize: '18px' }}>Booking #{b.id}</span>

                                                <span
                                                    style={{
                                                        ...statusBadgeStyle(b.status),
                                                        padding: '6px 12px',
                                                        borderRadius: 999,
                                                        fontWeight: 900,
                                                        fontSize: 12,
                                                        lineHeight: '12px',
                                                        display: 'inline-flex',
                                                        alignItems: 'center',
                                                    }}
                                                >
                                                    {b.status}
                                                </span>
                                            </div>

                                            <div style={{ fontWeight: 900, fontSize: '18px', color: '#0f172a' }}>
                                                {b.totalPrice != null ? `₪${b.totalPrice}` : ''}
                                            </div>
                                        </div>

                                        <div style={{ color: '#334155', fontWeight: 700, marginTop: '4px' }}>
                                            Parking: {b.parkingLocation || '—'}
                                            <span style={{ marginLeft: 8, color: '#64748b', fontWeight: 600, fontSize: 13 }}>
                                                (ID: {b.parkingId})
                                            </span>
                                        </div>

                                        <div style={{ color: '#475569', fontSize: '15px' }}>
                                            <strong>Start:</strong> {fmt(b.startTime)} &nbsp;&nbsp;|&nbsp;&nbsp;
                                            <strong>End:</strong> {fmt(b.endTime)}
                                        </div>

                                        {isApprovedBooking(b) && (
                                            <div
                                                style={{
                                                    marginTop: 12,
                                                    padding: 16,
                                                    borderRadius: 12,
                                                    background: '#f8fafc',
                                                    border: '1px solid #e2e8f0',
                                                    display: 'flex',
                                                    alignItems: 'center',
                                                    justifyContent: 'space-between',
                                                    gap: 16,
                                                    flexWrap: 'wrap',
                                                }}
                                            >
                                                <div style={{ fontWeight: 800, color: '#334155', fontSize: '15px' }}>
                                                    Rate your experience
                                                </div>

                                                <div style={{ display: 'flex', alignItems: 'center', gap: '16px', flexWrap: 'wrap' }}>
                                                    <div style={{ display: 'flex', gap: '6px' }}>
                                                        {[1, 2, 3, 4, 5].map((num) => {
                                                            const isSelected = Number(ratingByBooking[b.id]) === num;
                                                            return (
                                                                <button
                                                                    key={num}
                                                                    type="button"
                                                                    onClick={() => handleRatingChange(b.id, num)}
                                                                    style={{
                                                                        width: '36px',
                                                                        height: '36px',
                                                                        borderRadius: '10px',
                                                                        border: isSelected ? '2px solid #2563eb' : '1px solid #e2e8f0',
                                                                        backgroundColor: isSelected ? '#eff6ff' : '#ffffff',
                                                                        color: isSelected ? '#1d4ed8' : '#64748b',
                                                                        fontWeight: 'bold',
                                                                        fontSize: '15px',
                                                                        cursor: 'pointer',
                                                                        transition: 'all 0.2s',
                                                                        display: 'flex',
                                                                        alignItems: 'center',
                                                                        justifyContent: 'center',
                                                                        padding: 0
                                                                    }}
                                                                    onMouseOver={(e) => { if (!isSelected) e.currentTarget.style.backgroundColor = '#f1f5f9' }}
                                                                    onMouseOut={(e) => { if (!isSelected) e.currentTarget.style.backgroundColor = '#ffffff' }}
                                                                >
                                                                    {num}
                                                                </button>
                                                            )
                                                        })}
                                                    </div>

                                                    <button
                                                        type="button"
                                                        disabled={ratingSavingId === b.id}
                                                        onClick={() => doRate(b)}
                                                        style={{
                                                            backgroundColor: '#2563eb',
                                                            color: 'white',
                                                            border: 'none',
                                                            padding: '10px 18px',
                                                            borderRadius: '10px',
                                                            cursor: ratingSavingId === b.id ? 'not-allowed' : 'pointer',
                                                            fontWeight: 'bold',
                                                            fontSize: '14px',
                                                            boxShadow: '0 4px 6px -1px rgba(37, 99, 235, 0.2)',
                                                            transition: 'opacity 0.2s, transform 0.1s',
                                                            opacity: ratingSavingId === b.id ? 0.7 : 1,
                                                        }}
                                                        onMouseOver={(e) => { if (ratingSavingId !== b.id) e.currentTarget.style.opacity = '0.9' }}
                                                        onMouseOut={(e) => { if (ratingSavingId !== b.id) e.currentTarget.style.opacity = '1' }}
                                                        onMouseDown={(e) => { if (ratingSavingId !== b.id) e.currentTarget.style.transform = 'scale(0.98)' }}
                                                        onMouseUp={(e) => { if (ratingSavingId !== b.id) e.currentTarget.style.transform = 'scale(1)' }}
                                                    >
                                                        {ratingSavingId === b.id ? 'Saving...' : 'Submit Rating'}
                                                    </button>
                                                </div>
                                            </div>
                                        )}

                                        {isApproved && (
                                            <div style={{
                                                marginTop: 8,
                                                padding: '10px 14px',
                                                backgroundColor: '#f0fdf4',
                                                border: '1px solid #bbf7d0',
                                                borderRadius: '10px',
                                                color: '#166534',
                                                fontSize: '14px',
                                                display: 'flex',
                                                alignItems: 'center',
                                                gap: '8px'
                                            }}>
                                                <span style={{ fontSize: '18px' }}>📞</span>
                                                <span><strong>Owner Contact:</strong> {b.ownerPhone || 'Not available'}</span>
                                            </div>
                                        )}

                                        <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 12 }}>
                                            <button
                                                type="button"
                                                disabled={!canCancel || savingId === b.id}
                                                title={!canCancel ? 'Cancellation is allowed only before the booking start time.' : ''}
                                                onClick={() => doCancel(b.id)}
                                                style={{
                                                    width: 140,
                                                    backgroundColor: canCancel ? '#ef4444' : '#f1f5f9',
                                                    color: canCancel ? 'white' : '#94a3b8',
                                                    border: canCancel ? 'none' : '1px solid #e2e8f0',
                                                    padding: '10px 16px',
                                                    borderRadius: '10px',
                                                    cursor: canCancel ? 'pointer' : 'not-allowed',
                                                    fontWeight: 'bold',
                                                    fontSize: '14px',
                                                    boxShadow: canCancel ? '0 4px 6px -1px rgba(239, 68, 68, 0.2)' : 'none',
                                                    transition: 'all 0.2s',
                                                }}
                                                onMouseOver={(e) => { if (canCancel && savingId !== b.id) e.currentTarget.style.opacity = '0.9' }}
                                                onMouseOut={(e) => { if (canCancel && savingId !== b.id) e.currentTarget.style.opacity = '1' }}
                                                onMouseDown={(e) => { if (canCancel && savingId !== b.id) e.currentTarget.style.transform = 'scale(0.98)' }}
                                                onMouseUp={(e) => { if (canCancel && savingId !== b.id) e.currentTarget.style.transform = 'scale(1)' }}
                                            >
                                                {savingId === b.id ? 'Cancelling...' : 'Cancel'}
                                            </button>
                                        </div>
                                    </div>
                                )
                            })}
                        </div>
                    )}
                </div>


                {showSuccessModal && (
                    <div style={{
                        position: 'fixed',
                        inset: 0,
                        backgroundColor: 'rgba(0,0,0,0.4)',
                        zIndex: 9999,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        backdropFilter: 'blur(2px)'
                    }}>
                        <div style={{
                            backgroundColor: 'white',
                            padding: '30px',
                            borderRadius: '16px',
                            boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04)',
                            textAlign: 'center',
                            maxWidth: '320px',
                            width: '90%',
                            animation: 'popIn 0.3s ease-out'
                        }}>
                            <div style={{ fontSize: '48px', marginBottom: '16px', lineHeight: '1' }}>✅</div>
                            <h3 style={{ margin: '0 0 12px 0', color: '#0f172a', fontSize: '20px', fontWeight: 'bold' }}>Thank You!</h3>
                            <p style={{ color: '#475569', marginBottom: '24px', fontSize: '15px' }}>
                                Your rating was submitted successfully.
                            </p>
                            <button
                                onClick={() => setShowSuccessModal(false)}
                                style={{
                                    backgroundColor: '#2563eb',
                                    color: 'white',
                                    border: 'none',
                                    padding: '10px 24px',
                                    borderRadius: '10px',
                                    cursor: 'pointer',
                                    fontWeight: 'bold',
                                    fontSize: '15px',
                                    width: '100%',
                                    transition: 'background-color 0.2s'
                                }}
                                onMouseOver={(e) => e.currentTarget.style.backgroundColor = '#1d4ed8'}
                                onMouseOut={(e) => e.currentTarget.style.backgroundColor = '#2563eb'}
                            >
                                Close
                            </button>
                        </div>
                    </div>
                )}
            </div>


            <style>{`
                @keyframes popIn {
                    0% { transform: scale(0.9); opacity: 0; }
                    100% { transform: scale(1); opacity: 1; }
                }
            `}</style>
        </Layout>
    )
}