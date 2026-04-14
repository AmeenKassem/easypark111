package com.example.demo.controller;

import com.example.demo.dto.BookingResponse;
import com.example.demo.dto.CreateBookingRequest;
import com.example.demo.dto.RatingRequest;
import com.example.demo.dto.UpdateBookingStatusRequest;
import com.example.demo.dto.UserSummary;
import com.example.demo.model.Booking;
import com.example.demo.model.BookingStatus;
import com.example.demo.service.BookingService;
import com.example.demo.service.EmailService;
import com.example.demo.service.RatingService;
import com.example.demo.service.UserService;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "*")
public class BookingController {

    private static final Logger log = LoggerFactory.getLogger(BookingController.class);

    private final BookingService bookingService;
    private final EmailService emailService;
    private final UserService userService;
    private final RatingService ratingService; // NEW

    public BookingController(BookingService bookingService, EmailService emailService, UserService userService, RatingService ratingService) {
        this.bookingService = bookingService;
        this.emailService = emailService;
        this.userService = userService;
        this.ratingService = ratingService; // NEW
    }

    private Long currentUserId(Authentication auth) {
        return (Long) auth.getPrincipal();
    }

    @PreAuthorize("hasRole('DRIVER')")
    @PostMapping
    public ResponseEntity<BookingResponse> create(@Valid @RequestBody CreateBookingRequest req,
                                                  Authentication auth) {
        Long userId = currentUserId(auth);
        log.info("action=booking_create start userId={} parkingId={} start={} end={}",
                userId, req.getParkingId(), req.getStartTime(), req.getEndTime());

        Booking b = bookingService.create(userId, req);

        log.info("action=booking_create success userId={} bookingId={} status={}",
                userId, b.getId(), b.getStatus());
        return ResponseEntity.ok(BookingResponse.from(b));
    }

    @PreAuthorize("hasRole('DRIVER')")
    @GetMapping("/my")
    public ResponseEntity<List<BookingResponse>> myBookings(Authentication auth) {
        Long userId = currentUserId(auth);
        log.info("action=booking_list_mine start userId={}", userId);

        List<BookingResponse> out = bookingService.listMine(userId)
                .stream().map(BookingResponse::from).toList();

        log.info("action=booking_list_mine success userId={} count={}", userId, out.size());
        return ResponseEntity.ok(out);
    }

    @PreAuthorize("hasRole('OWNER')")
    @GetMapping("/owner")
    public ResponseEntity<List<BookingResponse>> ownerBookings(Authentication auth) {
        Long userId = currentUserId(auth);
        log.info("action=booking_list_owner start ownerId={}", userId);

        List<BookingResponse> out = bookingService.listForOwner(userId)
                .stream().map(BookingResponse::from).toList();

        log.info("action=booking_list_owner success ownerId={} count={}", userId, out.size());
        return ResponseEntity.ok(out);
    }

    @PreAuthorize("hasRole('OWNER')")
    @PutMapping("/{id}/status")
    public ResponseEntity<BookingResponse> updateStatus(@PathVariable Long id,
                                                        @Valid @RequestBody UpdateBookingStatusRequest req,
                                                        Authentication auth) {
        Long ownerId = currentUserId(auth);
        log.info("action=booking_status_update start ownerId={} bookingId={} status={}",
                ownerId, id, req.getStatus());

        Booking b = bookingService.updateStatus(ownerId, id, req);

        if (b != null && b.getStatus().equals(BookingStatus.APPROVED)) {
            UserSummary ownerSummary = userService.getUserSummary(ownerId);
            emailService.sendBookingApprovedNotification(b.getDriver().getEmail(), b, ownerSummary);
        }

        log.info("action=booking_status_update success ownerId={} bookingId={} status={}",
                ownerId, b.getId(), b.getStatus());
        return ResponseEntity.ok(BookingResponse.from(b));
    }

    @PreAuthorize("hasRole('DRIVER')")
    @PutMapping("/{id}/cancel")
    public ResponseEntity<BookingResponse> cancel(@PathVariable Long id, Authentication auth) {
        Long userId = currentUserId(auth);
        log.info("action=booking_cancel start userId={} bookingId={}", userId, id);

        Booking b = bookingService.cancel(userId, id);

        log.info("action=booking_cancel success userId={} bookingId={} status={}",
                userId, b.getId(), b.getStatus());
        return ResponseEntity.ok(BookingResponse.from(b));
    }

    // --- NEW: Rate Driver Endpoint ---
    @PreAuthorize("hasRole('OWNER')")
    @PostMapping("/{id}/rate-driver")
    public ResponseEntity<?> rateDriver(@PathVariable Long id,
                                        @Valid @RequestBody RatingRequest req,
                                        Authentication auth) {
        Long ownerId = currentUserId(auth);
        log.info("action=rate_driver start ownerId={} bookingId={} score={}", ownerId, id, req.getScore());

        ratingService.rateDriver(id, ownerId, req.getScore());

        log.info("action=rate_driver success ownerId={} bookingId={}", ownerId, id);
        return ResponseEntity.ok(Map.of("message", "Rating submitted successfully"));
    }
}