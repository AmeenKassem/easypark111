package com.example.demo.repository;

import com.example.demo.model.DriverRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DriverRatingRepository extends JpaRepository<DriverRating, Long> {
    boolean existsByBookingId(Long bookingId);
    List<DriverRating> findByOwnerId(Long ownerId);
}