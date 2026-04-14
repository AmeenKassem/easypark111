import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Layout from '../components/layout/layout'
import { loginUser } from '../services/session'
import '../styles/auth.css'
import {API_BASE_URL} from "../config.js";
import PhoneInput from 'react-phone-input-2'
import 'react-phone-input-2/lib/material.css'
import { isValidPhoneNumber } from 'libphonenumber-js'

const PhoneInputComp = PhoneInput?.default ?? PhoneInput

const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/
const lettersOnlyRegex = /^[\p{L}]+$/u
const hasDigitRegex = /\d/

function buildFullName(firstName, lastName) {
    return `${(firstName ?? '').toString().trim()} ${(lastName ?? '').toString().trim()}`.trim()
}

function toE164FromPhoneInputValue(v) {
    const raw = (v ?? '').toString().trim()
    if (!raw) return ''
    return raw.startsWith('+') ? raw : `+${raw}`
}

export default function RegisterPage() {
    const nav = useNavigate()

    const [firstName, setFirstName] = useState('')
    const [lastName, setLastName] = useState('')
    const [email, setEmail] = useState('')
    const [phone, setPhone] = useState('')
    const [password, setPassword] = useState('')

    const [error, setError] = useState('')

    const [fieldErrors, setFieldErrors] = useState({
        firstName: '',
        lastName: '',
        email: '',
        phone: '',
        password: '',
    })

    const canSubmit = useMemo(() => {
        return (
            firstName.trim().length >= 2 &&
            lastName.trim().length >= 2 &&
            email.trim().length >= 5 &&
            phone.toString().trim().length >= 8 &&
            password.length >= 8
        )
    }, [firstName, lastName, email, phone, password])

    function clearFieldError(key) {
        setFieldErrors((prev) => (prev[key] ? { ...prev, [key]: '' } : prev))
    }

    function validate() {
        const next = { firstName: '', lastName: '', email: '', phone: '', password: '' }

        const f = firstName.trim()
        const l = lastName.trim()
        const mail = email.trim()
        const e164 = toE164FromPhoneInputValue(phone)
        const pass = password

        // First name
        if (f.length < 2) next.firstName = 'First name is not valid'
        else if (hasDigitRegex.test(f)) next.firstName = 'First name cannot include numbers'
        else if (!lettersOnlyRegex.test(f)) next.firstName = 'First name must contain letters only'

        // Last name
        if (l.length < 2) next.lastName = 'Last name is not valid'
        else if (hasDigitRegex.test(l)) next.lastName = 'Last name cannot include numbers'
        else if (!lettersOnlyRegex.test(l)) next.lastName = 'Last name must contain letters only'

        // Email
        if (mail.length < 5 || !emailRegex.test(mail)) next.email = 'Email is not valid'

        // Phone
        if (!e164) next.phone = 'Phone number is not valid'
        else if (!isValidPhoneNumber(e164)) next.phone = 'Phone number is not valid'

        // Password
        if (pass.length < 8) next.password = 'Password must be at least 8 characters'

        return { next, e164 }
    }

    async function onSubmit(e) {
        e.preventDefault()
        setError('')

        const { next, e164 } = validate()
        const hasAny =
            !!next.firstName || !!next.lastName || !!next.email || !!next.phone || !!next.password

        if (hasAny) {
            setFieldErrors(next)
            return
        }

        setFieldErrors({ firstName: '', lastName: '', email: '', phone: '', password: '' })

        const fullName = buildFullName(firstName, lastName)

        const payload = {
            fullName,
            email: email.trim(),
            phone: e164,
            password,
            role: 'BOTH',
        }

        try {
            const res = await fetch(`${API_BASE_URL}/api/auth/register`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload),
            })

            const text = await res.text()
            let data = null
            try {
                data = text ? JSON.parse(text) : null
            } catch {
                data = null
            }

            if (!res.ok) {
                const msg =
                    (data && (data.message || data.error)) || text || `Register failed (${res.status})`
                setError(msg)
                return
            }

            const token = data?.token || null
            loginUser({ user: data?.user, token })
            nav('/driver')
        } catch (err) {
            setError(err?.message ? `Register error: ${err.message}` : 'Register error.')
        }
    }

    return (
        <Layout title="Register">
            <div className="auth-wrap">
                <div className="auth-card">
                    <h2 className="auth-title">Create your EasyPark account</h2>

                    <form className="auth-form" onSubmit={onSubmit}>
                        <div className="auth-field">
                            {fieldErrors.firstName && (
                                <div className="auth-field-error">{fieldErrors.firstName}</div>
                            )}
                            <label>First name</label>
                            <input
                                className="auth-input"
                                value={firstName}
                                onChange={(e) => {
                                    setFirstName(e.target.value)
                                    clearFieldError('firstName')
                                }}
                                placeholder="John"
                                autoComplete="given-name"
                            />
                        </div>

                        <div className="auth-field">
                            {fieldErrors.lastName && (
                                <div className="auth-field-error">{fieldErrors.lastName}</div>
                            )}
                            <label>Last name</label>
                            <input
                                className="auth-input"
                                value={lastName}
                                onChange={(e) => {
                                    setLastName(e.target.value)
                                    clearFieldError('lastName')
                                }}
                                placeholder="Doe"
                                autoComplete="family-name"
                            />
                        </div>

                        <div className="auth-field">
                            {fieldErrors.email && (
                                <div className="auth-field-error">{fieldErrors.email}</div>
                            )}
                            <label>Email</label>
                            <input
                                className="auth-input"
                                value={email}
                                onChange={(e) => {
                                    setEmail(e.target.value)
                                    clearFieldError('email')
                                }}
                                placeholder="name@example.com"
                                autoComplete="email"
                            />
                        </div>

                        <div className="auth-field">
                            {fieldErrors.phone && (
                                <div className="auth-field-error">{fieldErrors.phone}</div>
                            )}
                            <label>Phone</label>

                            <PhoneInputComp
                                country="il"
                                enableSearch
                                value={phone}
                                onChange={(val) => {
                                    setPhone(val)
                                    clearFieldError('phone')
                                }}
                                inputClass="auth-input"
                                specialLabel=""
                                placeholder="Phone number"
                            />
                        </div>

                        <div className="auth-field">
                            {fieldErrors.password && (
                                <div className="auth-field-error">{fieldErrors.password}</div>
                            )}
                            <label>Password</label>
                            <input
                                className="auth-input"
                                type="password"
                                value={password}
                                onChange={(e) => {
                                    setPassword(e.target.value)
                                    clearFieldError('password')
                                }}
                                placeholder="••••••••"
                                autoComplete="new-password"
                            />
                        </div>

                        {error && <div className="auth-error">{error}</div>}

                        <div className="auth-actions">
                            <button
                                className="auth-primary"
                                type="submit"
                                disabled={false}
                                style={{ opacity: canSubmit ? 1 : 0.9, cursor: 'pointer' }}
                            >
                                Register
                            </button>

                            <button className="auth-secondary" type="button" onClick={() => nav('/login')}>
                                Back to login
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </Layout>
    )
}
