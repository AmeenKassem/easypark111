import { useCallback, useEffect, useState } from 'react'
import { getCurrentUser, subscribeAuthChanged } from '../services/session'
import {
    getUnreadNotificationCount,
    subscribeNotificationsChanged,
    subscribeUnreadCount,
} from '../services/notifications'
import { connectRealtime, disconnectRealtime } from '../services/realtime'

/**
 * Single source of truth for the unread-notifications badge.
 *
 * Responsibilities:
 *  - opens the realtime (STOMP/WebSocket) connection while a user is logged in,
 *  - keeps an accurate unread count by combining:
 *      • an initial REST fetch,
 *      • instant WebSocket pushes of the authoritative count, and
 *      • a REST re-sync whenever a "notifications changed" event fires.
 *
 * Used by every header (shared Layout AND the standalone driver header) so the
 * red badge updates in real time on any page, with no reload or click.
 */
export function useUnreadNotifications() {
    const [unread, setUnread] = useState(0)

    const refreshUnread = useCallback(async () => {
        const current = getCurrentUser()
        if (!current?.id) {
            setUnread(0)
            return
        }
        try {
            const count = await getUnreadNotificationCount()
            setUnread(count)
        } catch {
            // Network blip: keep the previous count rather than flickering to 0.
        }
    }, [])

    useEffect(() => {
        const syncConnection = () => {
            if (getCurrentUser()?.id) connectRealtime()
            else disconnectRealtime()
        }

        // Connect + initial count on mount.
        syncConnection()
        refreshUnread()

        // Login/logout: (re)connect and re-sync.
        const cleanupAuth = subscribeAuthChanged(() => {
            syncConnection()
            refreshUnread()
        })
        // Local triggers (mark-all-read, clear): re-sync from the server.
        const cleanupChanged = subscribeNotificationsChanged(refreshUnread)
        // Instant push of the authoritative unread count over WebSocket.
        const cleanupCount = subscribeUnreadCount((count) => setUnread(count))

        return () => {
            cleanupAuth()
            cleanupChanged()
            cleanupCount()
        }
    }, [refreshUnread])

    return {
        unread,
        // Capped label like typical social apps.
        unreadLabel: unread > 99 ? '99+' : unread,
        refreshUnread,
    }
}
