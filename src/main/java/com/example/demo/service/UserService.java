package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.model.AuthProvider;
import com.example.demo.model.PasswordResetToken;
import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.repository.PasswordResetTokenRepository;
import com.example.demo.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final GoogleAuthService googleAuthService;
    private final EmailService emailService;

    private static final String TEMP_PASSWORD_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.frontend.reset-password-url}")
    private String resetPasswordUrl;

    @Value("${app.security.reset-token-expiration-minutes:30}")
    private long resetTokenExpirationMinutes;

    public UserService(UserRepository userRepository,
                       BCryptPasswordEncoder passwordEncoder,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       GoogleAuthService googleAuthService,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.googleAuthService = googleAuthService;
        this.emailService = emailService;
    }

    // ============================
    // REGISTER / LOGIN
    // ============================

    @Transactional
    public User register(RegisterRequest request) {
        logger.info("action=register_user start email={} role={}",
                safeEmail(request.getEmail()),
                request.getRole());

        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            logger.warn("action=register_user fail reason=EMAIL_EXISTS email={}", safeEmail(request.getEmail()));
            throw new IllegalArgumentException("Email already in use");
        });

        Role role;
        try {
            if (request.getRole() == null) {
                logger.warn("action=register_user fail reason=MISSING_ROLE email={}", safeEmail(request.getEmail()));
                throw new IllegalArgumentException("Invalid role: null");
            }
            role = Role.valueOf(request.getRole().toString());
        } catch (IllegalArgumentException ex) {
            logger.warn("action=register_user fail reason=INVALID_ROLE email={} role={}",
                    safeEmail(request.getEmail()),
                    request.getRole());
            throw new IllegalArgumentException("Invalid role: " + request.getRole());
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRole(Role.BOTH);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        User saved = userRepository.save(user);

        logger.info("action=register_user success userId={} email={} role={}",
                saved.getId(),
                safeEmail(saved.getEmail()),
                saved.getRole());

        return saved;
    }

    @Transactional(readOnly = true)
    public User login(LoginRequest request) {
        logger.info("action=login start email={}", safeEmail(request.getEmail()));

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    logger.warn("action=login fail reason=USER_NOT_FOUND email={}", safeEmail(request.getEmail()));
                    return new IllegalArgumentException("Invalid email or password");
                });

        // Never log password; only outcome
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            logger.warn("action=login fail reason=BAD_PASSWORD email={}", safeEmail(request.getEmail()));
            throw new IllegalArgumentException("Invalid email or password");
        }

        logger.info("action=login success userId={} email={} role={}",
                user.getId(),
                safeEmail(user.getEmail()),
                user.getRole());

        return user;
    }

    @Transactional(readOnly = true)
    public List<UserSummary> getAllUsers() {
        logger.info("action=list_users start");

        List<UserSummary> list = userRepository.findAll()
                .stream()
                .map(this::toSummary)
                .toList();

        logger.info("action=list_users success count={}", list.size());
        return list;
    }

    // ============================
    // ME / PROFILE
    // ============================

    @Transactional(readOnly = true)
    public UserSummary getUserSummary(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return toSummary(user);
    }

    @Transactional
    public UserSummary updateProfile(Long userId, UpdateUserProfileRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (req.getFullName() != null) {
            String name = req.getFullName().trim();
            if (name.isEmpty()) {
                throw new IllegalArgumentException("fullName cannot be empty");
            }
            user.setFullName(name);
        }

        if (req.getPhone() != null) {
            String phone = req.getPhone().trim();
            user.setPhone(phone.isEmpty() ? null : phone);
        }

        if (req.getEmail() != null) {
            String email = req.getEmail().trim();

            // Allow clearing email
            if (email.isEmpty()) {
                user.setEmail(null);
            } else {
                // Basic sanity validation (do not rely ONLY on this; add @Email in DTO too)
                String normalized = email.toLowerCase();
                if (!normalized.contains("@") || normalized.startsWith("@") || normalized.endsWith("@")) {
                    throw new IllegalArgumentException("Invalid email");
                }

                // If changed -> ensure uniqueness
                String current = user.getEmail() == null ? null : user.getEmail().trim().toLowerCase();
                if (current == null || !current.equals(normalized)) {
                    // if you have findByEmailIgnoreCase, prefer it to exclude self by id
                    boolean taken = userRepository.existsByEmail(normalized);
                    if (taken) {
                        throw new IllegalArgumentException("Email already in use");
                    }
                    user.setEmail(normalized);
                }
            }
        }

        User saved = userRepository.save(user);
        return toSummary(saved);
    }

    @Transactional
    public UserSummary changePassword(Long userId, ChangePasswordRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String current = (req.getCurrentPassword() == null) ? "" : req.getCurrentPassword();
        String next = (req.getNewPassword() == null) ? "" : req.getNewPassword();

        if (current.isBlank()) {
            throw new IllegalArgumentException("currentPassword cannot be empty");
        }
        if (next.isBlank()) {
            throw new IllegalArgumentException("newPassword cannot be empty");
        }
        if (next.length() < 8) {
            throw new IllegalArgumentException("newPassword must be at least 8 characters");
        }

        // IMPORTANT: verify current password
        if (!passwordEncoder.matches(current, user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        // Prevent same password
        if (passwordEncoder.matches(next, user.getPasswordHash())) {
            throw new IllegalArgumentException("New password must be different");
        }

        user.setPasswordHash(passwordEncoder.encode(next));
        User saved = userRepository.save(user);

        return toSummary(saved);
    }



    @Transactional
    public UserSummary updateRole(Long userId, UpdateUserRoleRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Role role;
        try {
            // UpdateUserRoleRequest.role is expected to be a String (e.g., "DRIVER", "OWNER", "BOTH")
            role = Role.valueOf(req.getRole().trim().toUpperCase());
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid role: " + req.getRole());
        }

        user.setRole(role);

        User saved = userRepository.save(user);
        return toSummary(saved);
    }

    // ============================
    // RESET PASSWORD (legacy - not needed anymore)
    // ============================

    @Transactional
    public String resetPassword(String email) {
        logger.info("action=reset_password_legacy start email={}", safeEmail(email));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.warn("action=reset_password_legacy fail reason=USER_NOT_FOUND email={}", safeEmail(email));
                    return new IllegalArgumentException("User not found");
                });

        String tempPassword = "123456";
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        userRepository.save(user);

        logger.info("action=reset_password_legacy success userId={} email={}", user.getId(), safeEmail(email));
        return tempPassword;
    }

    // ============================
    // RESET PASSWORD (token-based)
    // ============================

    @Transactional
    public void createPasswordResetToken(String email) {
        logger.info("action=reset_token_create start email={}", safeEmail(email));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.warn("action=reset_token_create fail reason=USER_NOT_FOUND email={}", safeEmail(email));
                    return new IllegalArgumentException("User with given email does not exist");
                });

        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(resetTokenExpirationMinutes);

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setToken(token);
        resetToken.setExpiresAt(expiresAt);
        resetToken.setUsed(false);

        passwordResetTokenRepository.save(resetToken);

        // Build reset link (DO NOT log it; it contains the token)
        String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
        String resetLink = resetPasswordUrl + "?token=" + encodedToken;

        logger.info("action=reset_token_create success userId={} email={} expiresAt={}",
                user.getId(), safeEmail(user.getEmail()), expiresAt);

        // Email send is logged inside EmailServiceImpl (and must not log the link/token)
        emailService.sendPasswordResetEmail(user.getEmail(), resetLink);
    }

    @Transactional
    public void resetPasswordWithToken(String token, String newPassword) {
        // DO NOT log token or newPassword
        logger.info("action=reset_password_with_token start");

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> {
                    logger.warn("action=reset_password_with_token fail reason=TOKEN_NOT_FOUND");
                    return new IllegalArgumentException("Invalid or unknown reset token");
                });

        if (resetToken.isUsed()) {
            logger.warn("action=reset_password_with_token fail reason=TOKEN_USED userId={}", safeUserId(resetToken));
            throw new IllegalArgumentException("Reset token has already been used");
        }

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            logger.warn("action=reset_password_with_token fail reason=TOKEN_EXPIRED userId={} expiresAt={}",
                    safeUserId(resetToken), resetToken.getExpiresAt());
            throw new IllegalArgumentException("Reset token has expired");
        }

        User user = resetToken.getUser();

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        logger.info("action=reset_password_with_token success userId={} email={}",
                user.getId(), safeEmail(user.getEmail()));
    }

    // ============================
    // GOOGLE LOGIN / REGISTER
    // ============================

    @Transactional
    public User loginWithGoogle(GoogleLoginRequest request) {
        // Do NOT log Google token; only whether it exists
        logger.info("action=google_login_service start hasIdToken={}",
                request != null && request.getGoogleIdToken() != null && !request.getGoogleIdToken().isBlank());

        String idToken = request.getGoogleIdToken();
        if (idToken == null || idToken.isBlank()) {
            logger.warn("action=google_login_service fail reason=MISSING_ID_TOKEN");
            throw new IllegalArgumentException("Missing Google ID token");
        }

        GoogleLoginRequest verifiedData = googleAuthService.verifyToken(idToken);

        String email = verifiedData.getEmail();
        String googleSub = verifiedData.getGoogleUserId();

        if (email == null || email.isBlank()) {
            logger.warn("action=google_login_service fail reason=NO_EMAIL_FROM_GOOGLE");
            throw new IllegalArgumentException("Google did not provide a valid email");
        }
        if (googleSub == null || googleSub.isBlank()) {
            logger.warn("action=google_login_service fail reason=NO_SUB_FROM_GOOGLE email={}", safeEmail(email));
            throw new IllegalArgumentException("Google did not provide a valid user id (sub)");
        }

        request.setEmail(email);
        request.setFullName(verifiedData.getFullName());
        request.setGoogleUserId(googleSub);

        return userRepository.findByEmail(request.getEmail())
                .map(existingUser -> handleExistingGoogleUser(existingUser, request))
                .orElseGet(() -> registerGoogleUserViaGoogle(request));
    }

    private User handleExistingGoogleUser(User user, GoogleLoginRequest request) {
        String incomingGoogleId = request.getGoogleUserId();

        if (user.getProviderUserId() == null) {
            logger.info("action=google_upgrade start userId={} email={}", user.getId(), safeEmail(user.getEmail()));
            user.setAuthProvider(AuthProvider.GOOGLE);
            user.setProviderUserId(incomingGoogleId);
            User saved = userRepository.save(user);
            logger.info("action=google_upgrade success userId={} email={}", saved.getId(), safeEmail(saved.getEmail()));
            return saved;
        }

        if (user.getProviderUserId().equals(incomingGoogleId)) {
            logger.info("action=google_login_service success existing userId={} email={}",
                    user.getId(), safeEmail(user.getEmail()));
            return user;
        }

        logger.warn("action=google_login_service fail reason=PROVIDER_ID_MISMATCH userId={} email={}",
                user.getId(), safeEmail(user.getEmail()));
        throw new IllegalArgumentException("Google account does not match this email");
    }

    private User registerGoogleUserViaGoogle(GoogleLoginRequest request) {
        logger.info("action=google_register start email={}", safeEmail(request.getEmail()));

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone("");
        user.setRole(Role.BOTH);

        user.setAuthProvider(AuthProvider.GOOGLE);
        user.setProviderUserId(request.getGoogleUserId());

        String tempPassword = generateRandomPassword(12);
        user.setPasswordHash(passwordEncoder.encode(tempPassword));

        User saved = userRepository.save(user);

        logger.info("action=google_register success userId={} email={}",
                saved.getId(), safeEmail(saved.getEmail()));
        return saved;
    }

    private String generateRandomPassword(int length) {
        StringBuilder sb = new StringBuilder(length);
        int charsLen = TEMP_PASSWORD_CHARS.length();
        for (int i = 0; i < length; i++) {
            int idx = secureRandom.nextInt(charsLen);
            sb.append(TEMP_PASSWORD_CHARS.charAt(idx));
        }
        return sb.toString();
    }

    private String safeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private Long safeUserId(PasswordResetToken token) {
        try {
            return token.getUser() == null ? null : token.getUser().getId();
        } catch (Exception e) {
            return null;
        }
    }

    private UserSummary toSummary(User user) {
        return new UserSummary(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole() == null ? null : user.getRole().name()
        );
    }

    @Transactional(readOnly = true)
    public User getUserEntity(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
