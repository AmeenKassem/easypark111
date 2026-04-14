package com.example.demo.service;

import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.example.demo.dto.UserSummary;
import com.example.demo.model.Booking;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from}")
    private String fromAddress;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        // Do NOT log the reset link (it contains the token)
        log.info("action=email_reset start to={}", safeEmail(toEmail));

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("Reset your password");

        String text = "Hi,\n\n"
                + "We received a request to reset your password.\n"
                + "To choose a new password, click the link below:\n"
                + resetLink + "\n\n"
                + "If you did not request a password reset, you can ignore this email.\n";

        message.setText(text);

        try {
            mailSender.send(message);
            log.info("action=email_reset success to={}", safeEmail(toEmail));
        } catch (Exception ex) {
            log.error("action=email_reset fail to={} reason={}", safeEmail(toEmail), ex.getClass().getSimpleName(), ex);
            throw ex;
        }
    }

    private String safeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    @Override
    public void sendBookingApprovedNotification(String toEmail, Booking booking, UserSummary ownerSummary) {
        log.info("action=email_approved start to={}", safeEmail(toEmail));

        String location = booking.getParking().getLocation();
        
        String driverName = booking.getDriver().getFullName();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
        String start = booking.getStartTime().format(fmt);
        String end   = booking.getEndTime().format(fmt);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("Booking Approved");

        String text = "Dear " + driverName + ",\n\n" +
                        "We are pleased to inform you that " + ownerSummary.getFullName() + " has approved your parking booking.\n\n" +
                        "Booking details:\n" +
                        "• Address: " + location + "\n" +
                        "• Time: " + start + " - " + end + "\n\n" +
                        "If you have any questions, please reply to this email.\n\n" +
                        "Sincerely,\n" +
                        "EasyPark Team";

        message.setText(text);

        try {
            mailSender.send(message);
            log.info("action=email_approved success to={}", safeEmail(toEmail));
        } catch (Exception ex) {
            log.error("action=email_approved fail to={} reason={}", safeEmail(toEmail), ex.getClass().getSimpleName(), ex);
            throw ex;
        }
    }
}
