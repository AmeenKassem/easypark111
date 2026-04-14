package com.example.demo.dto;

import java.util.List;

public class OwnerDashboardResponse {

    private Double totalRevenue;
    private int totalReservations;
    private List<BookingResponse> recentActivity;

    public OwnerDashboardResponse(Double totalRevenue, int totalReservations, List<BookingResponse> recentActivity) {
        this.totalRevenue = totalRevenue != null ? totalRevenue : 0.0;
        this.totalReservations = totalReservations;
        this.recentActivity = recentActivity;
    }

    // Getters and Setters
    public Double getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(Double totalRevenue) { this.totalRevenue = totalRevenue; }

    public int getTotalReservations() { return totalReservations; }
    public void setTotalReservations(int totalReservations) { this.totalReservations = totalReservations; }

    public List<BookingResponse> getRecentActivity() { return recentActivity; }
    public void setRecentActivity(List<BookingResponse> recentActivity) { this.recentActivity = recentActivity; }
}