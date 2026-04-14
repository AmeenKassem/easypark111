package com.example.demo.dto;

public class AuthResponse {

    private String message;
    private String token;
    private UserSummary user;

    public AuthResponse() {
    }
    
    public AuthResponse(String message) {
        this.message = message;
    }

    public AuthResponse(String message, String token, UserSummary user) {
        this.message = message;
        this.token = token;
        this.user = user;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public UserSummary getUser() {
        return user;
    }

    public void setUser(UserSummary user) {
        this.user = user;
    }
}