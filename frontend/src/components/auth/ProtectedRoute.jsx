// components/auth/ProtectedRoute.jsx
import { Navigate, useLocation } from 'react-router-dom'
import { getCurrentUser } from '../../services/session'

export default function ProtectedRoute({ allowRoles, children }) {
    const location = useLocation()
    const user = getCurrentUser()
    const roles = new Set(user?.roles ?? [])

    const allowed = allowRoles.some(r => roles.has(r))
    if (!allowed) {
        return (
            <Navigate
                to="/no-permission"
                replace
                state={{ from: location.pathname, allowRoles }}
            />
        )
    }

    return children
}
