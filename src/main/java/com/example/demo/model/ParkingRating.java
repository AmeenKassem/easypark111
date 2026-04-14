package com.example.demo.model;

import jakarta.persistence.*;

@Entity
@Table(
        name = "parking_ratings",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"parking_id", "user_id"})
        }
)
public class ParkingRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "parking_id", nullable = false)
    private Long parkingId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private int rating;

    public ParkingRating() {}

    public Long getId() { return id; }

    public Long getParkingId() { return parkingId; }
    public void setParkingId(Long parkingId) { this.parkingId = parkingId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
}