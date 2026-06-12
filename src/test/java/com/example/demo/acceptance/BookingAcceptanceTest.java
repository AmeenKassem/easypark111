package com.example.demo.acceptance;

import com.example.demo.service.EmailService;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BookingAcceptanceTest extends AcceptanceTestBase {

    /*
     * We mock EmailService because approving a booking triggers an email.
     * Acceptance tests should verify the booking behavior, not depend on real SMTP/external email.
     */
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
                .andExpect(jsonPath("$.totalPrice", notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response);
    }

    private boolean responseArrayContainsBookingId(String responseBody, long bookingId) throws Exception {
        JsonNode array = objectMapper.readTree(responseBody);

        for (JsonNode item : array) {
            if (item.has("id") && item.get("id").asLong() == bookingId) {
                return true;
            }
        }

        return false;
    }

    @Test
    void AT20_userCanCreateBookingForAvailableParking() throws Exception {
        String ownerToken = registerAndGetToken(uniqueEmail("owner"), "OWNER");
        String driverToken = registerAndGetToken(uniqueEmail("driver"), "DRIVER");

        long parkingId = createParking(ownerToken, "AT20 Booking Happy Path Spot");

        JsonNode booking = createBooking(
                driverToken,
                parkingId,
                "2035-06-01T10:00:00",
                "2035-06-01T12:00:00"
        );

        assertTrue(booking.get("id").asLong() > 0);
        assertTrue(booking.get("totalPrice").asDouble() > 0);
    }

    @Test
    void AT21_driverCanViewOwnBookings() throws Exception {
        String ownerToken = registerAndGetToken(uniqueEmail("owner"), "OWNER");
        String driverToken = registerAndGetToken(uniqueEmail("driver"), "DRIVER");

        long parkingId = createParking(ownerToken, "AT21 Driver History Spot");

        long bookingId = createBooking(
                driverToken,
                parkingId,
                "2035-06-01T12:00:00",
                "2035-06-01T13:00:00"
        ).get("id").asLong();

        String response = mockMvc.perform(get("/api/bookings/my")
                        .header("Authorization", bearer(driverToken)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertTrue(
                responseArrayContainsBookingId(response, bookingId),
                "Driver booking history should contain the created booking"
        );
    }

    @Test
    void AT22_ownerCanViewBookingsForOwnedParking() throws Exception {
        String ownerToken = registerAndGetToken(uniqueEmail("owner"), "OWNER");
        String driverToken = registerAndGetToken(uniqueEmail("driver"), "DRIVER");

        long parkingId = createParking(ownerToken, "AT22 Owner Booking History Spot");

        long bookingId = createBooking(
                driverToken,
                parkingId,
                "2035-06-01T13:00:00",
                "2035-06-01T14:00:00"
        ).get("id").asLong();

        String response = mockMvc.perform(get("/api/bookings/owner")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertTrue(
                responseArrayContainsBookingId(response, bookingId),
                "Owner booking list should contain bookings for owned parking spots"
        );
    }

    @Test
    void AT23_ownerCanApproveBookingRequest() throws Exception {
        String ownerToken = registerAndGetToken(uniqueEmail("owner"), "OWNER");
        String driverToken = registerAndGetToken(uniqueEmail("driver"), "DRIVER");

        long parkingId = createParking(ownerToken, "AT23 Approve Booking Spot");

        long bookingId = createBooking(
                driverToken,
                parkingId,
                "2035-06-01T14:00:00",
                "2035-06-01T15:00:00"
        ).get("id").asLong();

        String body = """
                {
                  "status": "APPROVED"
                }
                """;

        mockMvc.perform(put("/api/bookings/" + bookingId + "/status")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value((int) bookingId))
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void AT24_ownerCanRejectBookingRequest() throws Exception {
        String ownerToken = registerAndGetToken(uniqueEmail("owner"), "OWNER");
        String driverToken = registerAndGetToken(uniqueEmail("driver"), "DRIVER");

        long parkingId = createParking(ownerToken, "AT24 Reject Booking Spot");

        long bookingId = createBooking(
                driverToken,
                parkingId,
                "2035-06-01T15:00:00",
                "2035-06-01T16:00:00"
        ).get("id").asLong();

        String body = """
                {
                  "status": "REJECTED"
                }
                """;

        mockMvc.perform(put("/api/bookings/" + bookingId + "/status")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value((int) bookingId))
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void AT25_driverCanCancelOwnBooking() throws Exception {
        String ownerToken = registerAndGetToken(uniqueEmail("owner"), "OWNER");
        String driverToken = registerAndGetToken(uniqueEmail("driver"), "DRIVER");

        long parkingId = createParking(ownerToken, "AT25 Cancel Booking Spot");

        long bookingId = createBooking(
                driverToken,
                parkingId,
                "2035-06-01T16:00:00",
                "2035-06-01T17:00:00"
        ).get("id").asLong();

        mockMvc.perform(put("/api/bookings/" + bookingId + "/cancel")
                        .header("Authorization", bearer(driverToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value((int) bookingId))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void AT26_overlappingBookingForSameParkingIsRejected() throws Exception {
        String ownerToken = registerAndGetToken(uniqueEmail("owner"), "OWNER");
        String firstDriverToken = registerAndGetToken(uniqueEmail("driver1"), "DRIVER");
        String secondDriverToken = registerAndGetToken(uniqueEmail("driver2"), "DRIVER");

        long parkingId = createParking(ownerToken, "AT26 Overlap Spot");

        createBooking(
                firstDriverToken,
                parkingId,
                "2035-06-01T17:00:00",
                "2035-06-01T18:00:00"
        );

        String overlappingBody = """
                {
                  "parkingId": %d,
                  "startTime": "2035-06-01T17:30:00",
                  "endTime": "2035-06-01T18:30:00"
                }
                """.formatted(parkingId);

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", bearer(secondDriverToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(overlappingBody))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void AT27_bookingWithInvalidTimeRangeIsRejected() throws Exception {
        String ownerToken = registerAndGetToken(uniqueEmail("owner"), "OWNER");
        String driverToken = registerAndGetToken(uniqueEmail("driver"), "DRIVER");

        long parkingId = createParking(ownerToken, "AT27 Invalid Time Spot");

        String body = """
                {
                  "parkingId": %d,
                  "startTime": "2035-06-01T19:00:00",
                  "endTime": "2035-06-01T18:00:00"
                }
                """.formatted(parkingId);

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", bearer(driverToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void AT28_bookingNonExistingParkingIsRejected() throws Exception {
        String driverToken = registerAndGetToken(uniqueEmail("driver"), "DRIVER");

        String body = """
                {
                  "parkingId": 999999999,
                  "startTime": "2035-06-01T10:00:00",
                  "endTime": "2035-06-01T11:00:00"
                }
                """;

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", bearer(driverToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void AT29_driverCannotCancelAnotherDriversBooking() throws Exception {
        String ownerToken = registerAndGetToken(uniqueEmail("owner"), "OWNER");
        String firstDriverToken = registerAndGetToken(uniqueEmail("driver1"), "DRIVER");
        String secondDriverToken = registerAndGetToken(uniqueEmail("driver2"), "DRIVER");

        long parkingId = createParking(ownerToken, "AT29 Cancel Other User Booking Spot");

        long bookingId = createBooking(
                firstDriverToken,
                parkingId,
                "2035-06-01T18:00:00",
                "2035-06-01T19:00:00"
        ).get("id").asLong();

        mockMvc.perform(put("/api/bookings/" + bookingId + "/cancel")
                        .header("Authorization", bearer(secondDriverToken)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void AT30_nonOwnerCannotApproveOrRejectBookingForAnotherOwnersParking() throws Exception {
        String realOwnerToken = registerAndGetToken(uniqueEmail("owner1"), "OWNER");
        String otherUserToken = registerAndGetToken(uniqueEmail("owner2"), "OWNER");
        String driverToken = registerAndGetToken(uniqueEmail("driver"), "DRIVER");

        long parkingId = createParking(realOwnerToken, "AT30 Non Owner Status Update Spot");

        long bookingId = createBooking(
                driverToken,
                parkingId,
                "2035-06-01T19:00:00",
                "2035-06-01T20:00:00"
        ).get("id").asLong();

        String body = """
                {
                  "status": "APPROVED"
                }
                """;

        mockMvc.perform(put("/api/bookings/" + bookingId + "/status")
                        .header("Authorization", bearer(otherUserToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void AT31_ownerCanRateDriverAfterApprovedBooking() throws Exception {
        String ownerToken = registerAndGetToken(uniqueEmail("owner"), "OWNER");
        String driverToken = registerAndGetToken(uniqueEmail("driver"), "DRIVER");

        long parkingId = createParking(ownerToken, "AT31 Rate Driver Spot");

        long bookingId = createBooking(
                driverToken,
                parkingId,
                "2035-06-01T09:00:00",
                "2035-06-01T10:00:00"
        ).get("id").asLong();

        String approveBody = """
                {
                  "status": "APPROVED"
                }
                """;

        mockMvc.perform(put("/api/bookings/" + bookingId + "/status")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(approveBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        String ratingBody = """
                {
                  "score": 5
                }
                """;

        mockMvc.perform(post("/api/bookings/" + bookingId + "/rate-driver")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ratingBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Rating submitted successfully"));
    }
}