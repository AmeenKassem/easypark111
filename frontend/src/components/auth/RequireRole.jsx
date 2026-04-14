import React from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { getCurrentUser } from '../../services/session'

export default function RequireRole({ allow = [], children }) {
    const location = useLocation()
    const user = getCurrentUser()

    // Not logged in
    if (!user) {
        return <Navigate to="/login" replace state={{ from: location.pathname }} />
    }

    const roles = Array.isArray(user.roles) ? user.roles : []
    const ok = allow.some((r) => roles.includes(r))

    // Logged in but no permission
    if (!ok) {
        return (
            <Navigate
                to="/no-permission"
                replace
                state={{
                    from: location.pathname,
                    allow,
                    roles,
                }}
            />
        )
    }

    return children
}
