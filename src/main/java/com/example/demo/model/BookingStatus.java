package com.example.demo.model;

// Booking lifecycle statuses
public enum BookingStatus {
    PENDING,    // Waiting for owner approval
    APPROVED,   // Approved by the owner
    REJECTED,   // Rejected by the owner
    CANCELLED   // Cancelled by the driver
}
