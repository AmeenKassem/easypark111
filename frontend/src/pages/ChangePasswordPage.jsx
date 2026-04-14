import { useState } from 'react'
import axios from 'axios'
import Layout from '../components/layout/layout'
import '../styles/auth.css'
import { loginUser, logout } from '../services/session'
import {API_BASE_URL} from "../config.js";

const API_BASE = API_BASE_URL

export default function ChangePasswordPage() {
    const [saving, setSaving] = useState(false)
    const [feedback, setFeedback] = useState({ message: '', isError: false })

    const [form, setForm] = useState({
        currentPassword: '',
        newPassword: '',
        confirmNewPassword: '',
    })

    const token = localStorage.getItem('easypark_token')

    const onChange = (e) => {
        const { name, value } = e.target
        setForm((p) => ({ ...p, [name]: value }))
    }

    const persistAuth = (authResponse) => {
        const newToken = authResponse?.token
        const user = authResponse?.user
        if (newToken && user) {
            loginUser({ user, token: newToken })
        }
    }

    const validate = () => {
        const cur = form.currentPassword.trim()
        const np = form.newPassword.trim()
        const cnp = form.confirmNewPassword.trim()

        if (!token) return 'You are not logged in.'
        if (!cur) return 'Current password is required.'
        if (!np) return 'New password is required.'
        if (np.length < 8) return 'New password must be at least 8 characters.' // adjust if backend requires different
        if (np !== cnp) return 'New password and confirmation do not match.'
        if (cur === np) return 'New password must be different from current password.'
        return null
    }

    const handleSave = async () => {
        setFeedback({ message: '', isError: false })

        const err = validate()
        if (err) {
            setFeedback({ message: err, isError: true })
            return
        }

        setSaving(true)
        try {
            const res = await axios.put(
                `${API_BASE}/api/users/me/password`,
                {
                    currentPassword: form.currentPassword,
                    newPassword: form.newPassword,
                },
                { headers: { Authorization: `Bearer ${token}` } },
            )

            // Backend returns AuthResponse("Password updated successfully", newToken, updatedSummary)
            persistAuth(res.data) // optional; keeps storage consistent briefly

            setForm({ currentPassword: '', newPassword: '', confirmNewPassword: '' })
            setFeedback({ message: res.data?.message || 'Password updated successfully!', isError: false })

// Hard logout after password change (security)
            logout()
            window.location.href = '/login'


            // Optional but recommended security behavior:
            // If you want to force re-login after password change, uncomment:
            //
            // logout()
            // window.location.href = '/login'
        } catch (e) {
            // If backend sends a useful message (e.g., "current password incorrect"), prefer it
            const msg =
                e?.response?.data?.message ||
                e?.response?.data ||
                e?.message ||
                'Error updating password. Please try again.'
            setFeedback({ message: String(msg), isError: true })

            // If token is invalid/expired, force logout
            if (e?.response?.status === 401) {
                logout()
                window.location.href = '/login'
            }
        } finally {
            setSaving(false)
        }
    }

    return (
        <Layout title="">
            <div className="auth-wrap" style={{ minHeight: 'calc(100vh - 80px)' }}>
                <div className="auth-card" style={{ maxWidth: 520, textAlign: 'left' }}>
                    <div className="auth-title" style={{ marginTop: 0 }}>
                        Change Password
                    </div>
                    <div className="auth-subtitle">Update your account password</div>

                    <div className="auth-form">
                        <div className="auth-field">
                            <label>Current Password</label>
                            <input
                                className="auth-input"
                                type="password"
                                name="currentPassword"
                                value={form.currentPassword}
                                onChange={onChange}
                                placeholder="Enter current password"
                                autoComplete="current-password"
                            />
                        </div>

                        <div className="auth-field">
                            <label>New Password</label>
                            <input
                                className="auth-input"
                                type="password"
                                name="newPassword"
                                value={form.newPassword}
                                onChange={onChange}
                                placeholder="Enter new password"
                                autoComplete="new-password"
                            />
                        </div>

                        <div className="auth-field">
                            <label>Confirm New Password</label>
                            <input
                                className="auth-input"
                                type="password"
                                name="confirmNewPassword"
                                value={form.confirmNewPassword}
                                onChange={onChange}
                                placeholder="Re-enter new password"
                                autoComplete="new-password"
                            />
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

                        <button
                            type="button"
                            className="auth-primary"
                            onClick={handleSave}
                            disabled={saving}
                            style={{ marginTop: 18 }}
                        >
                            {saving ? 'Saving...' : 'Update Password'}
                        </button>
                    </div>
                </div>
            </div>
        </Layout>
    )
}
