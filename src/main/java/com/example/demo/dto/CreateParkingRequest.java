package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class CreateParkingRequest {

    @NotBlank(message = "location is required")
    private String location;
    private Double lat;
    private Double lng;

    @Positive(message = "pricePerHour must be positive")
    private double pricePerHour;
    private boolean covered;
    private String availabilityType; // "SPECIFIC" or "RECURRING"

    @Size(max = 80, message = "Description must be up to 80 characters")
    private String description;

    // For specific (dates)
    private List<SpecificSlotDto> specificAvailability;

    // For recurring (weekly)
    private List<RecurringScheduleDto> recurringSchedule;

    // --- Inner DTO Classes ---
    public static class SpecificSlotDto {
        private LocalDateTime start;
        private LocalDateTime end;
        // getters/setters
        public LocalDateTime getStart() { return start; }
        public void setStart(LocalDateTime start) { this.start = start; }
        public LocalDateTime getEnd() { return end; }
        public void setEnd(LocalDateTime end) { this.end = end; }
    }

    public static class RecurringScheduleDto {
        private Integer dayOfWeek;
        private LocalTime start;
        private LocalTime end;
        // getters/setters
        public Integer getDayOfWeek() { return dayOfWeek; }
        public void setDayOfWeek(Integer dayOfWeek) { this.dayOfWeek = dayOfWeek; }
        public LocalTime getStart() { return start; }
        public void setStart(LocalTime start) { this.start = start; }
        public LocalTime getEnd() { return end; }
        public void setEnd(LocalTime end) { this.end = end; }
    }

    // Getters and Setters for main class
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
    public String getAvailabilityType() { return availabilityType; }
    public void setAvailabilityType(String availabilityType) { this.availabilityType = availabilityType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<SpecificSlotDto> getSpecificAvailability() { return specificAvailability; }
    public void setSpecificAvailability(List<SpecificSlotDto> specificAvailability) { this.specificAvailability = specificAvailability; }
    public List<RecurringScheduleDto> getRecurringSchedule() { return recurringSchedule; }
    public void setRecurringSchedule(List<RecurringScheduleDto> recurringSchedule) { this.recurringSchedule = recurringSchedule; }
}