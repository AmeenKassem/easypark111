package com.example.demo.service;

import com.example.demo.dto.CreatePaymentRequest;
import com.example.demo.model.Booking;
import com.example.demo.model.BookingStatus;
import com.example.demo.model.Payment;
import com.example.demo.model.PaymentStatus;
import com.example.demo.repository.BookingRepository;
import com.example.demo.repository.PaymentRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              BookingRepository bookingRepository) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
    }

    @Override
    public Payment create(Long driverId, CreatePaymentRequest req) {
        if (req == null || req.getBookingId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "bookingId is required");
        }

        Booking booking = bookingRepository.findById(req.getBookingId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        if (!driverId.equals(booking.getDriver().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to pay for this booking");
        }

        if (booking.getStatus() != BookingStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only APPROVED bookings can be paid");
        }

        paymentRepository.findByBookingId(booking.getId()).ifPresent(p -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment already exists for this booking");
        });

        if (booking.getTotalPrice() == null || booking.getTotalPrice() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking totalPrice is invalid");
        }

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(booking.getTotalPrice());
        payment.setCurrency("ILS");
        payment.setProvider("BIT");

        // Phase 1: simulate immediate success (no BIT integration yet)
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());

        return paymentRepository.save(payment);
    }

    @Override
    public List<Payment> listMine(Long driverId) {
        return paymentRepository.findMine(driverId);
    }

    @Override
    public List<Payment> listForOwner(Long ownerId) {
        return paymentRepository.findForOwner(ownerId);
    }
}
