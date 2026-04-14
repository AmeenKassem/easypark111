package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.model.User;
import com.example.demo.service.UserService;
import com.example.demo.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;
    private final JwtService jwtService;

    public AuthController(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                                 HttpServletRequest httpReq) {
        log.info("action=register start email={} role={} ip={}",
                safeEmail(request.getEmail()),
                request.getRole(),
                httpReq.getRemoteAddr());

        User user = userService.register(request);
        String token = jwtService.generateToken(user);

        UserSummary summary = new UserSummary(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole().name()
        );

        AuthResponse response = new AuthResponse("Registration successful", token, summary);

        log.info("action=register success userId={} email={} role={}",
                user.getId(), safeEmail(user.getEmail()), user.getRole());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest httpReq) {
        log.info("action=login start email={} ip={}", safeEmail(request.getEmail()), httpReq.getRemoteAddr());

        User user = userService.login(request);
        String token = jwtService.generateToken(user);

        UserSummary summary = new UserSummary(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole().name()
        );

        AuthResponse response = new AuthResponse("Login successful", token, summary);

        log.info("action=login success userId={} email={} role={}",
                user.getId(), safeEmail(user.getEmail()), user.getRole());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserSummary>> getUsers(HttpServletRequest httpReq) {
        log.info("action=list_users start ip={}", httpReq.getRemoteAddr());
        List<UserSummary> users = userService.getAllUsers();
        log.info("action=list_users success count={}", users.size());
        return ResponseEntity.ok(users);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<AuthResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request,
                                                       HttpServletRequest httpReq) {
        log.info("action=forgot_password start email={} ip={}",
                safeEmail(request.getEmail()), httpReq.getRemoteAddr());

        try {
            userService.createPasswordResetToken(request.getEmail());
            log.info("action=forgot_password processed email={}", safeEmail(request.getEmail()));
        } catch (IllegalArgumentException ex) {
            // Intentionally do not reveal if email exists
            log.info("action=forgot_password processed email={} note=generic_response", safeEmail(request.getEmail()));
        }

        return ResponseEntity.ok(new AuthResponse(
                "If this email exists in our system, a reset link has been sent."
        ));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<AuthResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request,
                                                      HttpServletRequest httpReq) {
        // Do NOT log the token or the new password
        log.info("action=reset_password start ip={}", httpReq.getRemoteAddr());

        userService.resetPasswordWithToken(request.getToken(), request.getNewPassword());

        log.info("action=reset_password success");
        return ResponseEntity.ok(new AuthResponse("Password has been reset successfully"));
    }

    @PostMapping("/google-login")
    public ResponseEntity<AuthResponse> googleLogin(@RequestBody GoogleLoginRequest request,
                                                    HttpServletRequest httpReq) {
        log.info("action=google_login start ip={} hasIdToken={}",
                httpReq.getRemoteAddr(),
                request != null && request.getGoogleIdToken() != null && !request.getGoogleIdToken().isBlank());

        try {
            User user = userService.loginWithGoogle(request);
            String token = jwtService.generateToken(user);

            UserSummary summary = new UserSummary(
                    user.getId(),
                    user.getFullName(),
                    user.getEmail(),
                    user.getPhone(),
                    user.getRole().name()
            );

            log.info("action=google_login success userId={} email={} role={}",
                    user.getId(), safeEmail(user.getEmail()), user.getRole());

            return ResponseEntity.ok(new AuthResponse("Login successful", token, summary));

        } catch (IllegalArgumentException ex) {
            log.warn("action=google_login fail reason=BAD_REQUEST msg={}", ex.getMessage());
            return ResponseEntity.badRequest().body(new AuthResponse(ex.getMessage()));
        } catch (Exception ex) {
            log.error("action=google_login fail reason=SERVER_ERROR", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AuthResponse("Google login failed"));
        }
    }

    private String safeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
