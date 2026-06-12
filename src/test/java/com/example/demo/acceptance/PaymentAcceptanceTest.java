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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentAcceptanceTest extends AcceptanceTestBase {

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

    private JsonNode createApprovedBooking(
            String ownerToken,
            String driverToken,
            String location,
            String startTime,
            String endTime
    ) throws Exception {
        long parkingId = createParking(ownerToken, location);
        JsonNode booking = createBooking(driverToken, parkingId, startTime, endTime);
        long bookingId = booking.get("id").asLong();
        return approveBooking(ownerToken, bookingId);
    }

    private JsonNode createPayment(String driverToken, long bookingId) throws Exception {
        String body = """
                {
                  "bookingId": %d
                }
                """.formatted(bookingId);

        String response = mockMvc.perform(post("/api/payments")
                        .header("Authorization", bearer(driverToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.bookingId").value((int) bookingId))
                .andExpect(jsonPath("$.amount", notNullValue()))
                .andExpect(jsonPath("$.currency").value("ILS"))
                .andExpect(jsonPath("$.provider").value("BIT"))
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.paidAt", notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response);
    }

    private boolean responseArrayContainsPaymentId(String responseBody, long paymentId) throws Exception {
        JsonNode array = objectMapper.readTree(responseBody);

        for (JsonNode item : array) {
            if (item.has("id") && item.get("id").asLong() == paymentId) {
                return true;
            }
        }

        return false;
    }

    @Test
    void AT32_driverCanPayForApprovedBooking() throws Exception {
        String ownerToken = registerAndGetToken(uniqueEmail("owner"), "OWNER");
        String driverToken = registerAndGetToken(uniqueEmail("driver"), "DRIVER");

        JsonNode approvedBooking = createApprovedBooking(
                ownerToken,
                driverToken,
                "AT32 Payment Happy Path Spot",
                "2035-06-01T08:00:00",
                "2035-06-01T09:00:00"
        );

        long bookingId = approvedBooking.get("id").asLong();

        JsonNode payment = createPayment(driverToken, bookingId);

        assertTrue(payment.get("id").asLong() > 0);
        assertTrue(payment.get("amount").asDouble() > 0);
        assertEquals("ILS", payment.get("currency").asText());
        assertEquals("BIT", payment.get("provider").asText());
        assertEquals("PAID", payment.get("status").asText());
    }

    @Test
    void AT33_paymentAmountMatchesBookingTotalPrice() throws Exception {
        String ownerToken = registerAndGetToken(uniqueEmail("owner"), "OWNER");
        String driverToken = registerAndGetToken(uniqueEmail("driver"), "DRIVER");

        JsonNode approvedBooking = createApprovedBooking(
                ownerToken,
                driverToken,
                "AT33 Payment Amount Spot",
                "2035-06-01T09:00:00",
                "2035-06-01T11:00:00"
        );

        long bookingId = approvedBooking.get("id").asLong();
        double bookingTotalPrice = approvedBooking.get("totalPrice").asDouble();

        JsonNode payment = createPayment(driverToken, bookingId);

        double paymentAmount = payment.get("amount").asDouble();

        assertEquals(bookingTotalPrice, paymentAmount, 0.001);
    }

    @Test
    void AT34_driverCanViewOwnPayments() throws Exception {
        String ownerToken = registerAndGetToken(uniqueEmail("owner"), "OWNER");
        String driverToken = registerAndGetToken(uniqueEmail("driver"), "DRIVER");

        JsonNode approvedBooking = createApprovedBooking(
                ownerToken,
                driverToken,
                "AT34 Driver Payment History Spot",
                "2035-06-01T11:00:00",
                "2035-06-01T12:00:00"
        );

        long paymentId = createPayment(driverToken, approvedBooking.get("id").asLong())
                .get("id")
                .asLong();

        String response = mockMvc.perform(get("/api/payments/my")
                        .header("Authorization", bearer(driverToken)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertTrue(
                responseArrayContainsPaymentId(response, paymentId),
                "Driver payment history should contain the created payment"
        );
    }

    @Test
    void AT35_ownerCanViewPaymentsForOwnedParkingSpots() throws Exception {
        String ownerToken = registerAndGetToken(uniqueEmail("owner"), "OWNER");
        String driverToken = registerAndGetToken(uniqueEmail("driver"), "DRIVER");

        JsonNode approvedBooking = createApprovedBooking(
                ownerToken,
                driverToken,
                "AT35 Owner Payment History Spot",
                "2035-06-01T12:00:00",
                "2035-06-01T13:00:00"
        );

        long paymentId = createPayment(driverToken, approvedBooking.get("id").asLong())
                .get("id")
                .asLong();

        String response = mockMvc.perform(get("/api/payments/owner")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertTrue(
                responseArrayContainsPaymentId(response, paymentId),
                "Owner payment list should contain payments for owned parking spots"
        );
    }

    @Test
    void AT36_paymentForPendingBookingIsRejected() throws Exception {
        String ownerToken = registerAndGetToken(uniqueEmail("owner"), "OWNER");
        String driverToken = registerAndGetToken(uniqueEmail("driver"), "DRIVER");

        long parkingId = createParking(ownerToken, "AT36 Pending Payment Rejected Spot");

        JsonNode pendingBooking = createBooking(
                driverToken,
                parkingId,
                "2035-06-01T13:00:00",
                "2035-06-01T14:00:00"
        );

        long bookingId = pendingBooking.get("id").asLong();

        String body = """
                {
                  "bookingId": %d
                }
                """.formatted(bookingId);

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", bearer(driverToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void AT37_paymentForNonExistingBookingIsRejected() throws Exception {
        String driverToken = registerAndGetToken(uniqueEmail("driver"), "DRIVER");

        String body = """
                {
                  "bookingId": 999999999
                }
                """;

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", bearer(driverToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void AT38_duplicatePaymentForSameBookingIsRejected() throws Exception {
        String ownerToken = registerAndGetToken(uniqueEmail("owner"), "OWNER");
        String driverToken = registerAndGetToken(uniqueEmail("driver"), "DRIVER");

        JsonNode approvedBooking = createApprovedBooking(
                ownerToken,
                driverToken,
                "AT38 Duplicate Payment Spot",
                "2035-06-01T14:00:00",
                "2035-06-01T15:00:00"
        );

        long bookingId = approvedBooking.get("id").asLong();

        createPayment(driverToken, bookingId);

        String body = """
                {
                  "bookingId": %d
                }
                """.formatted(bookingId);

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", bearer(driverToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void AT39_userCannotPayForAnotherUsersBooking() throws Exception {
        String ownerToken = registerAndGetToken(uniqueEmail("owner"), "OWNER");
        String realDriverToken = registerAndGetToken(uniqueEmail("driver1"), "DRIVER");
        String otherDriverToken = registerAndGetToken(uniqueEmail("driver2"), "DRIVER");

        JsonNode approvedBooking = createApprovedBooking(
                ownerToken,
                realDriverToken,
                "AT39 Other User Payment Spot",
                "2035-06-01T15:00:00",
                "2035-06-01T16:00:00"
        );

        long bookingId = approvedBooking.get("id").asLong();

        String body = """
                {
                  "bookingId": %d
                }
                """.formatted(bookingId);

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", bearer(otherDriverToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void AT40_paymentRequestWithoutBookingIdIsRejected() throws Exception {
        String driverToken = registerAndGetToken(uniqueEmail("driver"), "DRIVER");

        String body = """
                {}
                """;

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", bearer(driverToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is4xxClientError());
    }
}