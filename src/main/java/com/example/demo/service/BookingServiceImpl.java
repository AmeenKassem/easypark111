package com.example.demo.service;

import com.example.demo.dto.CreateBookingRequest;
import com.example.demo.dto.UpdateBookingStatusRequest;
import com.example.demo.model.*;
import com.example.demo.repository.BookingRepository;
import com.example.demo.repository.DriverRatingRepository;
import com.example.demo.repository.ParkingRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final ParkingRepository parkingRepository;
    private final UserRepository userRepository;
    private final DriverRatingRepository driverRatingRepository; // NEW

    private static final EnumSet<BookingStatus> ACTIVE_STATUSES =
            EnumSet.of(BookingStatus.PENDING, BookingStatus.APPROVED);

    public BookingServiceImpl(BookingRepository bookingRepository,
                              ParkingRepository parkingRepository,
                              UserRepository userRepository,
                              DriverRatingRepository driverRatingRepository) { // NEW
        this.bookingRepository = bookingRepository;
        this.parkingRepository = parkingRepository;
        this.userRepository = userRepository;
        this.driverRatingRepository = driverRatingRepository; // NEW
    }

    @Override
    public Booking create(Long driverId, CreateBookingRequest req) {
        if (req.getStartTime() == null || req.getEndTime() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startTime and endTime are required");
        }
        if (!req.getStartTime().isBefore(req.getEndTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startTime must be before endTime");
        }

        Parking parking = parkingRepository.findById(req.getParkingId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parking spot not found"));

        if (!parking.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parking spot is not active");
        }

        if (parking.getOwnerId() != null && parking.getOwnerId().equals(driverId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot book your own parking spot");
        }

        validateParkingAvailability(parking, req.getStartTime(), req.getEndTime());

        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Driver not found"));

        long overlaps = bookingRepository.countOverlaps(
                parking.getId(),
                req.getStartTime(),
                req.getEndTime(),
                ACTIVE_STATUSES
        );

        if (overlaps > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Parking spot is already booked for that time range");
        }

        Booking booking = new Booking();
        booking.setParking(parking);
        booking.setDriver(driver);
        booking.setStartTime(req.getStartTime());
        booking.setEndTime(req.getEndTime());
        booking.setStatus(BookingStatus.PENDING);
        booking.setTotalPrice(calculateTotalPrice(parking, req.getStartTime(), req.getEndTime()));

        return bookingRepository.save(booking);
    }

    private void validateParkingAvailability(Parking parking, LocalDateTime start, LocalDateTime end) {
        if (parking.getAvailabilityList() == null || parking.getAvailabilityList().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parking availability configuration is missing.");
        }

        boolean isWithinSlot = false;
        String reason = "Time outside available slots.";

        if (parking.getAvailabilityType() == AvailabilityType.SPECIFIC) {
            for (ParkingAvailability slot : parking.getAvailabilityList()) {
                if (slot.getStartDateTime() != null && slot.getEndDateTime() != null) {
                    if (!start.isBefore(slot.getStartDateTime()) && !end.isAfter(slot.getEndDateTime())) {
                        isWithinSlot = true;
                        break;
                    }
                }
            }
            if (!isWithinSlot) {
                reason = "Selected date is outside the specific availability range.";
            }

        } else if (parking.getAvailabilityType() == AvailabilityType.RECURRING) {
            int javaDay = start.getDayOfWeek().getValue();
            int dbDay = (javaDay == 7) ? 0 : javaDay;

            if (!start.toLocalDate().isEqual(end.toLocalDate())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking cannot span multiple days for recurring parking.");
            }

            LocalTime reqStart = start.toLocalTime();
            LocalTime reqEnd = end.toLocalTime();
            boolean dayFound = false;

            for (ParkingAvailability slot : parking.getAvailabilityList()) {
                if (slot.getDayOfWeek() != null && slot.getDayOfWeek() == dbDay) {
                    dayFound = true;
                    if (slot.getStartTime() != null && slot.getEndTime() != null) {
                        if (!reqStart.isBefore(slot.getStartTime()) && !reqEnd.isAfter(slot.getEndTime())) {
                            isWithinSlot = true;
                            break;
                        } else {
                            reason = String.format("On %s, parking is only available between %s and %s.",
                                    start.getDayOfWeek(), slot.getStartTime(), slot.getEndTime());
                        }
                    }
                }
            }
            if (!dayFound) {
                reason = "Parking is closed on " + start.getDayOfWeek() + ".";
            }
        }

        if (!isWithinSlot) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
        }
    }

    @Override
    public List<Booking> listMine(Long driverId) {
        List<Booking> bookings = bookingRepository.findMine(driverId);
        for (Booking booking : bookings) {
            if (booking.getParking() != null && booking.getParking().getOwnerId() != null) {
                Long ownerId = booking.getParking().getOwnerId();
                userRepository.findById(ownerId).ifPresent(owner -> {
                    booking.setOwnerPhone(owner.getPhone());
                });
            }
        }
        return bookings;
    }

    @Override
    public List<Booking> listForOwner(Long ownerId) {
        List<Booking> bookings = bookingRepository.findForOwner(ownerId);

        // --- NEW: Map which bookings have already been rated by this owner ---
        List<DriverRating> ratings = driverRatingRepository.findByOwnerId(ownerId);
        Set<Long> ratedBookingIds = ratings.stream()
                .map(DriverRating::getBookingId)
                .collect(Collectors.toSet());

        for (Booking booking : bookings) {
            booking.setRatedByOwner(ratedBookingIds.contains(booking.getId()));
        }
        // --------------------------------------------------------------------

        return bookings;
    }

    @Override
    public Booking updateStatus(Long ownerId, Long bookingId, UpdateBookingStatusRequest req) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        Long bookingOwnerId = booking.getParking().getOwnerId();
        if (!ownerId.equals(bookingOwnerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to update this booking");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PENDING bookings can be updated");
        }

        BookingStatus newStatus = parseStatus(req.getStatus());
        if (newStatus != BookingStatus.APPROVED && newStatus != BookingStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status must be APPROVED or REJECTED");
        }

        booking.setStatus(newStatus);
        return bookingRepository.save(booking);
    }

    @Override
    public Booking cancel(Long driverId, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        if (!driverId.equals(booking.getDriver().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to cancel this booking");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.REJECTED) {
            return booking;
        }

        if (!LocalDateTime.now().isBefore(booking.getStartTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot cancel after booking has started");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        return bookingRepository.save(booking);
    }

    private BookingStatus parseStatus(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status is required");
        }
        try {
            return BookingStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: " + raw);
        }
    }

    private double calculateTotalPrice(Parking parking, LocalDateTime start, LocalDateTime end) {
        double pricePerHour = parking.getPricePerHour();
        long minutes = Duration.between(start, end).toMinutes();
        double hoursExact = minutes / 60.0;
        double calculatedPrice = hoursExact * pricePerHour;
        return Math.round(calculatedPrice * 100.0) / 100.0;
    }
}