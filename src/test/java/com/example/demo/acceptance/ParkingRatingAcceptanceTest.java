package com.example.demo.acceptance;

import com.example.demo.service.EmailService;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ParkingRatingAcceptanceTest extends AcceptanceTestBase {

    @MockBean
    private EmailService emailService;

    private long createParking(String ownerToken, String location) throws Exception {
        String body = """
                {
                  "location": "%s",
                  "lat": 31.2622,
                  "lng": 34.8015,
                  "pricePerHour": 12.5,
                  "covered": true,
                  "availabilityType": "SPECIFIC",
                  "description": "Test parking",
                  "specificAvailability": [
                    {
                      "start": "2035-06-01T08:00:00",
                      "end": "2035-06-01T20:00:00"
                    }
                  ],
                  "recurringSchedule": null
                }
                """.formatted(location);

        String response = mockMvc.perform(post("/api/parking-spots")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.availabilityType").value("SPECIFIC"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    private JsonNode createBooking(
            String driverToken,
            long parkingId,
            String startTime,
            String endTime
    ) throws Exception {
        String body = """
                {
                  "parkingId": %d,
                  "startTime": "%s",
                  "endTime": "%s"
                }
                """.formatted(parkingId, startTime, endTime);

        String response = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", bearer(driverToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.parkingId").value((int) parkingId))
                .andExpect(jsonPath("$.status", notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response);
    }

    private JsonNode approveBooking(String ownerToken, long bookingId) throws Exception {
        String body = """
                {
                  "status": "APPROVED"
                }
                """;

        String response = mockMvc.perform(put("/api/bookings/" + bookingId + "/status")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value((int) bookingId))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response);
    }

    private long createApprovedBooking(
            String ownerToken,
            String driverToken,
            long parkingId,
            String startTime,
            String endTime
    ) throws Exception {
        long bookingId = createBooking(driverToken, parkingId, startTime, endTime)
                .get("id")
                .asLong();

        approveBooking(ownerToken, bookingId);

        return bookingId;
    }

    private JsonNode rateParking(String driverToken, long parkingId, int rating) throws Exception {
        String body = """
                {
                  "rating": %d
                }
                """.formatted(rating);

        String response = mockMvc.perform(post("/api/parking-spots/" + parkingId + "/rate")
                        .header("Authorization", bearer(driverToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value((int) parkingId))
                .andExpect(jsonPath("$.averageRating", notNullValue()))
                .andExpect(jsonPath("$.ratingCount", notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response);
    }

    private String listNotifications(String token) throws Exception {
        return mockMvc.perform(get("/api/notifications")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private boolean responseArrayContainsTitle(String responseBody, String expectedTitle) throws Exception {
        JsonNode array = objectMapper.readTree(responseBody);
        String expectedLower = expectedTitle.toLowerCase();

        for (JsonNode item : array) {
            String actualTitle = item.path("title").asText("").toLowerCase();

            if (actualTitle.equals(expectedLower) || actualTitle.contains(expectedLower)) {
                return true;
            }
        }

        return false;
    }

    @Test
    void AT71_driverCanRateParkingAfterApprovedBooking() throws Exception {
        String ownerToken = registerAndGetToken(uniqueEmail("owner"), "OWNER");
        String driverToken = registerAndGetToken(uniqueEmail("driver"), "DRIVER");

        long parkingId = createParking(ownerToken, "AT71 Rate Parking Spot");

        createApprovedBooking(
                ownerToken,
                driverToken,
                parkingId,
                "2035-06-01T08:00:00",
                "2035-06-01T09:00:00"
        );

        mockMvc.perform(post("/api/parking-spots/" + parkingId + "/rate")
                        .header("Authorization", bearer(driverToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rating": 5
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value((int) parkingId))
                .andExpect(jsonPath("$.averageRating", closeTo(5.0, 0.001)))
                .andExpect(jsonPath("$.ratingCount").value(1));
    }

    @Test
    void AT72_driverCanUpdateOwnParkingRatingWithoutDuplicatingCount() throws Exception {
        String ownerToken = registerAndGetToken(uniqueEmail("owner"), "OWNER");
        String driverToken = registerAndGetToken(uniqueEmail("driver"), "DRIVER");

        long parkingId = createParking(ownerToken, "AT72 Update Rating Spot");

        createApprovedBooking(
                ownerToken,
                driverToken,
                parkingId,
                "2035-06-01T09:00:00",
                "2035-06-01T10:00:00"
        );

        rateParking(driverToken, parkingId, 5);

        mockMvc.perform(post("/api/parking-spots/" + parkingId + "/rate")
                        .header("Authorization", bearer(driverToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rating": 3
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating", closeTo(3.0, 0.001)))
                .andExpect(jsonPath("$.ratingCount").value(1));
    }

    @Test
    void AT73_multipleDriversRatingsUpdateAverage() throws Exception {
        String ownerToken = registerAndGetToken(uniqueEmail("owner"), "OWNER");
        String firstDriverToken = registerAndGetToken(uniqueEmail("driver1"), "DRIVER");
        String secondDriverToken = registerAndGetToken(uniqueEmail("driver2"), "DRIVER");

        long parkingId = createParking(ownerToken, "AT73 Average Rating Spot");

        createApprovedBooking(
                ownerToken,
                firstDriverToken,
                parkingId,
                "2035-06-01T10:00:00",
                "2035-06-01T11:00:00"
        );

        createApprovedBooking(
                ownerToken,
                secondDriverToken,
                parkingId,
                "2035-06-01T12:00:00",
                "2035-06-01T13:00:00"
        );

        rateParking(firstDriverToken, parkingId, 5);

        mockMvc.perform(post("/api/parking-spots/" + parkingId + "/rate")
                        .header("Authorization", bearer(secondDriverToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rating": 3
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating", closeTo(4.0, 0.001)))
                .andExpect(jsonPath("$.ratingCount").value(2));
    }

    @Test
    void AT74_driverCannotRateParkingBeforeBookingIsApproved() throws Exception {
        String ownerToken = registerAndGetToken(uniqueEmail("owner"), "OWNER");
        String driverToken = registerAndGetToken(uniqueEmail("driver"), "DRIVER");

        long parkingId = createParking(ownerToken, "AT74 Pending Booking Rating Spot");

        createBooking(
                driverToken,
                parkingId,
                "2035-06-01T13:00:00",
                "2035-06-01T14:00:00"
        );

        mockMvc.perform(post("/api/parking-spots/" + parkingId + "/rate")
                        .header("Authorization", bearer(driverToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rating": 5
                                }
                                """))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void AT75_userCannotRateParkingWithoutApprovedBooking() throws Exception {
        String ownerToken = registerAndGetToken(uniqueEmail("owner"), "OWNER");
        String unrelatedDriverToken = registerAndGetToken(uniqueEmail("driver"), "DRIVER");

        long parkingId = createParking(ownerToken, "AT75 No Booking Rating Spot");

        mockMvc.perform(post("/api/parking-spots/" + parkingId + "/rate")
                        .header("Authorization", bearer(unrelatedDriverToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rating": 4
                                }
                                """))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void AT76_invalidParkingRatingValuesAreRejected() throws Exception {
        String ownerToken = registerAndGetToken(uniqueEmail("owner"), "OWNER");
        String driverToken = registerAndGetToken(uniqueEmail("driver"), "DRIVER");

        long parkingId = createParking(ownerToken, "AT76 Invalid Rating Spot");

        createApprovedBooking(
                ownerToken,
                driverToken,
                parkingId,
                "2035-06-01T14:00:00",
                "2035-06-01T15:00:00"
        );

        mockMvc.perform(post("/api/parking-spots/" + parkingId + "/rate")
                        .header("Authorization", bearer(driverToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rating": 0
                                }
                                """))
                .andExpect(status().is4xxClientError());

        mockMvc.perform(post("/api/parking-spots/" + parkingId + "/rate")
                        .header("Authorization", bearer(driverToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rating": 6
                                }
                                """))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void AT77_ratingNonExistingParkingIsRejected() throws Exception {
        String driverToken = registerAndGetToken(uniqueEmail("driver"), "DRIVER");

        mockMvc.perform(post("/api/parking-spots/999999999/rate")
                        .header("Authorization", bearer(driverToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rating": 5
                                }
                                """))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void AT78_ownerReceivesNotificationAfterParkingRating() throws Exception {
        String ownerToken = registerAndGetToken(uniqueEmail("owner"), "OWNER");
        String driverToken = registerAndGetToken(uniqueEmail("driver"), "DRIVER");

        long parkingId = createParking(ownerToken, "AT78 Rating Notification Spot");

        createApprovedBooking(
                ownerToken,
                driverToken,
                parkingId,
                "2035-06-01T15:00:00",
                "2035-06-01T16:00:00"
        );

        rateParking(driverToken, parkingId, 5);

        String notifications = listNotifications(ownerToken);

        assertTrue(
                responseArrayContainsTitle(notifications, "Rating"),
                "Owner should receive a notification after a driver rates their parking spot"
        );
    }

    @Test
    void AT79_unauthenticatedUserCannotRateParking() throws Exception {
        mockMvc.perform(post("/api/parking-spots/1/rate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rating": 5
                                }
                                """))
                .andExpect(status().is4xxClientError());
    }
}