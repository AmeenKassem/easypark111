import axios from 'axios'
import { API_BASE_URL } from '../config.js'

const API_BASE = API_BASE_URL

// Fired when something that affects notifications changed and listeners should
// re-sync from the server (e.g. after mark-all-read / clear, or as a fallback).
const NOTIFICATIONS_CHANGED_EVENT = 'easypark_notifications_changed'
// Fired with a single freshly-arrived notification (detail = NotificationResponse).
const NOTIFICATION_RECEIVED_EVENT = 'easypark_notification_received'
// Fired with the authoritative unread count pushed by the server (detail = number).
const UNREAD_COUNT_EVENT = 'easypark_unread_count'

function authHeaders() {
    const token = localStorage.getItem('easypark_token')
    return token ? { Authorization: `Bearer ${token}` } : {}
}

/* ----------------------------- event plumbing ----------------------------- */

export function notifyNotificationsChanged() {
    window.dispatchEvent(new Event(NOTIFICATIONS_CHANGED_EVENT))
}

export function subscribeNotificationsChanged(handler) {
    window.addEventListener(NOTIFICATIONS_CHANGED_EVENT, handler)
    return () => window.removeEventListener(NOTIFICATIONS_CHANGED_EVENT, handler)
}

export function notifyNotificationReceived(notification) {
    window.dispatchEvent(new CustomEvent(NOTIFICATION_RECEIVED_EVENT, { detail: notification }))
}

export function subscribeNotificationReceived(handler) {
    const wrapped = (e) => handler(e.detail)
    window.addEventListener(NOTIFICATION_RECEIVED_EVENT, wrapped)
    return () => window.removeEventListener(NOTIFICATION_RECEIVED_EVENT, wrapped)
}

export function notifyUnreadCount(count) {
    window.dispatchEvent(new CustomEvent(UNREAD_COUNT_EVENT, { detail: count }))
}

export function subscribeUnreadCount(handler) {
    const wrapped = (e) => handler(e.detail)
    window.addEventListener(UNREAD_COUNT_EVENT, wrapped)
    return () => window.removeEventListener(UNREAD_COUNT_EVENT, wrapped)
}

/* --------------------------------- REST ---------------------------------- */

export async function getMyNotifications() {
    const res = await axios.get(`${API_BASE}/api/notifications`, {
        headers: authHeaders(),
    })
    return res.data || []
}

export async function getUnreadNotificationCount() {
    const res = await axios.get(`${API_BASE}/api/notifications/unread-count`, {
        headers: authHeaders(),
    })
    return res.data?.count ?? 0
}

export async function markAllNotificationsRead() {
    const res = await axios.put(`${API_BASE}/api/notifications/read-all`, null, {
        headers: authHeaders(),
    })
    return res.data || []
}

export async function clearNotifications() {
    const res = await axios.delete(`${API_BASE}/api/notifications`, {
        headers: authHeaders(),
    })
    return res.data || []
}
