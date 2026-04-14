import { useMemo, useState } from 'react'
import { useNavigate, useSearchParams, Link } from 'react-router-dom'
import Layout from '../components/layout/layout'
import '../styles/auth.css'
import {API_BASE_URL} from "../config.js";

export default function ResetPasswordPage() {
  const nav = useNavigate()
  const [params] = useSearchParams()

  const token = params.get('token') || ''

  const [newPassword, setNewPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [msg, setMsg] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const canSubmit = useMemo(() => {
    return token && newPassword.length >= 8 && newPassword === confirm && !loading
  }, [token, newPassword, confirm, loading])

  async function onSubmit(e) {
    e.preventDefault()
    setMsg('')
    setError('')

    if (!token) {
      setError('Missing token in URL. Please open the reset link from your email again.')
      return
    }

    if (newPassword.length < 8) {
      setError('Password must be at least 8 characters.')
      return
    }

    if (newPassword !== confirm) {
      setError('Passwords do not match.')
      return
    }

    setLoading(true)
    try {
      const res = await fetch(`${API_BASE_URL}/api/auth/reset-password`, {

        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token, newPassword }),
      })

      const data = await res.json().catch(() => ({}))

      if (!res.ok) {
        throw new Error(data.message || `Reset failed (${res.status})`)
      }

      setMsg(data.message || 'Password has been reset successfully. You can login now.')

      setTimeout(() => nav('/login'), 1200)
    } catch (err) {
      setError(err?.message || 'Reset failed.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <Layout title="Reset Password">
      <div className="auth-wrap">
        <div className="auth-card">
          <h2 className="auth-title">Reset password</h2>
          <p className="auth-subtitle">
            Choose a new password for your account.
          </p>

          <form className="auth-form" onSubmit={onSubmit}>
            <div className="auth-field">
              <label>New password</label>
              <input
                className="auth-input"
                type="password"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                placeholder="••••••••"
                autoComplete="new-password"
              />
            </div>

            <div className="auth-field">
              <label>Confirm password</label>
              <input
                className="auth-input"
                type="password"
                value={confirm}
                onChange={(e) => setConfirm(e.target.value)}
                placeholder="••••••••"
                autoComplete="new-password"
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
                {loading ? 'Resetting...' : 'Reset password'}
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