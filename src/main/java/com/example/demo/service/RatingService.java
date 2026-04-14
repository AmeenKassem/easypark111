package com.example.demo.service;

import com.example.demo.model.Booking;
import com.example.demo.model.BookingStatus;
import com.example.demo.model.DriverRating;
import com.example.demo.model.User;
import com.example.demo.repository.BookingRepository;
import com.example.demo.repository.DriverRatingRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RatingService {

    private final DriverRatingRepository driverRatingRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    public RatingService(DriverRatingRepository driverRatingRepository,
                         BookingRepository bookingRepository,
                         UserRepository userRepository) {
        this.driverRatingRepository = driverRatingRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void rateDriver(Long bookingId, Long ownerId, Integer score) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        if (!booking.getParking().getOwnerId().equals(ownerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the owner of this parking spot");
        }

        if (booking.getStatus() != BookingStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You can only rate a driver after approving the booking");
        }

        if (driverRatingRepository.existsByBookingId(bookingId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This booking has already been rated");
        }

        DriverRating rating = new DriverRating();
        rating.setBookingId(bookingId);
        rating.setOwnerId(ownerId);
        rating.setDriverId(booking.getDriver().getId());
        rating.setScore(score);
        driverRatingRepository.save(rating);

        User driver = booking.getDriver();
        double currentAverage = driver.getAverageRating();
        int totalRatings = driver.getTotalRatings();

        double newTotalScore = (currentAverage * totalRatings) + score;
        int newTotalRatings = totalRatings + 1;
        double newAverage = newTotalScore / newTotalRatings;

        // Round to 1 decimal place (e.g., 4.5)
        newAverage = Math.round(newAverage * 10.0) / 10.0;

        driver.setTotalRatings(newTotalRatings);
        driver.setAverageRating(newAverage);
        userRepository.save(driver);
    }
}