package com.example.demo.controller;

import com.example.demo.dto.CreatePaymentRequest;
import com.example.demo.dto.PaymentResponse;
import com.example.demo.model.Payment;
import com.example.demo.service.PaymentService;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    private Long currentUserId(Authentication auth) {
        return (Long) auth.getPrincipal(); // set by JwtAuthenticationFilter
    }

    // Driver: pay for an approved booking (BIT simulated)
    @PreAuthorize("hasRole('DRIVER')")
    @PostMapping
    public ResponseEntity<PaymentResponse> create(@Valid @RequestBody CreatePaymentRequest req,
                                                  Authentication auth) {
        Long userId = currentUserId(auth);

        log.info("action=payment_create start userId={} bookingId={}",
                userId, req.getBookingId());

        Payment p = paymentService.create(userId, req);

        log.info("action=payment_create success userId={} paymentId={} bookingId={} status={}",
                userId, p.getId(), p.getBooking().getId(), p.getStatus());

        return ResponseEntity.ok(PaymentResponse.from(p));
    }

    // Driver: list my payments
    @PreAuthorize("hasRole('DRIVER')")
    @GetMapping("/my")
    public ResponseEntity<List<PaymentResponse>> myPayments(Authentication auth) {
        Long userId = currentUserId(auth);

        log.info("action=payment_list_mine start userId={}", userId);

        List<PaymentResponse> out = paymentService.listMine(userId)
                .stream().map(PaymentResponse::from).toList();

        log.info("action=payment_list_mine success userId={} count={}", userId, out.size());
        return ResponseEntity.ok(out);
    }

    // Owner: list payments related to my parking spots
    @PreAuthorize("hasRole('OWNER')")
    @GetMapping("/owner")
    public ResponseEntity<List<PaymentResponse>> ownerPayments(Authentication auth) {
        Long userId = currentUserId(auth);

        log.info("action=payment_list_owner start ownerId={}", userId);

        List<PaymentResponse> out = paymentService.listForOwner(userId)
                .stream().map(PaymentResponse::from).toList();

        log.info("action=payment_list_owner success ownerId={} count={}", userId, out.size());
        return ResponseEntity.ok(out);
    }
}
