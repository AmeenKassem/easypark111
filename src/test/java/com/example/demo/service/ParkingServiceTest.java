package com.example.demo.service;

import com.example.demo.dto.CreateParkingRequest;
import com.example.demo.dto.UpdateParkingRequest;
import com.example.demo.model.AvailabilityType;
import com.example.demo.model.BookingStatus;
import com.example.demo.model.Parking;
import com.example.demo.model.ParkingRating;
import com.example.demo.repository.BookingRepository;
import com.example.demo.repository.ParkingRepository;
import com.example.demo.repository.ParkingRatingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParkingServiceTest {

    @Mock
    private ParkingRepository parkingRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ParkingRatingRepository parkingRatingRepository;

    @InjectMocks
    private ParkingService parkingService;

    private Long ownerId = 200L;
    private Parking parking;

    private void setEntityId(Object entity, Long id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set ID for testing", e);
        }
    }

    @BeforeEach
    void setUp() {
        parking = new Parking();
        setEntityId(parking, 1L);
        parking.setOwnerId(ownerId);
        parking.setActive(true);
        parking.setPricePerHour(10.0);
        parking.setCovered(true);
    }

    @Test
    void create_ShouldSaveAndReturnParking() {
        // Arrange
        CreateParkingRequest req = new CreateParkingRequest();
        req.setLocation("Tel Aviv");
        req.setPricePerHour(20.0);
        req.setCovered(false);

        req.setAvailabilityType("SPECIFIC");
        CreateParkingRequest.SpecificSlotDto slot = new CreateParkingRequest.SpecificSlotDto();
        slot.setStart(LocalDateTime.now().plusDays(1));
        slot.setEnd(LocalDateTime.now().plusDays(2));
        req.setSpecificAvailability(List.of(slot));

        when(parkingRepository.save(any(Parking.class))).thenAnswer(invocation -> {
            Parking p = invocation.getArgument(0);
            setEntityId(p, 55L);
            return p;
        });

        // Act
        Parking result = parkingService.create(ownerId, req);

        // Assert
        assertNotNull(result);
        assertEquals(55L, result.getId());
        assertEquals(ownerId, result.getOwnerId());
        assertEquals(AvailabilityType.SPECIFIC, result.getAvailabilityType());
        assertEquals(1, result.getAvailabilityList().size());

        verify(parkingRepository).save(any(Parking.class));
    }

    @Test
    void update_WhenOwnerMatches_ShouldUpdate() {
        // Arrange
        UpdateParkingRequest req = new UpdateParkingRequest();
        req.setLocation("New Location");
        req.setActive(false);
        req.setPricePerHour(15.0);
        req.setAvailabilityType("RECURRING");

        when(parkingRepository.findById(1L)).thenReturn(Optional.of(parking));
        when(parkingRepository.save(any(Parking.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Parking result = parkingService.update(ownerId, 1L, req);

        // Assert
        assertEquals("New Location", result.getLocation());
        assertFalse(result.isActive());
        assertEquals(15.0, result.getPricePerHour());
        assertEquals(AvailabilityType.RECURRING, result.getAvailabilityType());
    }

    @Test
    void update_WhenNotOwner_ShouldThrowAccessDenied() {
        // Arrange
        Long otherOwnerId = 999L;
        when(parkingRepository.findById(1L)).thenReturn(Optional.of(parking));

        UpdateParkingRequest req = new UpdateParkingRequest();

        // Act & Assert
        assertThrows(AccessDeniedException.class,
                () -> parkingService.update(otherOwnerId, 1L, req));
    }

    @Test
    void rateParking_WhenValidBooking_ShouldUpdateAverageRating() {
        // Arrange
        Long userId = 500L;
        int newRatingValue = 5;

        parking.setAverageRating(3.0);
        parking.setRatingCount(1);

        when(parkingRepository.findById(1L)).thenReturn(Optional.of(parking));
        when(bookingRepository.existsByParkingIdAndDriverIdAndStatus(1L, userId, BookingStatus.APPROVED))
                .thenReturn(true);
        when(parkingRatingRepository.findByParkingIdAndUserId(1L, userId)).thenReturn(Optional.empty());

        // Mock list of ratings for average calculation (3.0 original + 5.0 new = 4.0 average)
        ParkingRating r1 = new ParkingRating(); r1.setRating(3);
        ParkingRating r2 = new ParkingRating(); r2.setRating(5);
        when(parkingRatingRepository.findByParkingId(1L)).thenReturn(Arrays.asList(r1, r2));

        when(parkingRepository.save(any(Parking.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Parking result = parkingService.rateParking(userId, 1L, newRatingValue);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getRatingCount());
        assertEquals(4.0, result.getAverageRating());
        verify(parkingRatingRepository).saveAndFlush(any());
        verify(parkingRepository).save(parking);
    }

    @Test
    void rateParking_WhenNoApprovedBooking_ShouldThrowForbidden() {
        // Arrange
        Long userId = 500L;
        when(parkingRepository.findById(1L)).thenReturn(Optional.of(parking));
        when(bookingRepository.existsByParkingIdAndDriverIdAndStatus(1L, userId, BookingStatus.APPROVED))
                .thenReturn(false);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> parkingService.rateParking(userId, 1L, 4));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void listMine_ShouldReturnList() {
        when(parkingRepository.findByOwnerId(ownerId)).thenReturn(Collections.singletonList(parking));
        List<Parking> result = parkingService.listMine(ownerId);
        assertEquals(1, result.size());
    }

    @Test
    void search_ShouldFilterCorrectly() {
        Parking p1 = new Parking(); p1.setActive(true); p1.setCovered(true); p1.setPricePerHour(10.0);
        Parking p2 = new Parking(); p2.setActive(true); p2.setCovered(false); p2.setPricePerHour(20.0);
        Parking p3 = new Parking(); p3.setActive(false); p3.setCovered(true); p3.setPricePerHour(5.0);

        when(parkingRepository.findAll()).thenReturn(Arrays.asList(p1, p2, p3));

        List<Parking> covered = parkingService.search(true, null, null);
        assertEquals(1, covered.size());

        List<Parking> expensive = parkingService.search(null, 15.0, 25.0);
        assertEquals(1, expensive.size());
    }
}