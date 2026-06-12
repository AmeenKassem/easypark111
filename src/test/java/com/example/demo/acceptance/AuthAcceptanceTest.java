package com.example.demo.acceptance;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthAcceptanceTest extends AcceptanceTestBase {

    @Test
    void AT01_userCanRegisterSuccessfully() throws Exception {
        String email = uniqueEmail("driver");

        String body = """
                {
                  "fullName": "Driver Test",
                  "email": "%s",
                  "phone": "0501234567",
                  "password": "Password123!",
                  "role": "DRIVER"
                }
                """.formatted(email);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Registration successful"))
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.role").value("BOTH"));
    }

    @Test
    void AT02_userCanLoginSuccessfully() throws Exception {
        String email = uniqueEmail("login");

        registerAndGetToken(email, "DRIVER");

        String body = """
                {
                  "email": "%s",
                  "password": "Password123!"
                }
                """.formatted(email);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.user.email").value(email));
    }

    @Test
    void AT03_invalidLoginIsRejected() throws Exception {
        String body = """
                {
                  "email": "not-existing@easypark.test",
                  "password": "wrong"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}