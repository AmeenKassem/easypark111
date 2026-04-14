package com.example.demo.dto;

import com.example.demo.model.Payment;

import java.time.LocalDateTime;

public class PaymentResponse {

    private Long id;
    private Long bookingId;
    private Long driverId;
    private Long ownerId;

    private Double amount;
    private String currency;

    private String provider;
    private String status;

    private LocalDateTime paidAt;
    private LocalDateTime createdAt;

    public static PaymentResponse from(Payment p) {
        PaymentResponse r = new PaymentResponse();
        r.id = p.getId();
        r.bookingId = p.getBooking().getId();
        r.driverId = p.getBooking().getDriver().getId();
        r.ownerId = p.getBooking().getParking().getOwnerId();

        r.amount = p.getAmount();
        r.currency = p.getCurrency();

        r.provider = p.getProvider();
        r.status = p.getStatus() == null ? null : p.getStatus().name();

        r.paidAt = p.getPaidAt();
        r.createdAt = p.getCreatedAt();
        return r;
    }

    public PaymentResponse() {}

    public Long getId() { return id; }
    public Long getBookingId() { return bookingId; }
    public Long getDriverId() { return driverId; }
    public Long getOwnerId() { return ownerId; }
    public Double getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getProvider() { return provider; }
    public String getStatus() { return status; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
