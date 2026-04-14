package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;

public class CreatePaymentRequest {

    @NotNull(message = "bookingId is required")
    private Long bookingId;

    public CreatePaymentRequest() {}

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }
}
