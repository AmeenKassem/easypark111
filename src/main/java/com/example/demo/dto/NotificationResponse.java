package com.example.demo.dto;

import com.example.demo.model.Notification;

import java.time.LocalDateTime;

/**
 * Wire representation of a {@link Notification}.
 *
 * <p>Decouples the persistence entity from the API/WebSocket contract and
 * guarantees a stable JSON shape that matches the frontend:
 * {@code id, userId, title, message, createdAt, read, type, bookingId,
 * bookingStatus, actionable}.
 *
 * <p>For {@code BOOKING_REQUEST} notifications, {@code bookingStatus} reflects the
 * booking's CURRENT status (looked up at read time, not a stale snapshot) and
 * {@code actionable} is true while the owner can still approve/reject it.
 */
public class NotificationResponse {

    private Long id;
    private Long userId;
    private String title;
    private String message;
    private LocalDateTime createdAt;
    private boolean read;
    private String type;
    private Long bookingId;
    private String bookingStatus;
    private boolean actionable;

    public static NotificationResponse from(Notification n) {
        NotificationResponse r = new NotificationResponse();
        r.id = n.getId();
        r.userId = n.getRecipientId();
        r.title = n.getTitle();
        r.message = n.getMessage();
        r.createdAt = n.getCreatedAt();
        r.read = n.isRead();
        // Null type (legacy rows) is treated as a plain notification.
        r.type = (n.getType() != null) ? n.getType() : Notification.TYPE_GENERAL;
        r.bookingId = n.getBookingId();
        r.bookingStatus = null;
        r.actionable = false;
        return r;
    }

    /** Attach the current booking state for actionable booking notifications. */
    public void applyBookingState(String bookingStatus, boolean actionable) {
        this.bookingStatus = bookingStatus;
        this.actionable = actionable;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // Serializes as the JSON property "read".
    public boolean isRead() { return read; }

    public String getType() { return type; }
    public Long getBookingId() { return bookingId; }
    public String getBookingStatus() { return bookingStatus; }
    public boolean isActionable() { return actionable; }
}
