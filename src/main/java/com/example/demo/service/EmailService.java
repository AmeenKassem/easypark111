package com.example.demo.service;

import com.example.demo.dto.UserSummary;
import com.example.demo.model.Booking;

/**
 * Simple abstraction for sending emails from the application.
 */
public interface EmailService {

    /**
     * Sends a password reset email to the given recipient.
     *
     * @param toEmail   recipient email address
     * @param resetLink full URL with the password reset token
     */
    void sendPasswordResetEmail(String toEmail, String resetLink);

    void sendBookingApprovedNotification(String toEmail, Booking booking, UserSummary ownerSummary);
}