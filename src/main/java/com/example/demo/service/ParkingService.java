package com.example.demo.service;

import com.example.demo.dto.BookedIntervalResponse;
import com.example.demo.dto.CreateParkingRequest;
import com.example.demo.dto.UpdateParkingRequest;
import com.example.demo.model.*;
import com.example.demo.repository.BookingRepository;
import com.example.demo.repository.ParkingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import com.example.demo.repository.ParkingRatingRepository;

@Service
public class ParkingService {

    private static final Logger log = LoggerFactory.getLogger(ParkingService.class);

    private final ParkingRepository parkingRepository;
    private final BookingRepository bookingRepository;
    private final ParkingRatingRepository parkingRatingRepository;
    private static final Collection<BookingStatus> BUSY_STATUSES =
            List.of(BookingStatus.PENDING, BookingStatus.APPROVED);

    public ParkingService(ParkingRepository parkingRepository,
                          BookingRepository bookingRepository,
                          ParkingRatingRepository parkingRatingRepository) {
        this.parkingRepository = parkingRepository;
        this.bookingRepository = bookingRepository;
        this.parkingRatingRepository = parkingRatingRepository;
    }

    @Transactional
    public Parking create(Long ownerId, CreateParkingRequest req) {
        Parking p = new Parking();
        p.setOwnerId(ownerId);
        p.setLocation(req.getLocation());
        p.setLat(req.getLat());
        p.setLng(req.getLng());
        p.setPricePerHour(req.getPricePerHour());
        p.setCovered(req.isCovered());
        p.setActive(true);

        // --- שמירת שדה התיאור האופציונלי ---
        p.setDescription(req.getDescription());

        handleAvailability(p, req.getAvailabilityType(), req.getSpecificAvailability(), req.getRecurringSchedule());

        Parking saved = parkingRepository.save(p);
        log.info("action=parking_create_service success ownerId={} parkingId={}", ownerId, saved.getId());
        return saved;
    }

    @Transactional
    public Parking update(Long ownerId, Long parkingId, UpdateParkingRequest req) {
        log.debug("action=parking_update_service start ownerId={} parkingId={}", ownerId, parkingId);

        Parking p = parkingRepository.findById(parkingId)
                .orElseThrow(() -> new IllegalArgumentException("Parking spot not found"));

        if (!p.getOwnerId().equals(ownerId)) {
            throw new AccessDeniedException("You are not the owner of this parking spot");
        }

        // Update basic fields
        p.setLocation(req.getLocation());
        p.setLat(req.getLat());
        p.setLng(req.getLng());
        p.setPricePerHour(req.getPricePerHour());
        p.setCovered(req.isCovered());
        p.setActive(req.isActive());

        // --- עדכון שדה התיאור האופציונלי ---
        p.setDescription(req.getDescription());

        // --- UPDATE AVAILABILITY LOGIC ---
        // Only overwrite availability if the client explicitly sent availabilityType.
        if (req.getAvailabilityType() != null) {
            p.getAvailabilityList().clear(); // orphanRemoval deletes rows
            handleAvailability(p,
                    req.getAvailabilityType(),
                    req.getSpecificAvailability(),
                    req.getRecurringSchedule());
        }

        Parking saved = parkingRepository.save(p);
        log.info("action=parking_update_service success ownerId={} parkingId={}", ownerId, saved.getId());
        return saved;
    }

    // Helper method to reuse logic between Create and Update
    private void handleAvailability(Parking p, String typeStr,
                                    List<CreateParkingRequest.SpecificSlotDto> specificSlots,
                                    List<CreateParkingRequest.RecurringScheduleDto> recurringSlots) {
        if (typeStr != null) {
            try {
                AvailabilityType type = AvailabilityType.valueOf(typeStr.toUpperCase());
                p.setAvailabilityType(type);

                if (type == AvailabilityType.SPECIFIC && specificSlots != null) {
                    for (CreateParkingRequest.SpecificSlotDto slot : specificSlots) {
                        ParkingAvailability pa = new ParkingAvailability();
                        pa.setStartDateTime(slot.getStart());
                        pa.setEndDateTime(slot.getEnd());
                        p.addAvailability(pa);
                    }
                } else if (type == AvailabilityType.RECURRING && recurringSlots != null) {
                    for (CreateParkingRequest.RecurringScheduleDto slot : recurringSlots) {
                        ParkingAvailability pa = new ParkingAvailability();
                        pa.setDayOfWeek(slot.getDayOfWeek());
                        pa.setStartTime(slot.getStart());
                        pa.setEndTime(slot.getEnd());
                        p.addAvailability(pa);
                    }
                }
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid availability type");
            }
        }
    }

