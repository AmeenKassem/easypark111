import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { API_BASE_URL } from '../config.js'
import {
    notifyNotificationsChanged,
    notifyNotificationReceived,
    notifyUnreadCount,
} from './notifications.js'

let client = null

export function connectRealtime() {
    const token = localStorage.getItem('easypark_token')

    if (!token) return null
    if (client?.active) return client

    client = new Client({
        webSocketFactory: () => new SockJS(`${API_BASE_URL}/ws`),
        connectHeaders: {
            Authorization: `Bearer ${token}`,
        },
        reconnectDelay: 5000,
        debug: () => {},
        onConnect: () => {
            // A new notification was created for this user.
            client.subscribe('/user/queue/notifications', (message) => {
                const notification = JSON.parse(message.body)
                notifyNotificationReceived(notification)
                notifyNotificationsChanged()
            })

            // Authoritative unread count pushed by the server (badge source of truth).
            client.subscribe('/user/queue/notifications-unread-count', (message) => {
                const payload = JSON.parse(message.body)
                notifyUnreadCount(Number(payload?.count ?? 0))
            })

            // New parking spot broadcast (consumed by the driver map).
            client.subscribe('/topic/parking-spots', (message) => {
                const parkingSpot = JSON.parse(message.body)
                window.dispatchEvent(new CustomEvent('easypark_parking_created', { detail: parkingSpot }))
            })
        },
        onStompError: (frame) => {
            console.error('WebSocket STOMP error:', frame.headers?.message || frame.body)
        },
        onWebSocketError: (error) => {
            console.error('WebSocket error:', error)
        },
    })

    client.activate()
    return client
}

export function disconnectRealtime() {
    if (client) {
        client.deactivate()
        client = null
    }
}
