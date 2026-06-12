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
import com.example.demo.service.NotificationService;
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
import org.springframework.test.web.servlet.MvcResult;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    private NotificationService notificationService;

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
    void create_ShouldHandleConcurrency_WhenMultipleUsersBookSameParking() throws Exception {
        int numberOfThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);

        CountDownLatch readyLatch = new CountDownLatch(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successfulBookings = new AtomicInteger(0);
        AtomicInteger failedBookings = new AtomicInteger(0);

        CreateBookingRequest req = new CreateBookingRequest();
        req.setParkingId(5L);
        req.setStartTime(LocalDateTime.now().plusHours(1));
        req.setEndTime(LocalDateTime.now().plusHours(3));

        String reqJson = objectMapper.writeValueAsString(req);

        AtomicBoolean isBooked = new AtomicBoolean(false);

        when(bookingService.create(any(Long.class), any(CreateBookingRequest.class))).thenAnswer(invocation -> {
            if (isBooked.compareAndSet(false, true)) {
                Booking mockBooking = new Booking();
                setEntityId(mockBooking, 555L);
                mockBooking.setStatus(BookingStatus.PENDING);
                return mockBooking;
            } else {
                throw new RuntimeException("Race condition: Parking is already booked!");
            }
        });

        for (int i = 0; i < numberOfThreads; i++) {
            final Long driverId = 100L + i;

            executor.submit(() -> {
                try {
                    TestingAuthenticationToken auth = new TestingAuthenticationToken(driverId, "PASSWORD", "ROLE_DRIVER");

                    readyLatch.countDown();
                    startLatch.await();

                    MvcResult result = mockMvc.perform(post("/api/bookings")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(reqJson)
                                    .principal(auth))
                            .andReturn();

                    if (result.getResponse().getStatus() == 200) {
                        successfulBookings.incrementAndGet();
                    } else {
                        failedBookings.incrementAndGet();
                    }
                } catch (Exception e) {
                    failedBookings.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        doneLatch.await();

        executor.shutdown();

        assertEquals(1, successfulBookings.get(), "Only exactly ONE booking should succeed");
        assertEquals(numberOfThreads - 1, failedBookings.get(), "All other concurrent requests should fail");
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
void cancel_ShouldReturn200() throws Exception {
    Long driverId = 100L;
    TestingAuthenticationToken auth = new TestingAuthenticationToken(driverId, "PASSWORD", "ROLE_DRIVER");

    Booking mockBooking = new Booking();
    setEntityId(mockBooking, 555L);
    mockBooking.setStatus(BookingStatus.CANCELLED);

    Parking parking = new Parking();
    setEntityId(parking, 5L);
    parking.setOwnerId(200L);
    parking.setLocation("Tel Aviv");
    mockBooking.setParking(parking);

    User driver = new User();
    setEntityId(driver, driverId);
    driver.setEmail("driver@test.com");
    mockBooking.setDriver(driver);

    when(bookingService.cancel(driverId, 555L)).thenReturn(mockBooking);

    mockMvc.perform(put("/api/bookings/555/cancel")
                    .principal(auth))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(555));
}
}