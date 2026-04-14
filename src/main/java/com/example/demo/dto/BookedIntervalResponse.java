package com.example.demo.dto;

import com.example.demo.model.Booking;
import com.example.demo.model.BookingStatus;

import java.time.LocalDateTime;

public class BookedIntervalResponse {

    private Long bookingId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BookingStatus status;

    public static BookedIntervalResponse from(Booking b) {
        BookedIntervalResponse r = new BookedIntervalResponse();
        r.bookingId = b.getId();
        r.startTime = b.getStartTime();
        r.endTime = b.getEndTime();
        r.status = b.getStatus();
        return r;
    }

    public Long getBookingId() { return bookingId; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public BookingStatus getStatus() { return status; }
}
