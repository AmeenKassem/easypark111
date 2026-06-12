package com.example.demo.controller;

import com.example.demo.dto.NotificationResponse;
import com.example.demo.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    private Long currentUserId(Authentication auth) {
        return (Long) auth.getPrincipal();
    }

    @PreAuthorize("hasAnyRole('DRIVER','OWNER')")
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> list(Authentication auth) {
        Long userId = currentUserId(auth);
        log.info("action=notifications_list userId={}", userId);
        return ResponseEntity.ok(notificationService.listForUser(userId));
    }

    @PreAuthorize("hasAnyRole('DRIVER','OWNER')")
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount(Authentication auth) {
        Long userId = currentUserId(auth);
        log.info("action=notifications_unread_count userId={}", userId);
        return ResponseEntity.ok(Map.of("count", notificationService.countUnread(userId)));
    }

    @PreAuthorize("hasAnyRole('DRIVER','OWNER')")
    @PutMapping("/read-all")
    public ResponseEntity<List<NotificationResponse>> markAllRead(Authentication auth) {
        Long userId = currentUserId(auth);
        log.info("action=notifications_mark_all_read userId={}", userId);
        return ResponseEntity.ok(notificationService.markAllAsRead(userId));
    }

    @PreAuthorize("hasAnyRole('DRIVER','OWNER')")
    @DeleteMapping
    public ResponseEntity<List<NotificationResponse>> clear(Authentication auth) {
        Long userId = currentUserId(auth);
        log.info("action=notifications_clear userId={}", userId);
        return ResponseEntity.ok(notificationService.clearAll(userId));
    }
}
