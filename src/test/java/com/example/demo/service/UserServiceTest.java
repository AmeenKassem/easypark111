package com.example.demo.service;

import com.example.demo.dto.ChangePasswordRequest;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.dto.UserSummary;
import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.repository.PasswordResetTokenRepository;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private BCryptPasswordEncoder passwordEncoder;
    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock
    private GoogleAuthService googleAuthService;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        // הזרקת ערכים ממשתני @Value באמצעות Reflection
        ReflectionTestUtils.setField(userService, "resetTokenExpirationMinutes", 30L);
        ReflectionTestUtils.setField(userService, "resetPasswordUrl", "http://localhost:3000/reset");

        user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setPasswordHash("encoded_password_123");
        user.setRole(Role.DRIVER); // לא משנה כאן כי זה משמש לטסטים של לוגין
        user.setFullName("Test User");
    }

    // --- Register Tests ---

    @Test
    void register_ShouldEncryptPasswordAndSave() {
        // Arrange
        RegisterRequest req = new RegisterRequest();
        req.setEmail("new@example.com");
        req.setPassword("secret123");
        req.setFullName("New User");
        // גם אם נבקש DRIVER, המערכת אמורה לכפות BOTH לפי ההחלטה שלך
        req.setRole(Role.DRIVER);

        when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret123")).thenReturn("hashed_secret_123");

        // כאן אנחנו מדמים את השמירה ומחזירים את האובייקט שנשמר עם ה-Role המעודכן
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(99L);
            return u;
        });

        // Act
        User result = userService.register(req);

        // Assert
        assertNotNull(result);
        assertEquals("hashed_secret_123", result.getPasswordHash(), "Password must be encrypted");

        // --- התיקון: מצפים ל-BOTH בהתאם ללוגיקה ב-Service ---
        assertEquals(Role.BOTH, result.getRole());

        // Verify save was called
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_ShouldFail_WhenEmailExists() {
        // Arrange
        RegisterRequest req = new RegisterRequest();
        req.setEmail("test@example.com");
        req.setRole(Role.DRIVER);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> userService.register(req));
        verify(userRepository, never()).save(any());
    }

    // --- Login Tests ---

    @Test
    void login_ShouldSuccess_WhenPasswordMatches() {
        // Arrange
        LoginRequest req = new LoginRequest();
        req.setEmail("test@example.com");
        req.setPassword("rawPassword");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("rawPassword", user.getPasswordHash())).thenReturn(true);

        // Act
        User result = userService.login(req);

        // Assert
        assertNotNull(result);
        assertEquals(user.getEmail(), result.getEmail());
    }

    @Test
    void login_ShouldFail_WhenPasswordIncorrect() {
        // Arrange
        LoginRequest req = new LoginRequest();
        req.setEmail("test@example.com");
        req.setPassword("wrongPassword");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", user.getPasswordHash())).thenReturn(false);

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.login(req));
        assertEquals("Invalid email or password", ex.getMessage());
    }

    // --- Change Password Tests ---

    @Test
    void changePassword_ShouldVerifyOldAndEncodeNew() {
        // Arrange
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("oldPass");
        req.setNewPassword("newPass123");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPass", user.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.matches("newPass123", user.getPasswordHash())).thenReturn(false);
        when(passwordEncoder.encode("newPass123")).thenReturn("hashed_new_pass");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        UserSummary result = userService.changePassword(1L, req);

        // Assert
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        assertEquals("hashed_new_pass", userCaptor.getValue().getPasswordHash());
    }
}