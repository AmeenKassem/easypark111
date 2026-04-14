package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class UpdateParkingRequest {

    @NotBlank(message = "location is required")
    private String location;

    private Double lat;
    private Double lng;

    @Positive(message = "pricePerHour must be positive")
    private double pricePerHour;

    private boolean covered;
    private boolean active = true;
    private String availabilityType; // "SPECIFIC" or "RECURRING"

    @Size(max = 80, message = "Description must be up to 80 characters")
    private String description;

    private List<CreateParkingRequest.SpecificSlotDto> specificAvailability;
    private List<CreateParkingRequest.RecurringScheduleDto> recurringSchedule;

    // Getters and Setters
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }
    public Double getLng() { return lng; }
    public void setLng(Double lng) { this.lng = lng; }
    public double getPricePerHour() { return pricePerHour; }
    public void setPricePerHour(double pricePerHour) { this.pricePerHour = pricePerHour; }
    public boolean isCovered() { return covered; }
    public void setCovered(boolean covered) { this.covered = covered; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getAvailabilityType() { return availabilityType; }
    public void setAvailabilityType(String availabilityType) { this.availabilityType = availabilityType; }

    public List<CreateParkingRequest.SpecificSlotDto> getSpecificAvailability() { return specificAvailability; }
    public void setSpecificAvailability(List<CreateParkingRequest.SpecificSlotDto> specificAvailability) { this.specificAvailability = specificAvailability; }

    public List<CreateParkingRequest.RecurringScheduleDto> getRecurringSchedule() { return recurringSchedule; }
    public void setRecurringSchedule(List<CreateParkingRequest.RecurringScheduleDto> recurringSchedule) { this.recurringSchedule = recurringSchedule; }
}