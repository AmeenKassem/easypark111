import { useEffect, useState } from 'react'
import axios from 'axios'
import Layout from '../components/layout/layout'
import '../styles/auth.css'
import { loginUser } from '../services/session'
import {API_BASE_URL} from "../config.js";

const API_BASE = API_BASE_URL

export default function ManageProfilePage() {
    const [loading, setLoading] = useState(true)
    const [saving, setSaving] = useState(false)
    const [feedback, setFeedback] = useState({ message: '', isError: false })

    const [form, setForm] = useState({
        fullName: '',
        phone: '',
        email: '',
    })

    const token = localStorage.getItem('easypark_token')

    useEffect(() => {
        const fetchMe = async () => {
            setLoading(true)
            setFeedback({ message: '', isError: false })

            try {
                if (!token) throw new Error('No token found')

                const res = await axios.get(`${API_BASE}/api/users/me`, {
                    headers: { Authorization: `Bearer ${token}` },
                })

                setForm({
                    fullName: res.data?.fullName ?? '',
                    phone: res.data?.phone ?? '',
                    email: res.data?.email ?? '',
                })
            } catch (e) {
                setFeedback({ message: 'Failed to load profile.', isError: true })
            } finally {
                setLoading(false)
            }
        }

        fetchMe()
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [])

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

    const handleSave = async () => {
        setSaving(true)
        setFeedback({ message: '', isError: false })

        try {
            if (!token) throw new Error('No token found')

            const profileRes = await axios.put(
                `${API_BASE}/api/users/me`,
                {
                    fullName: form.fullName,
                    phone: form.phone,
                    email: form.email,
                },
                { headers: { Authorization: `Bearer ${token}` } },
            )

            // Your backend returns AuthResponse (message + token + user summary)
            persistAuth(profileRes.data)

            setFeedback({ message: 'Profile updated successfully!', isError: false })
        } catch (e) {
            const msg =
                e?.response?.data?.message ||
                e?.message ||
                'Error updating profile. Please try again.'
            setFeedback({ message: msg, isError: true })
        } finally {
            setSaving(false)
        }
    }

    return (
        <Layout title="">
            <div className="auth-wrap" style={{ minHeight: 'calc(100vh - 80px)' }}>
                <div className="auth-card" style={{ maxWidth: 520, textAlign: 'left' }}>
                    <div className="auth-title" style={{ marginTop: 0 }}>
                        Manage Profile
                    </div>
                    <div className="auth-subtitle">Update your personal details</div>

                    {loading ? (
                        <div style={{ padding: 10 }}>Loading...</div>
                    ) : (
                        <div className="auth-form">
                            <div className="auth-field">
                                <label>Full Name</label>
                                <input
                                    className="auth-input"
                                    name="fullName"
                                    value={form.fullName}
                                    onChange={onChange}
                                    placeholder="Enter full name"
                                />
                            </div>

                            <div className="auth-field">
                                <label>Phone Number</label>
                                <input
                                    className="auth-input"
                                    name="phone"
                                    value={form.phone}
                                    onChange={onChange}
                                    placeholder="Enter phone number"
                                />
                            </div>

                            <div className="auth-field">
                                <label>Email</label>
                                <input
                                    className="auth-input"
                                    name="email"
                                    value={form.email}
                                    onChange={onChange}
                                    placeholder="Enter email"
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
                                {saving ? 'Saving...' : 'Save Changes'}
                            </button>
                        </div>
                    )}
                </div>
            </div>
        </Layout>
    )
}
