// pages/NoPermissionPage.jsx
import { useLocation, useNavigate } from 'react-router-dom'
import { getCurrentUser } from '../services/session'

export default function NoPermissionPage() {
    const nav = useNavigate()
    const location = useLocation()
    const user = getCurrentUser()
    const roles = new Set(user?.roles ?? [])

    const fallback =
        roles.has('OWNER') ? '/owner' :
            roles.has('DRIVER') ? '/driver' :
                '/login'

    const from = location.state?.from

    return (
        <div style={{ padding: 24 }}>
            <h1>No permission</h1>
            <p>You don’t have permission to access this page.</p>

            <button onClick={() => nav(fallback, { replace: true })}>
                Go to an allowed page
            </button>

            {from && (
                <button onClick={() => nav(-1)} style={{ marginLeft: 10 }}>
                    Back
                </button>
            )}
        </div>
    )
}
