package com.example.demo.repository;

import com.example.demo.model.ParkingRating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParkingRatingRepository extends JpaRepository<ParkingRating, Long> {
    boolean existsByParkingIdAndUserId(Long parkingId, Long userId);
    Optional<ParkingRating> findByParkingIdAndUserId(Long parkingId, Long userId);
    List<ParkingRating> findByParkingId(Long parkingId);
}