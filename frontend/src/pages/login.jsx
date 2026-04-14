import { useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { GoogleLogin } from '@react-oauth/google'
import Layout from '../components/layout/layout'
import { getCurrentUser, loginUser } from '../services/session'
import { API_BASE_URL } from '../config'
import '../styles/auth.css'

function MailIcon() {
    return (
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
            <path
                d="M4 6.5h16v11H4v-11Z"
                stroke="currentColor"
                strokeWidth="1.7"
                opacity="0.75"
            />
            <path
                d="M4.5 7l7.2 6a.7.7 0 0 0 .9 0L19.5 7"
                stroke="currentColor"
                strokeWidth="1.7"
                opacity="0.75"
            />
        </svg>
    )
}

function LockIcon() {
    return (
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
            <path
                d="M7 11V8.8C7 6.15 9.1 4 11.7 4h.6C14.9 4 17 6.15 17 8.8V11"
                stroke="currentColor"
                strokeWidth="1.7"
                opacity="0.75"
            />
            <path
                d="M6.5 11h11v9h-11v-9Z"
                stroke="currentColor"
                strokeWidth="1.7"
                opacity="0.75"
            />
        </svg>
    )
}

export default function LoginPage() {
    const nav = useNavigate()
    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const [error, setError] = useState('')
    function redirectAfterLogin() {
        const u = getCurrentUser()
        const isDriver = u?.roles?.includes('DRIVER')
        nav(isDriver ? '/driver' : '/owner')
    }


    const canSubmit = useMemo(() => {
        return email.trim().length >= 5 && password.length >= 6
    }, [email, password])

    async function loginWithEmailPassword() {
        setError('')

        if (!canSubmit) {
            setError('Please enter a valid email and password.')
            return
        }

        try {
            const res = await fetch(`${API_BASE_URL}/api/auth/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email: email.trim(), password }),
            })

            const data = await res.json().catch(() => null)
            if (!res.ok) {
                setError(data?.message || `Login failed (${res.status})`)
                return
            }

            loginUser({ user: data?.user, token: data?.token })
            redirectAfterLogin()

        } catch (err) {
            setError(err?.message ? `Login error: ${err.message}` : 'Login error.')
        }
    }

    async function loginWithGoogleIdToken(idToken) {
        setError('')
        try {
            const res = await fetch(`${API_BASE_URL}/api/auth/google-login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ googleIdToken: idToken }),
            })

            const data = await res.json().catch(() => null)
            if (!res.ok) {
                setError(data?.message || `Google login failed (${res.status})`)
                return
            }

            loginUser({ user: data?.user, token: data?.token })
            redirectAfterLogin()

        } catch (err) {
            setError(err?.message ? `Google login error: ${err.message}` : 'Google login error.')
        }
    }

    return (
        <Layout title="">
            <div className="auth-wrap">
                <div className="auth-card auth-card-mobile">
                    {/*<div className="auth-bubble">📍</div>*/}
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 10, marginBottom: '20px' }}>
                        <span style={{ width: 42, height: 42, borderRadius: 999, background: 'rgb(37,99,235)', display: 'grid', placeItems: 'center', color: 'white', overflow: 'hidden' }} aria-hidden="true">
                            <img src="Logo_notext.png" alt="Logo" style={{ width: 41, height: 50, display: 'block', marginLeft: -1 }} />
                        </span>
                        <div style={{ fontWeight: 900, fontSize: 24, color: '#0549fa' }}>EasyPark</div>
                    </div>

                    <h2 className="auth-title">Welcome to EasyPark</h2>
                    <p className="auth-subtitle">Sign in to continue</p>

                    {/* Custom Google button (styled like the screenshot) */}
                    <div className="auth-google-wrap">
                        <GoogleLogin
                            locale="en"
                            onSuccess={(cred) => {
                                const idToken = cred?.credential
                                if (!idToken) {
                                    setError('Google did not return an ID token.')
                                    return
                                }
                                loginWithGoogleIdToken(idToken)
                            }}
                            onError={() => setError('Google login failed. Please try again.')}
                            useOneTap={false}
                            theme="outline"
                            shape="pill"
                            width="360"
                            text="continue_with"
                        />
                    </div>

                    <div className="auth-divider">OR</div>

                    <div className="auth-field">
                        <label>Email</label>
                        <div className="auth-input-wrap">
                            <span className="auth-input-icon">
                                <MailIcon />
                            </span>
                            <input
                                className="auth-input auth-input-iconpad"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                placeholder="you@example.com"
                                autoComplete="email"
                            />
                        </div>
                    </div>

                    <div className="auth-field">
                        <label>Password</label>
                        <div className="auth-input-wrap">
                            <span className="auth-input-icon">
                                <LockIcon />
                            </span>
                            <input
                                className="auth-input auth-input-iconpad"
                                type="password"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                placeholder="••••••••"
                                autoComplete="current-password"
                            />
                        </div>
                    </div>

                    {error && <div className="auth-error">{error}</div>}

                    <button
                        className="auth-primary"
                        type="button"
                        onClick={loginWithEmailPassword}
                        disabled={!canSubmit}
                        style={{ opacity: canSubmit ? 1 : 0.6, cursor: canSubmit ? 'pointer' : 'not-allowed' }}
                    >
                        Sign in
                    </button>

                    <div className="auth-bottom">
                        <Link to="/forgot-password">Forgot password?</Link>
                        <div>
                            Need an account? <Link to="/register">Sign up</Link>
                        </div>
                    </div>
                </div>
            </div>
        </Layout>
    )
}
