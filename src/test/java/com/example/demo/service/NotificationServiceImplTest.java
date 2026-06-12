package com.example.demo.service;

import com.example.demo.dto.NotificationResponse;
import com.example.demo.model.Booking;
import com.example.demo.model.BookingStatus;
import com.example.demo.model.Notification;
import com.example.demo.repository.BookingRepository;
import com.example.demo.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificationServiceImpl service;

    private static final Long OWNER_ID = 200L;

    private Notification bookingRequest(Long id, Long bookingId) {
        Notification n = new Notification();
        n.setId(id);
        n.setRecipientId(OWNER_ID);
        n.setTitle("New Booking Request");
        n.setMessage("Someone requested your spot.");
        n.setType(Notification.TYPE_BOOKING_REQUEST);
        n.setBookingId(bookingId);
        return n;
    }

    private Booking bookingWithStatus(BookingStatus status) {
        Booking b = new Booking();
        b.setStatus(status);
        return b;
    }

    @Test
    void listForUser_bookingRequestIsActionable_whenBookingStillPending() {
        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(OWNER_ID))
                .thenReturn(List.of(bookingRequest(1L, 10L)));
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(bookingWithStatus(BookingStatus.PENDING)));

        List<NotificationResponse> result = service.listForUser(OWNER_ID);

        assertEquals(1, result.size());
        NotificationResponse r = result.get(0);
        assertEquals(Notification.TYPE_BOOKING_REQUEST, r.getType());
        assertEquals(Long.valueOf(10L), r.getBookingId());
        assertEquals("PENDING", r.getBookingStatus());
        assertTrue(r.isActionable(), "owner should be able to approve/reject a pending request");
    }

    @Test
    void listForUser_bookingRequestNotActionable_whenAlreadyApproved() {
        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(OWNER_ID))
                .thenReturn(List.of(bookingRequest(1L, 10L)));
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(bookingWithStatus(BookingStatus.APPROVED)));

        NotificationResponse r = service.listForUser(OWNER_ID).get(0);

        assertEquals("APPROVED", r.getBookingStatus());
        assertFalse(r.isActionable(), "a resolved booking must not show action buttons");
    }

    @Test
    void listForUser_generalNotification_isNeverActionable() {
        Notification general = new Notification();
        general.setId(2L);
        general.setRecipientId(OWNER_ID);
        general.setTitle("Booking Approved");
        general.setType(Notification.TYPE_GENERAL);
        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(OWNER_ID))
                .thenReturn(List.of(general));

        NotificationResponse r = service.listForUser(OWNER_ID).get(0);

        assertEquals(Notification.TYPE_GENERAL, r.getType());
        assertFalse(r.isActionable());
        assertNull(r.getBookingStatus());
        // No booking lookup should happen for non-booking notifications.
        verifyNoInteractions(bookingRepository);
    }

    @Test
    void createNotification_typed_persistsMetadataAndPushesRealtime() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookingRepository.findById(55L)).thenReturn(Optional.of(bookingWithStatus(BookingStatus.PENDING)));

        service.createNotification(OWNER_ID, "New Booking Request", "msg",
                Notification.TYPE_BOOKING_REQUEST, 55L);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertEquals(Notification.TYPE_BOOKING_REQUEST, saved.getType());
        assertEquals(Long.valueOf(55L), saved.getBookingId());

        // Pushes both the new notification and the unread-count update to the recipient.
        verify(messagingTemplate).convertAndSendToUser(eq("200"), eq("/queue/notifications"), any(NotificationResponse.class));
        verify(messagingTemplate).convertAndSendToUser(eq("200"), eq("/queue/notifications-unread-count"), any());
    }

    @Test
    void createNotification_general_defaultsTypeAndHasNoBooking() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        service.createNotification(OWNER_ID, "Booking Approved", "msg");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertEquals(Notification.TYPE_GENERAL, saved.getType());
        assertNull(saved.getBookingId());
    }
}
