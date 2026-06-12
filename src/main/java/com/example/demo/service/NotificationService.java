package com.example.demo.service;

import com.example.demo.dto.NotificationResponse;
import com.example.demo.model.Notification;

import java.util.List;

public interface NotificationService {
    List<NotificationResponse> listForUser(Long userId);
    long countUnread(Long userId);

    /** Create a plain (GENERAL) notification. */
    Notification createNotification(Long recipientId, String title, String message);

    /** Create a typed notification, optionally linked to a booking (for actionable items). */
    Notification createNotification(Long recipientId, String title, String message, String type, Long bookingId);

    List<NotificationResponse> markAllAsRead(Long userId);
    List<NotificationResponse> clearAll(Long userId);
}
