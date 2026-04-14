import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import Layout from '../components/layout/layout'
import '../styles/landing.css'

export default function DashboardPage() {
    const nav = useNavigate()
    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')

    return (
        <Layout title="Dashboard">
            <div className="landing-bg">
                <div className="landing-card">
                    <div className="landing-bubble">📍</div>

                    <h1 className="landing-title">Welcome to EasyPark</h1>
                    <div className="landing-subtitle">Sign in to continue</div>

                    <button
                        className="landing-google"
                        type="button"
                        onClick={() => nav('/login')}
                    >
                        <span style={{ fontSize: 18 }}>G</span>
                        Continue with Google
                    </button>

                    <div className="landing-divider">OR</div>

                    <div className="landing-label">Email</div>
                    <input
                        className="landing-input"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        placeholder="you@example.com"
                        autoComplete="email"
                    />

                    <div className="landing-label">Password</div>
                    <input
                        className="landing-input"
                        type="password"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        placeholder="••••••••"
                        autoComplete="current-password"
                    />

                    <button
                        className="landing-primary"
                        type="button"
                        onClick={() => nav('/login')}
                    >
                        Sign in
                    </button>

                    <div className="landing-bottom">
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
