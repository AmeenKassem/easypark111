package com.example.demo.service;

import com.example.demo.dto.DriverReportResponse;
import com.example.demo.dto.OwnerDashboardResponse;
import com.example.demo.model.Booking;
import com.example.demo.model.BookingStatus;
import com.example.demo.model.Parking;
import com.example.demo.model.User;
import com.example.demo.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private ReportService reportService;

    private User driver;
    private User owner;
    private Parking parking;
    private Booking booking1;
    private Booking booking2;


    private void setEntityId(Object entity, Long id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set ID for testing on entity: " + entity.getClass().getSimpleName(), e);
        }
    }

    @BeforeEach
    void setUp() {
        // ---  Driver ---
        driver = new User();
        setEntityId(driver, 100L);

        // --- Owner ---
        owner = new User();
        setEntityId(owner, 200L);

        // ---  Parking ---
        parking = new Parking();
        setEntityId(parking, 1L);
        parking.setOwnerId(200L);

        // --- Booking 1 (Approved) ---
        booking1 = new Booking();
        setEntityId(booking1, 10L);
        booking1.setDriver(driver);
        booking1.setParking(parking);
        booking1.setStatus(BookingStatus.APPROVED);
        booking1.setTotalPrice(50.0);
        booking1.setStartTime(LocalDateTime.now().minusDays(1));
        booking1.setEndTime(LocalDateTime.now().minusDays(1).plusHours(2));

        // --- Booking 2 (Pending) ---
        booking2 = new Booking();
        setEntityId(booking2, 11L);
        booking2.setDriver(driver);
        booking2.setParking(parking);
        booking2.setStatus(BookingStatus.PENDING);
        booking2.setTotalPrice(0.0);
    }

    // ==========================================
    // OWNER TESTS
    // ==========================================

    @Test
    void getOwnerDashboard_ShouldReturnCorrectData() {
        // Arrange
        Long ownerId = 200L;
        List<Booking> mockBookings = Arrays.asList(booking1, booking2);

        // When findForOwner is called, return our list
        when(bookingRepository.findForOwner(ownerId)).thenReturn(mockBookings);

        // When calculateTotalRevenue is called, return a specific sum
        when(bookingRepository.calculateTotalRevenueForOwner(ownerId)).thenReturn(150.0);

        // Act
        OwnerDashboardResponse response = reportService.getOwnerDashboard(ownerId);

        // Assert
        assertNotNull(response);
        assertEquals(150.0, response.getTotalRevenue(), "Total revenue should match the repository result");
        assertEquals(2, response.getTotalReservations(), "Total reservations count should match list size");

        assertEquals(2, response.getRecentActivity().size(), "Recent activity list should contain mapped DTOs");

        // Verify repository interaction
        verify(bookingRepository).findForOwner(ownerId);
        verify(bookingRepository).calculateTotalRevenueForOwner(ownerId);
    }

    @Test
    void getOwnerDashboard_WhenNoRevenue_ShouldReturnZero() {
        // Arrange
        Long ownerId = 200L;
        when(bookingRepository.findForOwner(ownerId)).thenReturn(Collections.emptyList());

        // Simulate DB returning null (happens when SUM() runs on empty set)
        when(bookingRepository.calculateTotalRevenueForOwner(ownerId)).thenReturn(null);

        // Act
        OwnerDashboardResponse response = reportService.getOwnerDashboard(ownerId);

        // Assert
        assertEquals(0.0, response.getTotalRevenue(), "Should default null revenue to 0.0");
        assertEquals(0, response.getTotalReservations());
    }

    // ==========================================
    // DRIVER TESTS
    // ==========================================

    @Test
    void getDriverReport_ShouldReturnCorrectData() {
        // Arrange
        Long driverId = 100L;
        List<Booking> mockBookings = Collections.singletonList(booking1);

        when(bookingRepository.findMine(driverId)).thenReturn(mockBookings);
        when(bookingRepository.calculateTotalExpensesForDriver(driverId)).thenReturn(50.0);

        // Act
        DriverReportResponse response = reportService.getDriverReport(driverId);

        // Assert
        assertNotNull(response);
        assertEquals(50.0, response.getTotalExpenses(), "Total expenses should match");
        assertEquals(1, response.getTotalBookings());
        assertEquals(booking1.getId(), response.getBookingHistory().get(0).getId());

        verify(bookingRepository).findMine(driverId);
        verify(bookingRepository).calculateTotalExpensesForDriver(driverId);
    }

    @Test
    void getDriverReport_WhenNoExpenses_ShouldReturnZero() {
        // Arrange
        Long driverId = 100L;
        when(bookingRepository.findMine(driverId)).thenReturn(Collections.emptyList());
        when(bookingRepository.calculateTotalExpensesForDriver(driverId)).thenReturn(null);

        // Act
        DriverReportResponse response = reportService.getDriverReport(driverId);

        // Assert
        assertEquals(0.0, response.getTotalExpenses(), "Should default null expenses to 0.0");
        assertEquals(0, response.getTotalBookings());
    }
}