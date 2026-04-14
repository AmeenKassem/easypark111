package com.example.demo.service;

import com.example.demo.dto.CreateBookingRequest;
import com.example.demo.model.*;
import com.example.demo.repository.BookingRepository;
import com.example.demo.repository.ParkingRepository;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ParkingRepository parkingRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private Long driverId = 100L;
    private Long ownerId = 200L;
    private Long parkingId = 1L;
    private Parking parking;
    private User driver;

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
        driver = new User();
        setEntityId(driver, driverId);

        parking = new Parking();
        setEntityId(parking, parkingId);
        parking.setOwnerId(ownerId);
        parking.setActive(true);
        parking.setPricePerHour(10.0);


        parking.setAvailabilityType(AvailabilityType.SPECIFIC);
        ParkingAvailability availability = new ParkingAvailability();
        availability.setStartDateTime(LocalDateTime.now().minusDays(10));
        availability.setEndDateTime(LocalDateTime.now().plusDays(10));
        parking.setAvailabilityList(List.of(availability));
    }

    @Test
    void create_ShouldSaveBooking_WhenValid() {
        // Arrange
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(3);

        CreateBookingRequest req = new CreateBookingRequest();
        req.setParkingId(parkingId);
        req.setStartTime(start);
        req.setEndTime(end);

        when(parkingRepository.findById(parkingId)).thenReturn(Optional.of(parking));
        when(userRepository.findById(driverId)).thenReturn(Optional.of(driver));
        when(bookingRepository.countOverlaps(eq(parkingId), any(), any(), any())).thenReturn(0L);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            setEntityId(b, 555L);
            return b;
        });

        // Act
        Booking result = bookingService.create(driverId, req);

        // Assert
        assertNotNull(result);
        assertEquals(20.0, result.getTotalPrice()); // 2 hours * 10.0
        assertEquals(BookingStatus.PENDING, result.getStatus());
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void create_ShouldThrow_WhenOutsideAvailability() {
        // Arrange

        parking.getAvailabilityList().get(0).setEndDateTime(LocalDateTime.now().minusHours(5));

        CreateBookingRequest req = new CreateBookingRequest();
        req.setParkingId(parkingId);
        req.setStartTime(LocalDateTime.now());
        req.setEndTime(LocalDateTime.now().plusHours(1));

        when(parkingRepository.findById(parkingId)).thenReturn(Optional.of(parking));

        // Act & Assert
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> bookingService.create(driverId, req));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("outside"));
    }

    @Test
    void listMine_ShouldIncludeOwnerPhone() {
        // Arrange
        Booking b1 = new Booking();
        b1.setParking(parking);

        User owner = new User();
        owner.setPhone("054-1234567");
        setEntityId(owner, ownerId);

        when(bookingRepository.findMine(driverId)).thenReturn(Collections.singletonList(b1));
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));

        // Act
        List<Booking> result = bookingService.listMine(driverId);

        // Assert
        assertFalse(result.isEmpty());
        assertEquals("054-1234567", result.get(0).getOwnerPhone());
        verify(userRepository).findById(ownerId);
    }

    @Test
    void create_ShouldThrow_WhenParkingNotFound() {
        // Arrange
        CreateBookingRequest req = new CreateBookingRequest();
        req.setParkingId(999L);
        req.setStartTime(LocalDateTime.now());
        req.setEndTime(LocalDateTime.now().plusHours(1));

        when(parkingRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResponseStatusException.class, () -> bookingService.create(driverId, req));
    }

    @Test
    void create_ShouldThrow_WhenBookingOwnSpot() {
        // Arrange
        CreateBookingRequest req = new CreateBookingRequest();
        req.setParkingId(parkingId);
        req.setStartTime(LocalDateTime.now());
        req.setEndTime(LocalDateTime.now().plusHours(1));

        when(parkingRepository.findById(parkingId)).thenReturn(Optional.of(parking));

        // Act & Assert (driverId == ownerId)
        assertThrows(ResponseStatusException.class, () -> bookingService.create(ownerId, req));
    }
}