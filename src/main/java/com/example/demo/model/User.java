package com.example.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import com.example.demo.model.Role;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "email")
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String fullName;

    @Email
    @NotBlank
    @Column(nullable = false, unique = true)
    private String email;

    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider authProvider = AuthProvider.LOCAL;

    @Column(name = "provider_user_id")
    private String providerUserId;

    @NotBlank
    @Column(nullable = false)
    private String passwordHash;

    // --- NEW: Driver Rating Fields ---
    @Column(columnDefinition = "double precision default 0.0")
    private Double averageRating = 0.0;

    @Column(columnDefinition = "int default 0")
    private Integer totalRatings = 0;
    // ---------------------------------

    public User() {
    }

    public User(String fullName, String email, String phone, Role role, String passwordHash) {
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.passwordHash = passwordHash;
        this.authProvider = AuthProvider.LOCAL;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public AuthProvider getAuthProvider() { return authProvider; }
    public void setAuthProvider(AuthProvider authProvider) { this.authProvider = authProvider; }

    public String getProviderUserId() { return providerUserId; }
    public void setProviderUserId(String providerUserId) { this.providerUserId = providerUserId; }

    // --- NEW Getters & Setters ---
    public Double getAverageRating() { return averageRating != null ? averageRating : 0.0; }
    public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }

    public Integer getTotalRatings() { return totalRatings != null ? totalRatings : 0; }
    public void setTotalRatings(Integer totalRatings) { this.totalRatings = totalRatings; }
}