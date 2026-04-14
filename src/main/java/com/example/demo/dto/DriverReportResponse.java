package com.example.demo.dto;

import java.util.List;

public class DriverReportResponse {

    private Double totalExpenses;
    private int totalBookings;
    private List<BookingResponse> bookingHistory;

    public DriverReportResponse(Double totalExpenses, int totalBookings, List<BookingResponse> bookingHistory) {
        this.totalExpenses = totalExpenses != null ? totalExpenses : 0.0;
        this.totalBookings = totalBookings;
        this.bookingHistory = bookingHistory;
    }

    // Getters and Setters
    public Double getTotalExpenses() { return totalExpenses; }
    public void setTotalExpenses(Double totalExpenses) { this.totalExpenses = totalExpenses; }

    public int getTotalBookings() { return totalBookings; }
    public void setTotalBookings(int totalBookings) { this.totalBookings = totalBookings; }

    public List<BookingResponse> getBookingHistory() { return bookingHistory; }
    public void setBookingHistory(List<BookingResponse> bookingHistory) { this.bookingHistory = bookingHistory; }
}