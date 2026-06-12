package com.example.demo.service;

import com.example.demo.dto.NotificationResponse;
import com.example.demo.model.BookingStatus;
import com.example.demo.model.Notification;
import com.example.demo.repository.BookingRepository;
import com.example.demo.repository.NotificationRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class NotificationServiceImpl implements NotificationService {

    /** User-destination suffix that delivers a single new notification. */
    static final String QUEUE_NEW = "/queue/notifications";
    /** User-destination suffix that delivers the current unread count for the badge. */
    static final String QUEUE_UNREAD_COUNT = "/queue/notifications-unread-count";

    private final NotificationRepository notificationRepository;
    private final BookingRepository bookingRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                   BookingRepository bookingRepository,
                                   SimpMessagingTemplate messagingTemplate) {
        this.notificationRepository = notificationRepository;
        this.bookingRepository = bookingRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public List<NotificationResponse> listForUser(Long userId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public long countUnread(Long userId) {
        return notificationRepository.countByRecipientIdAndIsReadFalse(userId);
    }

    @Override
    public Notification createNotification(Long recipientId, String title, String message) {
        return createNotification(recipientId, title, message, Notification.TYPE_GENERAL, null);
    }

    @Override
    @Transactional
    public Notification createNotification(Long recipientId, String title, String message,
                                           String type, Long bookingId) {
        Notification notification = new Notification();
        notification.setRecipientId(recipientId);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setType(type != null ? type : Notification.TYPE_GENERAL);
        notification.setBookingId(bookingId);

        Notification saved = notificationRepository.save(notification);

        // 1) Push the new notification (with action metadata) so open views can
        //    prepend it instantly, including its approve/reject buttons.
        messagingTemplate.convertAndSendToUser(
                String.valueOf(recipientId),
                QUEUE_NEW,
                toResponse(saved)
        );

        // 2) Push the fresh unread count so the badge updates without a REST round-trip.
        sendUnreadCount(recipientId);

        return saved;
    }

    @Override
    @Transactional
    public List<NotificationResponse> markAllAsRead(Long userId) {
        List<Notification> notifications = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId);
        notifications.forEach(notification -> notification.setRead(true));
        List<Notification> saved = notificationRepository.saveAll(notifications);

        // Everything is read now -> badge must drop to 0 in real time.
        sendUnreadCount(userId);

        return saved.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public List<NotificationResponse> clearAll(Long userId) {
        notificationRepository.deleteAllByRecipientId(userId);

        sendUnreadCount(userId);

        return List.of();
    }

    /**
     * Map an entity to its wire form, enriching BOOKING_REQUEST notifications with
     * the booking's CURRENT status so the frontend knows whether to show the
     * approve/reject buttons (only while the booking is still PENDING).
     */
    private NotificationResponse toResponse(Notification n) {
        NotificationResponse response = NotificationResponse.from(n);
        if (Notification.TYPE_BOOKING_REQUEST.equals(n.getType()) && n.getBookingId() != null) {
            bookingRepository.findById(n.getBookingId()).ifPresent(booking -> {
                BookingStatus status = booking.getStatus();
                response.applyBookingState(
                        status != null ? status.name() : null,
                        status == BookingStatus.PENDING
                );
            });
        }
        return response;
    }

    private void sendUnreadCount(Long userId) {
        messagingTemplate.convertAndSendToUser(
                String.valueOf(userId),
                QUEUE_UNREAD_COUNT,
                Map.of("count", countUnread(userId))
        );
    }
}
