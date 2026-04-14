package com.example.demo.controller;

import com.example.demo.dto.CreateBookingRequest;
import com.example.demo.dto.RatingRequest;
import com.example.demo.dto.UpdateBookingStatusRequest;
import com.example.demo.dto.UserSummary;
import com.example.demo.model.Booking;
import com.example.demo.model.BookingStatus;
import com.example.demo.model.Parking;
import com.example.demo.model.User;
import com.example.demo.service.BookingService;
import com.example.demo.service.EmailService;
import com.example.demo.service.RatingService;
import com.example.demo.service.UserService;
import com.example.demo.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookingController.class)
@AutoConfigureMockMvc(addFilters = false)
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingService bookingService;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private RatingService ratingService;

    @MockitoBean
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private void setEntityId(Object entity, Long id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set ID for testing", e);
        }
    }

    @Test
    void create_ShouldReturn200_WhenValid() throws Exception {
        Long driverId = 100L;
        TestingAuthenticationToken auth = new TestingAuthenticationToken(driverId, "PASSWORD", "ROLE_DRIVER");

        CreateBookingRequest req = new CreateBookingRequest();
        req.setParkingId(5L);
        req.setStartTime(LocalDateTime.now().plusHours(1));
        req.setEndTime(LocalDateTime.now().plusHours(3));

        Booking mockBooking = new Booking();
        setEntityId(mockBooking, 555L);
        mockBooking.setStatus(BookingStatus.PENDING);

        Parking p = new Parking();
        setEntityId(p, 5L);
        p.setOwnerId(200L);
        p.setLocation("Tel Aviv");
        mockBooking.setParking(p);

        User u = new User();
        setEntityId(u, driverId);
        mockBooking.setDriver(u);

        mockBooking.setStartTime(req.getStartTime());
        mockBooking.setEndTime(req.getEndTime());

        when(bookingService.create(eq(driverId), any(CreateBookingRequest.class))).thenReturn(mockBooking);

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(555));
    }

    @Test
    void myBookings_ShouldReturn200() throws Exception {
        Long driverId = 100L;
        TestingAuthenticationToken auth = new TestingAuthenticationToken(driverId, "PASSWORD", "ROLE_DRIVER");

        Booking mockBooking = new Booking();
        setEntityId(mockBooking, 555L);
        mockBooking.setStatus(BookingStatus.PENDING);

        when(bookingService.listMine(driverId)).thenReturn(List.of(mockBooking));

        mockMvc.perform(get("/api/bookings/my")
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(555));
    }

    @Test
    void ownerBookings_ShouldReturn200() throws Exception {
        Long ownerId = 200L;
        TestingAuthenticationToken auth = new TestingAuthenticationToken(ownerId, "PASSWORD", "ROLE_OWNER");

        Booking mockBooking = new Booking();
        setEntityId(mockBooking, 555L);
        mockBooking.setStatus(BookingStatus.PENDING);

        when(bookingService.listForOwner(ownerId)).thenReturn(List.of(mockBooking));

        mockMvc.perform(get("/api/bookings/owner")
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(555));
    }

    @Test
    void updateStatus_ShouldReturn200_AndSendEmail_WhenApproved() throws Exception {
        Long ownerId = 200L;
        TestingAuthenticationToken auth = new TestingAuthenticationToken(ownerId, "PASSWORD", "ROLE_OWNER");

        UpdateBookingStatusRequest req = new UpdateBookingStatusRequest();
        req.setStatus(BookingStatus.APPROVED.name());

        Booking mockBooking = new Booking();
        setEntityId(mockBooking, 555L);
        mockBooking.setStatus(BookingStatus.APPROVED);

        User driver = new User();
        setEntityId(driver, 100L);
        driver.setEmail("driver@test.com");
        mockBooking.setDriver(driver);

        UserSummary ownerSummary = Mockito.mock(UserSummary.class);

        when(bookingService.updateStatus(eq(ownerId), eq(555L), any(UpdateBookingStatusRequest.class))).thenReturn(mockBooking);
        when(userService.getUserSummary(ownerId)).thenReturn(ownerSummary);

        mockMvc.perform(put("/api/bookings/555/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(555));

        verify(emailService).sendBookingApprovedNotification(eq("driver@test.com"), eq(mockBooking), eq(ownerSummary));
    }

    @Test
    void cancel_ShouldReturn200() throws Exception {
        Long driverId = 100L;
        TestingAuthenticationToken auth = new TestingAuthenticationToken(driverId, "PASSWORD", "ROLE_DRIVER");

        Booking mockBooking = new Booking();
        setEntityId(mockBooking, 555L);
        mockBooking.setStatus(BookingStatus.CANCELLED);

        when(bookingService.cancel(driverId, 555L)).thenReturn(mockBooking);

        mockMvc.perform(put("/api/bookings/555/cancel")
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(555));
    }

    @Test
    void rateDriver_ShouldReturn200() throws Exception {
        Long ownerId = 200L;
        TestingAuthenticationToken auth = new TestingAuthenticationToken(ownerId, "PASSWORD", "ROLE_OWNER");

        RatingRequest req = new RatingRequest();
        req.setScore(5);

        mockMvc.perform(post("/api/bookings/555/rate-driver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Rating submitted successfully"));

        verify(ratingService).rateDriver(eq(555L), eq(ownerId), eq(5));
    }
}