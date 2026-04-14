package com.example.demo.service;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.example.demo.dto.UserSummary;
import com.example.demo.model.Booking;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Value("${mail.from}")
    private String fromAddress;

    @Value("${sendgrid.api-key}")
    private String sendGridApiKey;

    @Override
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        log.info("action=email_reset start to={}", safeEmail(toEmail));

        String text = "Hi,\n\n"
                + "We received a request to reset your password.\n"
                + "To choose a new password, click the link below:\n"
                + resetLink + "\n\n"
                + "If you did not request a password reset, you can ignore this email.\n";

        try {
            sendPlainEmail(toEmail, "Reset your password", text);
            log.info("action=email_reset success to={}", safeEmail(toEmail));
        } catch (Exception ex) {
            log.error("action=email_reset fail to={} reason={}", safeEmail(toEmail), ex.getClass().getSimpleName(), ex);
            throw new RuntimeException("Failed to send password reset email", ex);
        }
    }

    @Override
    @Async
    public void sendBookingApprovedNotification(String toEmail, Booking booking, UserSummary ownerSummary) {
        log.info("action=email_approved start to={}", safeEmail(toEmail));

        String location = booking.getParking().getLocation();
        String driverName = booking.getDriver().getFullName();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
        String start = booking.getStartTime().format(fmt);
        String end = booking.getEndTime().format(fmt);

        String text = "Dear " + driverName + ",\n\n"
                + "We are pleased to inform you that " + ownerSummary.getFullName() + " has approved your parking booking.\n\n"
                + "Booking details:\n"
                + "• Address: " + location + "\n"
                + "• Time: " + start + " - " + end + "\n\n"
                + "If you have any questions, please reply to this email.\n\n"
                + "Sincerely,\n"
                + "EasyPark Team";

        try {
            sendPlainEmail(toEmail, "Booking Approved", text);
            log.info("action=email_approved success to={}", safeEmail(toEmail));
        } catch (Exception ex) {
            log.error("action=email_approved fail to={} reason={}", safeEmail(toEmail), ex.getClass().getSimpleName(), ex);
        }
    }

    private void sendPlainEmail(String toEmail, String subject, String body) throws IOException {
        Email from = new Email(fromAddress);
        Email to = new Email(toEmail);
        Content content = new Content("text/plain", body);
        Mail mail = new Mail(from, subject, to, content);

        SendGrid sg = new SendGrid(sendGridApiKey);
        Request request = new Request();
        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        request.setBody(mail.build());

        Response response = sg.api(request);

        if (response.getStatusCode() >= 400) {
            throw new IOException("SendGrid failed with status " + response.getStatusCode() + ": " + response.getBody());
        }
    }

    private String safeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}