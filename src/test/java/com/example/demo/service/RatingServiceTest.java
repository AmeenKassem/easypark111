package com.example.demo.service;

import com.example.demo.model.*;
import com.example.demo.repository.BookingRepository;
import com.example.demo.repository.DriverRatingRepository;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RatingServiceTest {

    @Mock
    private DriverRatingRepository driverRatingRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RatingService ratingService;

    private Booking mockBooking;
    private User mockDriver;
    private final Long BOOKING_ID = 1L;
    private final Long OWNER_ID = 100L;
    private final Long DRIVER_ID = 200L;

    @BeforeEach
    void setUp() {
        mockBooking = mock(Booking.class);
        Parking mockParking = mock(Parking.class);
        mockDriver = mock(User.class);

        lenient().when(mockBooking.getParking()).thenReturn(mockParking);
        lenient().when(mockParking.getOwnerId()).thenReturn(OWNER_ID);
        lenient().when(mockBooking.getDriver()).thenReturn(mockDriver);
        lenient().when(mockDriver.getId()).thenReturn(DRIVER_ID);
    }

    @Test
    void rateDriver_Success_SavesRatingAndUpdatesDriverStats() {
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(mockBooking));
        when(mockBooking.getStatus()).thenReturn(BookingStatus.APPROVED);
        when(driverRatingRepository.existsByBookingId(BOOKING_ID)).thenReturn(false);

        when(mockDriver.getAverageRating()).thenReturn(4.0);
        when(mockDriver.getTotalRatings()).thenReturn(2);

        int newScore = 5;

        ratingService.rateDriver(BOOKING_ID, OWNER_ID, newScore);

        ArgumentCaptor<DriverRating> ratingCaptor = ArgumentCaptor.forClass(DriverRating.class);
        verify(driverRatingRepository).save(ratingCaptor.capture());
        DriverRating savedRating = ratingCaptor.getValue();

        assertEquals(BOOKING_ID, savedRating.getBookingId());
        assertEquals(OWNER_ID, savedRating.getOwnerId());
        assertEquals(DRIVER_ID, savedRating.getDriverId());
        assertEquals(newScore, savedRating.getScore());

        verify(mockDriver).setTotalRatings(3);
        verify(mockDriver).setAverageRating(4.3);
        verify(userRepository).save(mockDriver);
    }

    @Test
    void rateDriver_ThrowsNotFound_WhenBookingDoesNotExist() {
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> ratingService.rateDriver(BOOKING_ID, OWNER_ID, 5));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Booking not found", exception.getReason());

        verifyNoInteractions(driverRatingRepository);
        verifyNoInteractions(userRepository);
    }

    @Test
    void rateDriver_ThrowsForbidden_WhenUserIsNotTheParkingOwner() {
        Long differentOwnerId = 999L;
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(mockBooking));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> ratingService.rateDriver(BOOKING_ID, differentOwnerId, 5));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("You are not the owner of this parking spot", exception.getReason());
        verifyNoInteractions(driverRatingRepository);
    }

    @Test
    void rateDriver_ThrowsBadRequest_WhenBookingIsNotApproved() {
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(mockBooking));
        when(mockBooking.getStatus()).thenReturn(BookingStatus.PENDING);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> ratingService.rateDriver(BOOKING_ID, OWNER_ID, 5));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("You can only rate a driver after approving the booking", exception.getReason());
        verifyNoInteractions(driverRatingRepository);
    }

    @Test
    void rateDriver_ThrowsConflict_WhenBookingIsAlreadyRated() {
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(mockBooking));
        when(mockBooking.getStatus()).thenReturn(BookingStatus.APPROVED);
        when(driverRatingRepository.existsByBookingId(BOOKING_ID)).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> ratingService.rateDriver(BOOKING_ID, OWNER_ID, 5));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("This booking has already been rated", exception.getReason());

        verify(driverRatingRepository, never()).save(any());
        verifyNoInteractions(userRepository);
    }
}