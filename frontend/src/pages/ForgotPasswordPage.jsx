import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import Layout from '../components/layout/layout'
import '../styles/auth.css'
import {API_BASE_URL} from "../config.js";

const API_BASE = API_BASE_URL

export default function ForgotPasswordPage() {
    const [email, setEmail] = useState('')
    const [msg, setMsg] = useState('')
    const [error, setError] = useState('')
    const [loading, setLoading] = useState(false)

    const canSubmit = useMemo(() => {
        return email.trim().length > 3 && email.includes('@') && !loading
    }, [email, loading])

    async function onSubmit(e) {
        e.preventDefault()
        setMsg('')
        setError('')

        const normalized = email.trim().toLowerCase()
        if (!normalized || !normalized.includes('@')) {
            setError('Please enter a valid email.')
            return
        }

        setLoading(true)
        try {
            const res = await fetch(`${API_BASE}/api/auth/forgot-password`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email: normalized }),
            })

            // Backend returns generic response by design (do not reveal if email exists)
            const data = await res.json().catch(() => ({}))
            if (!res.ok) {
                // Even if server errors, keep message safe and generic
                throw new Error(data?.message || `Request failed (${res.status})`)
            }

            setMsg(data?.message || 'If this email exists, a reset link has been sent.')
            setEmail('')
        } catch (err) {
            setError(err?.message || 'Failed to request password reset. Please try again.')
        } finally {
            setLoading(false)
        }
    }

    return (
        <Layout title="Forgot Password">
            <div className="auth-wrap">
                <div className="auth-card">
                    <h2 className="auth-title">Forgot password</h2>
                    <p className="auth-subtitle">
                        Enter your email and we will send you a password reset link.
                    </p>

                    <form className="auth-form" onSubmit={onSubmit}>
                        <div className="auth-field">
                            <label>Email</label>
                            <input
                                className="auth-input"
                                type="email"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                placeholder="you@example.com"
                                autoComplete="email"
                            />
                        </div>

                        {error && <div className="auth-error">{error}</div>}
                        {msg && <div className="auth-hint">{msg}</div>}

                        <div className="auth-actions">
                            <button
                                className="ep-btn ep-btn-primary"
                                type="submit"
                                disabled={!canSubmit}
                                style={{
                                    opacity: canSubmit ? 1 : 0.55,
                                    cursor: canSubmit ? 'pointer' : 'not-allowed',
                                }}
                            >
                                {loading ? 'Sending...' : 'Send reset link'}
                            </button>

                            <Link className="ep-btn" to="/login" style={{ textDecoration: 'none' }}>
                                Back to login
                            </Link>
                        </div>
                    </form>
                </div>
            </div>
        </Layout>
    )
}
