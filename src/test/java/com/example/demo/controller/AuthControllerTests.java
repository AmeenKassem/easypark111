package com.example.demo.controller;

import com.example.demo.dto.ForgotPasswordRequest;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.dto.ResetPasswordRequest;
import com.example.demo.model.Role;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtService;
import com.example.demo.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test; // שים לב לשימוש ב-junit.jupiter.api
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;



    private RegisterRequest buildRegisterRequest(
            String fullName, String email, String phone, String password, Role role) {
        RegisterRequest req = new RegisterRequest();
        req.setFullName(fullName);
        req.setEmail(email);
        req.setPhone(phone);
        req.setPassword(password);
        req.setRole(role);
        return req;
    }

    @Test
    void registerEndpoint_withValidData_returns200AndPersistsUser() throws Exception {
        RegisterRequest request = buildRegisterRequest(
                "API User",
                "api@example.com",
                "050-5555555",
                "Password1!",
                Role.DRIVER
        );

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Registration successful"));

        assertThat(userRepository.existsByEmail("api@example.com")).isTrue();
    }

    @Test
    void registerEndpoint_withInvalidEmail_returns400() throws Exception {

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("fullName", "");
        requestMap.put("email", "not-an-email");
        requestMap.put("phone", "");
        requestMap.put("password", "");
        requestMap.put("role", "DRIVER");

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestMap))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").exists());
    }

    @Test
    void loginEndpoint_withCorrectCredentials_returns200() throws Exception {
        // Arrange
        RegisterRequest reg = buildRegisterRequest(
                "Login API User",
                "loginapi@example.com",
                "050-6666666",
                "Password1!",
                Role.BOTH
        );
        userService.register(reg);

        LoginRequest login = new LoginRequest();
        login.setEmail("loginapi@example.com");
        login.setPassword("Password1!");

        // Act & Assert
        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(login))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void loginEndpoint_withWrongPassword_returns400() throws Exception {
        // Arrange
        RegisterRequest reg = buildRegisterRequest(
                "Wrong Pass User",
                "wrongpassapi@example.com",
                "050-7777777",
                "Password1!",
                Role.DRIVER
        );
        userService.register(reg);

        LoginRequest login = new LoginRequest();
        login.setEmail("wrongpassapi@example.com");
        login.setPassword("WrongPassword!");

        // Act & Assert

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(login))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void forgotPassword_alwaysReturnsOk() throws Exception {
        // Arrange
        userService.register(buildRegisterRequest(
                "User Forgot",
                "userforgot@example.com",
                "050-1234567",
                "Password1!",
                Role.DRIVER
        ));

        ForgotPasswordRequest forgotRequest = new ForgotPasswordRequest();
        forgotRequest.setEmail("userforgot@example.com");

        // Act & Assert
        mockMvc.perform(
                        post("/api/auth/forgot-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(forgotRequest))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void resetPassword_withInvalidToken_fails() throws Exception {
        ResetPasswordRequest resetRequest = new ResetPasswordRequest();
        resetRequest.setToken("invalid-token");
        resetRequest.setNewPassword("NewPassword123");

        mockMvc.perform(
                        post("/api/auth/reset-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(resetRequest))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUsersEndpoint_withValidToken_returnsRegisteredUsers() throws Exception {
        // Arrange
        var userA = userService.register(buildRegisterRequest(
                "User A", "userA@example.com", "050-9999999", "Password1!", Role.DRIVER));

        userService.register(buildRegisterRequest(
                "User B", "userB@example.com", "050-1010101", "Password1!", Role.OWNER));

        // Generate Token
        String token = jwtService.generateToken(userA);

        // Act & Assert
        mockMvc.perform(
                        get("/api/auth/users")
                                .header("Authorization", "Bearer " + token)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].email").exists());
    }
}