    public void delete(Long ownerId, Long parkingId) {
        Parking p = parkingRepository.findById(parkingId)
                .orElseThrow(() -> new IllegalArgumentException("Parking spot not found"));
        if (!p.getOwnerId().equals(ownerId)) {
            throw new AccessDeniedException("You are not the owner");
        }
        parkingRepository.delete(p);
    }

    public List<Parking> listMine(Long ownerId) {
        return parkingRepository.findByOwnerId(ownerId);
    }

    public List<Parking> search(Boolean covered, Double minPrice, Double maxPrice,
                                LocalDateTime from, LocalDateTime to) {

        // base filters + availability by bookings
        if (from != null && to != null) {
            if (!from.isBefore(to)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from must be before to");
            }
            return parkingRepository.searchAvailable(
                    covered, minPrice, maxPrice,
                    from, to,
                    BUSY_STATUSES
            );
        }

        // fallback: no time filter, return active + basic filters
        return parkingRepository.findAll().stream()
                .filter(Parking::isActive)
                .filter(p -> covered == null || p.isCovered() == covered)
                .filter(p -> minPrice == null || p.getPricePerHour() >= minPrice)
                .filter(p -> maxPrice == null || p.getPricePerHour() <= maxPrice)
                .toList();
    }

    public List<Parking> search(Boolean covered, Double minPrice, Double maxPrice) {
        return search(covered, minPrice, maxPrice, null, null);
    }

    public List<BookedIntervalResponse> getBusyIntervals(Long parkingId, LocalDateTime from, LocalDateTime to) {
        Parking p = parkingRepository.findById(parkingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parking spot not found"));

        // Default to +/- 1 year if params missing
        LocalDateTime effectiveFrom = (from != null) ? from : LocalDateTime.now().minusYears(1);
        LocalDateTime effectiveTo = (to != null) ? to : LocalDateTime.now().plusYears(1);

        if (!effectiveFrom.isBefore(effectiveTo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from must be before to");
        }

        List<Booking> overlaps = bookingRepository.findOverlaps(parkingId, effectiveFrom, effectiveTo, BUSY_STATUSES);
        return overlaps.stream().map(BookedIntervalResponse::from).toList();
    }

    @Transactional
    public Parking rateParking(Long userId, Long parkingId, int rating) {
        Parking p = parkingRepository.findById(parkingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parking spot not found"));

        if (rating < 1 || rating > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rating must be between 1 and 5");
        }

        boolean hasApprovedBooking = bookingRepository
                .existsByParkingIdAndDriverIdAndStatus(parkingId, userId, BookingStatus.APPROVED);

        if (!hasApprovedBooking) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can rate this parking spot only if your booking was approved"
            );
        }

        ParkingRating parkingRating = parkingRatingRepository
                .findByParkingIdAndUserId(parkingId, userId)
                .orElse(null);

        boolean isUpdate = parkingRating != null;

        if (!isUpdate) {
            parkingRating = new ParkingRating();
            parkingRating.setParkingId(parkingId);
            parkingRating.setUserId(userId);
        }

        parkingRating.setRating(rating);
        parkingRatingRepository.saveAndFlush(parkingRating);

        List<ParkingRating> allRatings = parkingRatingRepository.findByParkingId(parkingId);

        int count = allRatings.size();
        double sum = allRatings.stream()
                .mapToInt(ParkingRating::getRating)
                .sum();

        double average = count == 0 ? 0.0 : sum / count;

        p.setRatingCount(count);
        p.setAverageRating(average);

        Parking saved = parkingRepository.save(p);

        log.info("action=parking_rate success userId={} parkingId={} rating={} updated={} newAverage={} ratingCount={}",
                userId, parkingId, rating, isUpdate, saved.getAverageRating(), saved.getRatingCount());

        return saved;
    }
}