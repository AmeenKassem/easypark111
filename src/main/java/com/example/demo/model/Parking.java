package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "parkings")
public class Parking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ownerId;

    @Column(nullable = false, length = 256)
    private String location;

    private Double lat;
    private Double lng;

    @Column(nullable = false)
    private double pricePerHour;

    @Column(nullable = false)
    private boolean covered;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private double averageRating = 0.0;

    @Column(nullable = false)
    private int ratingCount = 0;


    @Column(length = 80)
    private String description;

    @Enumerated(EnumType.STRING)
    private AvailabilityType availabilityType; // SPECIFIC or RECURRING

    @OneToMany(mappedBy = "parking", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<ParkingAvailability> availabilityList = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Parking() {}

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public AvailabilityType getAvailabilityType() { return availabilityType; }
    public void setAvailabilityType(AvailabilityType availabilityType) { this.availabilityType = availabilityType; }

    public List<ParkingAvailability> getAvailabilityList() { return availabilityList; }
    public void setAvailabilityList(List<ParkingAvailability> availabilityList) {
        this.availabilityList = availabilityList;
    }

    public void addAvailability(ParkingAvailability pa) {
        availabilityList.add(pa);
        pa.setParking(this);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }


    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }

    public Double getLng() { return lng; }
    public void setLng(Double lng) { this.lng = lng; }
    // ---------------------------

    public double getPricePerHour() { return pricePerHour; }
    public void setPricePerHour(double pricePerHour) { this.pricePerHour = pricePerHour; }

    public boolean isCovered() { return covered; }
    public void setCovered(boolean covered) { this.covered = covered; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }

    public int getRatingCount() { return ratingCount; }
    public void setRatingCount(int ratingCount) { this.ratingCount = ratingCount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}