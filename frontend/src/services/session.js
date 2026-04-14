const STORAGE_KEY = 'easypark_user'
const TOKEN_KEY = 'easypark_token'

export function getCurrentUser() {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : null
}

const AUTH_EVENT = 'easypark_auth_changed'

export function notifyAuthChanged() {
    window.dispatchEvent(new Event(AUTH_EVENT))
}

export function subscribeAuthChanged(handler) {
    window.addEventListener(AUTH_EVENT, handler)
    return () => window.removeEventListener(AUTH_EVENT, handler)
}

export function isAuthenticated() {
    return !!getCurrentUser()
}

export function loginMock({ fullName, roles }) {
    localStorage.setItem(
        STORAGE_KEY,
        JSON.stringify({
            fullName,
            roles,
        })
    )
}

// Real login helper (works for email/password + Google login)
export function loginUser({ user, token }) {
    if (token) localStorage.setItem(TOKEN_KEY, token)

    const role = user?.role
    let roles = ['DRIVER'] // default

    if (role === 'OWNER') {
        roles = ['OWNER']
    } else if (role === 'BOTH') {
        roles = ['DRIVER', 'OWNER']
    } else if (role === 'DRIVER') {
        roles = ['DRIVER']
    }


    localStorage.setItem(
        STORAGE_KEY,
        JSON.stringify({
            id: user?.id,
            fullName: user?.fullName,
            email: user?.email,
            roles,
        })
    )
    notifyAuthChanged()

}

export function logout() {
    localStorage.removeItem(STORAGE_KEY)
    localStorage.removeItem(TOKEN_KEY)
    notifyAuthChanged()

}

export const getAuthToken = () => {
    return localStorage.getItem(TOKEN_KEY);
}
