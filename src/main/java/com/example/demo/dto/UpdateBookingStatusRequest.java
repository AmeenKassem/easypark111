package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdateBookingStatusRequest {

    @NotBlank
    private String status; // APPROVED / REJECTED

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
