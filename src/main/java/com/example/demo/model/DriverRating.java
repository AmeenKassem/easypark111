package com.example.demo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "driver_ratings")
public class DriverRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long bookingId;

    @Column(nullable = false)
    private Long ownerId;

    @Column(nullable = false)
    private Long driverId;

    @Column(nullable = false)
    private Integer score;

    public DriverRating() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }

    public Long getDriverId() { return driverId; }
    public void setDriverId(Long driverId) { this.driverId = driverId; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
}