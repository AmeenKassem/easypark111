package com.example.demo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        // מדמה את הערך ב-application.properties
        ReflectionTestUtils.setField(emailService, "fromAddress", "noreply@easypark.com");
    }

    @Test
    void sendPasswordResetEmail_ShouldConstructAndSendCorrectMessage() {
        // Arrange
        String toEmail = "user@example.com";
        String resetLink = "http://localhost:8080/reset?token=xyz";

        // Act
        emailService.sendPasswordResetEmail(toEmail, resetLink);

        // Assert
        // אנחנו תופסים את האובייקט שנשלח ל-mailSender כדי לבדוק אותו
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();

        assertEquals("noreply@easypark.com", sentMessage.getFrom());
        assertEquals(toEmail, sentMessage.getTo()[0]);
        assertEquals("Reset your password", sentMessage.getSubject());

        // בודקים שגוף ההודעה מכיל את הלינק
        assertTrue(sentMessage.getText().contains(resetLink), "Email body must contain the reset link");
    }
}