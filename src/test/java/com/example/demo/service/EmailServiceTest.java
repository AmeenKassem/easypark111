package com.example.demo.service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmailServiceTest {

    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailServiceImpl();

        ReflectionTestUtils.setField(emailService, "fromAddress", "noreply@easypark.com");
        ReflectionTestUtils.setField(emailService, "sendGridApiKey", "test-api-key");
    }

    @Test
    void sendPasswordResetEmail_ShouldConstructAndSendCorrectRequest() throws IOException {
        String toEmail = "user@example.com";
        String resetLink = "http://localhost:8080/reset?token=xyz";

        try (MockedConstruction<SendGrid> mocked = mockConstruction(
                SendGrid.class,
                (mock, context) -> when(mock.api(any(Request.class)))
                        .thenReturn(new Response(202, "", Collections.emptyMap()))
        )) {
            emailService.sendPasswordResetEmail(toEmail, resetLink);

            SendGrid constructed = mocked.constructed().get(0);

            ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
            verify(constructed).api(requestCaptor.capture());

            Request sentRequest = requestCaptor.getValue();

            assertEquals(Method.POST, sentRequest.getMethod());
            assertEquals("mail/send", sentRequest.getEndpoint());

            String body = sentRequest.getBody();
            assertNotNull(body);
            assertTrue(body.contains("noreply@easypark.com"));
            assertTrue(body.contains(toEmail));
            assertTrue(body.contains("Reset your password"));
            assertTrue(body.contains(resetLink));
        }
    }

    @Test
    void sendPasswordResetEmail_ShouldThrow_WhenSendGridFails() throws IOException {
        String toEmail = "user@example.com";
        String resetLink = "http://localhost:8080/reset?token=xyz";

        try (MockedConstruction<SendGrid> mocked = mockConstruction(
                SendGrid.class,
                (mock, context) -> when(mock.api(any(Request.class)))
                        .thenReturn(new Response(500, "boom", Collections.emptyMap()))
        )) {
            RuntimeException ex = assertThrows(
                    RuntimeException.class,
                    () -> emailService.sendPasswordResetEmail(toEmail, resetLink)
            );

            assertTrue(ex.getMessage().contains("Failed to send password reset email"));
        }
    }
}