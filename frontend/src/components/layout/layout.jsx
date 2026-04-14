import { useEffect, useRef, useState } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { getCurrentUser, logout, subscribeAuthChanged } from '../../services/session'
import '../../styles/layout.css'
import ProfileModal from '../modals/ProfileModal'

function IconUser({ size = 18 }) {
    return (
        <svg width={size} height={size} viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <path
                d="M12 12a4.2 4.2 0 1 0-4.2-4.2A4.2 4.2 0 0 0 12 12Z"
                stroke="currentColor"
                strokeWidth="1.8"
            />
            <path
                d="M4.5 20.2c1.4-4.2 13.6-4.2 15 0"
                stroke="currentColor"
                strokeWidth="1.8"
                strokeLinecap="round"
            />
        </svg>
    )
}

export default function Layout({ title, children }) {
    const nav = useNavigate()
    const location = useLocation()

    // Profile menu (Driver-style)
    const [profileMenuOpen, setProfileMenuOpen] = useState(false)
    const profileBtnRef = useRef(null)
    const [profileMenuPos, setProfileMenuPos] = useState({ top: 0, left: 0 })

    // (kept for future use) modal that edits profile/role
    const [profileModalOpen, setProfileModalOpen] = useState(false)
    const [user, setUser] = useState(getCurrentUser())

    // Responsive label for Back button
    const [isNarrowMobile, setIsNarrowMobile] = useState(() => {
        if (typeof window === 'undefined') return false
        return window.innerWidth < 480
    })

    useEffect(() => {
        const onResize = () => setIsNarrowMobile(window.innerWidth < 480)
        window.addEventListener('resize', onResize)
        return () => window.removeEventListener('resize', onResize)
    }, [])

    useEffect(() => {
        // initial sync (covers cases where storage changed before mount)
        setUser(getCurrentUser())

        // re-render when auth changes
        return subscribeAuthChanged(() => {
            setUser(getCurrentUser())
        })
    }, [])

    const roles = new Set(user?.roles ?? [])

    // Auth screens should look like a mobile app landing (no header/top nav)
    const isAuthRoute = ['/', '/login', '/register', '/reset-password'].includes(location.pathname)

    // Show in all pages except DRIVER (and only when logged-in)
    const showBackToSearch = !isAuthRoute && location.pathname !== '/driver' && !!user

    useEffect(() => {
        setProfileMenuOpen(false)
    }, [location.pathname])

    const openProfileMenu = () => {
        const el = profileBtnRef.current
        if (el) {
            const r = el.getBoundingClientRect()
            const menuWidth = 220
            const gap = 10
            const left = Math.min(window.innerWidth - menuWidth - 12, Math.max(12, r.right - menuWidth))
            const top = r.bottom + gap
            setProfileMenuPos({ top, left })
        }
        setProfileMenuOpen(true)
    }

    useEffect(() => {
        if (!profileMenuOpen) return

        const onResizeOrScroll = () => {
            const el = profileBtnRef.current
            if (!el) return
            const r = el.getBoundingClientRect()
            const menuWidth = 220
            const gap = 10
            const left = Math.min(window.innerWidth - menuWidth - 12, Math.max(12, r.right - menuWidth))
            const top = r.bottom + gap
            setProfileMenuPos({ top, left })
        }

        window.addEventListener('resize', onResizeOrScroll)
        window.addEventListener('scroll', onResizeOrScroll, true)
        return () => {
            window.removeEventListener('resize', onResizeOrScroll)
            window.removeEventListener('scroll', onResizeOrScroll, true)
        }
    }, [profileMenuOpen])

    return (
        <div className={isAuthRoute ? 'ep-app ep-app-auth' : 'ep-app'}>
            {!isAuthRoute && (
                <header
                    className="ep-header"
                    style={{
                        position: 'sticky',
                        top: 0,
                        zIndex: 20,
                        padding: '12px 14px',
                        background: 'rgba(255,255,255,0.92)',
                        backdropFilter: 'blur(10px)',
                        boxShadow: '0 8px 30px rgba(15, 23, 42, 0.12)',
                        borderBottom: 'none',
                        height: 'auto',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                    }}
                >
                    {/* Brand - EXACT like Driver */}
                    <div
                        onClick={() => nav('/driver')}
                        role="button"
                        style={{
                            display: 'flex',
                            alignItems: 'center',
                            gap: 10,
                            cursor: 'pointer',
                            userSelect: 'none',
                        }}
                    >
                        <span
                            aria-hidden="true"
                            style={{
                                width: 42,
                                height: 42,
                                borderRadius: 999,
                                background: 'rgb(37,99,235)',
                                display: 'grid',
                                placeItems: 'center',
                                color: 'white',
                                overflow: 'hidden',
                            }}
                        >
                            <img
                                src="Logo_notext.png"
                                alt="Logo"
                                style={{ width: 41, height: 50, display: 'block', marginLeft: -1 }}
                            />
                        </span>

                        <div style={{ fontWeight: 900, fontSize: 18, color: '#0549fa' }}>EasyPark</div>
                    </div>

                    <div className="ep-actions" style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                        {user ? (
                            <>
                                {showBackToSearch && (
                                    <button
                                        type="button"
                                        onClick={() => nav('/driver')}
                                        style={{
                                            height: 42,
                                            borderRadius: 999,
                                            border: '1px solid rgba(15, 23, 42, 0.12)',
                                            background: 'rgba(255,255,255,0.9)',
                                            padding: '0 14px',
                                            fontWeight: 800,
                                            cursor: 'pointer',
                                            color: '#1e293b',
                                            boxShadow: '0 10px 20px rgba(15, 23, 42, 0.06)',
                                        }}
                                    >
                                        {isNarrowMobile ? 'Back' : 'Back to Search for Parking'}
                                    </button>
                                )}

                                <button
                                    ref={profileBtnRef}
                                    type="button"
                                    onClick={() => {
                                        if (profileMenuOpen) setProfileMenuOpen(false)
                                        else openProfileMenu()
                                    }}
                                    aria-label="Profile menu"
                                    title="Account"
                                    style={{
                                        width: 42,
                                        height: 42,
                                        borderRadius: 999,
                                        border: 0,
                                        background: '#2563eb',
                                        color: 'white',
                                        cursor: 'pointer',
                                        display: 'grid',
                                        placeItems: 'center',
                                        boxShadow: '0 10px 20px rgba(37, 99, 235, 0.25)',
                                    }}
                                >
                                    <IconUser size={18} />
                                </button>
                            </>
                        ) : (
                            <>
                                <button className="ep-btn" onClick={() => nav('/login')}>
                                    Login
                                </button>
                                <button className="ep-btn ep-btn-primary" onClick={() => nav('/register')}>
                                    Register
                                </button>
                            </>
                        )}
                    </div>
                </header>
            )}

            <main className={isAuthRoute ? 'ep-main ep-main-auth' : 'ep-main'}>
                {isAuthRoute ? (
                    children
                ) : (
                    <div className="ep-container">
                        <h1 className="ep-title">{title}</h1>
                        {children}
                    </div>
                )}
            </main>

            <ProfileModal
                isOpen={profileModalOpen}
                onClose={() => setProfileModalOpen(false)}
                onUpdateSuccess={(updatedUser) => {
                    setProfileModalOpen(false)

                    const rolesArr = updatedUser?.roles ?? []
                    const hasDriver = rolesArr.includes('DRIVER')
                    const hasOwner = rolesArr.includes('OWNER')

                    // If BOTH: stay on the same page
                    if (hasDriver && hasOwner) return

                    // If only DRIVER: go to driver
                    if (hasDriver) nav('/driver', { replace: true })
                    // If only OWNER: go to owner
                    else if (hasOwner) nav('/manage-spots', { replace: true })
                    // fallback
                    else nav('/login', { replace: true })
                }}
            />

            {/* Driver-style profile dropdown (shared across all pages) */}
            {profileMenuOpen && !isAuthRoute && (
                <div style={{ position: 'fixed', inset: 0, zIndex: 999999, pointerEvents: 'auto' }}>
                    <button
                        type="button"
                        onClick={() => setProfileMenuOpen(false)}
                        style={{
                            position: 'absolute',
                            inset: 0,
                            background: 'transparent',
                            border: 0,
                            padding: 0,
                            margin: 0,
                        }}
                        aria-label="Close profile menu"
                    />

                    <div
                        style={{
                            position: 'absolute',
                            top: profileMenuPos.top,
                            left: profileMenuPos.left,
                            width: 220,
                            background: 'rgba(255,255,255,0.98)',
                            backdropFilter: 'blur(10px)',
                            border: '1px solid rgba(15, 23, 42, 0.10)',
                            boxShadow: '0 18px 40px rgba(15, 23, 42, 0.18)',
                            borderRadius: 14,
                            padding: 8,
                        }}
                    >
                        {roles.has('OWNER') && (
                            <button
                                type="button"
                                onClick={() => {
                                    setProfileMenuOpen(false)
                                    nav('/manage-spots')
                                }}
                                style={{
                                    width: '100%',
                                    height: 42,
                                    borderRadius: 12,
                                    border: 0,
                                    background: '#e2e8f0',
                                    textAlign: 'left',
                                    padding: '0 12px',
                                    fontWeight: 700,
                                    cursor: 'pointer',
                                    color: '#1e293b',
                                    marginBottom: '5px',
                                    transition: 'background 0.2s',
                                }}
                                onMouseOver={(e) => (e.currentTarget.style.background = '#f1f5f9')}
                                onMouseOut={(e) => (e.currentTarget.style.background = 'transparent')}
                            >
                                Manage Spots
                            </button>
                        )}

                        {roles.has('DRIVER') && (
                            <button
                                type="button"
                                onClick={() => {
                                    setProfileMenuOpen(false)
                                    nav('/my-bookings')
                                }}
                                style={{
                                    width: '100%',
                                    height: 42,
                                    borderRadius: 12,
                                    border: 0,
                                    background: 'transparent',
                                    textAlign: 'left',
                                    padding: '0 12px',
                                    fontWeight: 600,
                                    cursor: 'pointer',
                                    color: '#1e293b',
                                    marginBottom: '5px',
                                    transition: 'background 0.2s',
                                }}
                                onMouseOver={(e) => (e.currentTarget.style.background = '#f1f5f9')}
                                onMouseOut={(e) => (e.currentTarget.style.background = 'transparent')}
                            >
                                My Bookings
                            </button>
                        )}

                        {/* --- revenues / expenses BUTTONS --- */}
                        <button
                            type="button"
                            onClick={() => {
                                setProfileMenuOpen(false)
                                nav('/revenues')
                            }}
                            style={{
                                width: '100%',
                                height: 42,
                                borderRadius: 12,
                                border: 0,
                                background: 'transparent',
                                textAlign: 'left',
                                padding: '0 12px',
                                fontWeight: 600,
                                cursor: 'pointer',
                                color: '#1e293b',
                                marginBottom: '5px',
                                transition: 'background 0.2s',
                            }}
                            onMouseOver={(e) => (e.currentTarget.style.background = '#f1f5f9')}
                            onMouseOut={(e) => (e.currentTarget.style.background = 'transparent')}
                        >
                            My Revenues
                        </button>

                        <button
                            type="button"
                            onClick={() => {
                                setProfileMenuOpen(false)
                                nav('/expenses')
                            }}
                            style={{
                                width: '100%',
                                height: 42,
                                borderRadius: 12,
                                border: 0,
                                background: 'transparent',
                                textAlign: 'left',
                                padding: '0 12px',
                                fontWeight: 600,
                                cursor: 'pointer',
                                color: '#1e293b',
                                marginBottom: '5px',
                                transition: 'background 0.2s',
                            }}
                            onMouseOver={(e) => (e.currentTarget.style.background = '#f1f5f9')}
                            onMouseOut={(e) => (e.currentTarget.style.background = 'transparent')}
                        >
                            My Expenses
                        </button>

                        <button
                            type="button"
                            onClick={() => {
                                setProfileMenuOpen(false)
                                nav('/manage-profile')
                            }}
                            style={{
                                width: '100%',
                                height: 42,
                                borderRadius: 12,
                                border: 0,
                                background: 'transparent',
                                textAlign: 'left',
                                padding: '0 12px',
                                fontWeight: 600,
                                cursor: 'pointer',
                                color: '#1e293b',
                                marginBottom: '5px',
                                transition: 'background 0.2s',
                            }}
                            onMouseOver={(e) => (e.currentTarget.style.background = '#f1f5f9')}
                            onMouseOut={(e) => (e.currentTarget.style.background = 'transparent')}
                        >
                            Manage Profile
                        </button>

                        <button
                            type="button"
                            onClick={() => {
                                setProfileMenuOpen(false)
                                nav('/change-password')
                            }}
                            style={{
                                width: '100%',
                                height: 42,
                                borderRadius: 12,
                                border: 0,
                                background: 'transparent',
                                textAlign: 'left',
                                padding: '0 12px',
                                fontWeight: 600,
                                cursor: 'pointer',
                                color: '#1e293b',
                                marginBottom: '5px',
                                transition: 'background 0.2s',
                            }}
                            onMouseOver={(e) => (e.currentTarget.style.background = '#f1f5f9')}
                            onMouseOut={(e) => (e.currentTarget.style.background = 'transparent')}
                        >
                            Change Password
                        </button>

                        <button
                            type="button"
                            onClick={() => {
                                setProfileMenuOpen(false)
                                logout()
                                nav('/login')
                            }}
                            style={{
                                width: '100%',
                                height: 42,
                                borderRadius: 12,
                                border: 0,
                                background: 'rgba(239, 68, 68, 0.10)',
                                textAlign: 'left',
                                padding: '0 12px',
                                fontWeight: 900,
                                cursor: 'pointer',
                                color: '#ef4444',
                            }}
                        >
                            Logout
                        </button>
                    </div>
                </div>
            )}
        </div>
    )
}