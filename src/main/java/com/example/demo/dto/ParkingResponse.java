package com.example.demo.dto;

import com.example.demo.model.AvailabilityType;
import com.example.demo.model.Parking;
import com.example.demo.model.ParkingAvailability;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class ParkingResponse {

    private Long id;
    private Long ownerId;
    private String location;
    private Double lat;
    private Double lng;
    private double pricePerHour;
    private boolean covered;
    private boolean active;
    private String availabilityType; // "SPECIFIC" or "RECURRING"
    private String description;
    private List<SpecificSlotResponse> specificAvailability;
    private List<RecurringScheduleResponse> recurringSchedule;
    private double averageRating;
    private int ratingCount;

    public static ParkingResponse from(Parking p) {
        ParkingResponse r = new ParkingResponse();
        r.id = p.getId();
        r.ownerId = p.getOwnerId();
        r.location = p.getLocation();
        r.lat = p.getLat();
        r.lng = p.getLng();
        r.pricePerHour = p.getPricePerHour();
        r.covered = p.isCovered();
        r.active = p.isActive();
        r.averageRating = p.getAverageRating();
        r.ratingCount = p.getRatingCount();
        r.description = p.getDescription();

        // Map Availability
        if (p.getAvailabilityType() != null) {
            r.availabilityType = p.getAvailabilityType().name();

            if (p.getAvailabilityType() == AvailabilityType.SPECIFIC) {
                r.specificAvailability = new ArrayList<>();
                if (p.getAvailabilityList() != null) {
                    for (ParkingAvailability pa : p.getAvailabilityList()) {
                        r.specificAvailability.add(new SpecificSlotResponse(pa.getStartDateTime(), pa.getEndDateTime()));
                    }
                }
            } else if (p.getAvailabilityType() == AvailabilityType.RECURRING) {
                r.recurringSchedule = new ArrayList<>();
                if (p.getAvailabilityList() != null) {
                    for (ParkingAvailability pa : p.getAvailabilityList()) {
                        r.recurringSchedule.add(new RecurringScheduleResponse(pa.getDayOfWeek(), pa.getStartTime(), pa.getEndTime()));
                    }
                }
            }
        }

        return r;
    }

    // --- Inner DTOs for Response ---

    public static class SpecificSlotResponse {
        private LocalDateTime start;
        private LocalDateTime end;

        public SpecificSlotResponse(LocalDateTime start, LocalDateTime end) {
            this.start = start;
            this.end = end;
        }

        public LocalDateTime getStart() { return start; }
        public LocalDateTime getEnd() { return end; }
    }

    public static class RecurringScheduleResponse {
        private Integer dayOfWeek;
        private LocalTime start;
        private LocalTime end;

        public RecurringScheduleResponse(Integer dayOfWeek, LocalTime start, LocalTime end) {
            this.dayOfWeek = dayOfWeek;
            this.start = start;
            this.end = end;
        }

        public Integer getDayOfWeek() { return dayOfWeek; }
        public LocalTime getStart() { return start; }
        public LocalTime getEnd() { return end; }
    }

    // --- Getters for Main Class ---
    public Long getId() { return id; }
    public Long getOwnerId() { return ownerId; }
    public String getLocation() { return location; }
    public Double getLat() { return lat; }
    public Double getLng() { return lng; }
    public double getPricePerHour() { return pricePerHour; }
    public boolean isCovered() { return covered; }
    public boolean isActive() { return active; }
    public String getAvailabilityType() { return availabilityType; }
    public String getDescription() { return description; }
    public List<SpecificSlotResponse> getSpecificAvailability() { return specificAvailability; }
    public List<RecurringScheduleResponse> getRecurringSchedule() { return recurringSchedule; }
    public double getAverageRating() { return averageRating; }
    public int getRatingCount() { return ratingCount; }
}