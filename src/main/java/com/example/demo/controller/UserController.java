package com.example.demo.controller;

import com.example.demo.dto.AuthResponse;
import com.example.demo.dto.UpdateUserProfileRequest;
import com.example.demo.dto.UpdateUserRoleRequest;
import com.example.demo.dto.UserSummary;
import com.example.demo.model.User;
import com.example.demo.security.JwtService;
import com.example.demo.service.UserService;
import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.example.demo.dto.ChangePasswordRequest;


@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final JwtService jwtService;

    public UserController(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    private Long currentUserId(Authentication auth) {
        return (Long) auth.getPrincipal(); // set by JwtAuthenticationFilter
    }

    @GetMapping("/me")
    public ResponseEntity<UserSummary> me(Authentication auth) {
        Long userId = currentUserId(auth);
        log.info("action=user_me start userId={}", userId);

        UserSummary out = userService.getUserSummary(userId);

        log.info("action=user_me success userId={}", userId);
        return ResponseEntity.ok(out);
    }

    @PutMapping("/me/password")
    public ResponseEntity<AuthResponse> changeMyPassword(Authentication auth,
                                                         @Valid @RequestBody ChangePasswordRequest req) {
        Long userId = currentUserId(auth);
        log.info("action=user_change_password start userId={}", userId);

        // 1) Change password in DB
        UserSummary updatedSummary = userService.changePassword(userId, req);

        // 2) Refresh token (same logic like updateMe)
        User userEntity = userService.getUserEntity(userId);
        String newToken = jwtService.generateToken(userEntity);

        log.info("action=user_change_password success userId={} token_refreshed=true", userId);
        return ResponseEntity.ok(new AuthResponse("Password updated successfully", newToken, updatedSummary));
    }



    // @PutMapping("/me")
    // public ResponseEntity<UserSummary> updateMe(Authentication auth,
    //                                             @Valid @RequestBody UpdateUserProfileRequest req) {
    //     Long userId = currentUserId(auth);
    //     log.info("action=user_update_me start userId={}", userId);

    //     UserSummary out = userService.updateProfile(userId, req);

    //     log.info("action=user_update_me success userId={}", userId);
    //     return ResponseEntity.ok(out);
    // }

    @PutMapping("/me")
    public ResponseEntity<AuthResponse> updateMe(Authentication auth,
                                                 @Valid @RequestBody UpdateUserProfileRequest req) {
        Long userId = currentUserId(auth);
        log.info("action=user_update_me start userId={}", userId);

        // 1. Update the profile in DB
        UserSummary updatedSummary = userService.updateProfile(userId, req);
        
        // 2. Fetch the full user entity to generate a new token
        User userEntity = userService.getUserEntity(userId);
        
        // 3. Generate a fresh token with updated claims (e.g. name, email)
        String newToken = jwtService.generateToken(userEntity);

        log.info("action=user_update_me success userId={} token_refreshed=true", userId);
        
        // 4. Return the new token to the client
        return ResponseEntity.ok(new AuthResponse("Profile updated successfully", newToken, updatedSummary));
    }

    // @PutMapping("/me/role")
    // public ResponseEntity<UserSummary> updateMyRole(Authentication auth,
    //                                                 @Valid @RequestBody UpdateUserRoleRequest req) {
    //     Long userId = currentUserId(auth);
    //     log.info("action=user_update_role start userId={} role={}", userId, req.getRole());

    //     UserSummary out = userService.updateRole(userId, req);

    //     log.info("action=user_update_role success userId={} role={}", userId, out.getRole());
    //     return ResponseEntity.ok(out);
    // }

    @PutMapping("/me/role")
    public ResponseEntity<AuthResponse> updateMyRole(Authentication auth,
                                                     @Valid @RequestBody UpdateUserRoleRequest req) {
        Long userId = currentUserId(auth);
        log.info("action=user_update_role start userId={} role={}", userId, req.getRole());

        // 1. Update the role in DB
        UserSummary updatedSummary = userService.updateRole(userId, req);
        
        // 2. Fetch the full user entity to generate a new token
        User userEntity = userService.getUserEntity(userId);
        
        // 3. Generate a fresh token with the NEW ROLE in claims
        String newToken = jwtService.generateToken(userEntity);

        log.info("action=user_update_role success userId={} role={} token_refreshed=true", userId, updatedSummary.getRole());
        
        // 4. Return the new token to the client
        return ResponseEntity.ok(new AuthResponse("Role updated successfully", newToken, updatedSummary));
    }

    @GetMapping
    public ResponseEntity<List<UserSummary>> listUsers() {
        log.info("action=user_list start");
        List<UserSummary> out = userService.getAllUsers();
        log.info("action=user_list success count={}", out.size());
        return ResponseEntity.ok(out);
    }
}
