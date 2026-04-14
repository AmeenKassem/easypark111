package com.example.demo.model;

public enum AuthProvider {
    LOCAL,   // Regular email + password registration
    GOOGLE,  // Google OAuth2 login
    APPLE    // Apple ID login (future)
}