package com.example.demo.acceptance;

import com.example.demo.service.EmailService;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
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
class ReportAcceptanceTest extends AcceptanceTestBase {

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
                .andExpect(jsonPath("$.amount", greaterThan(0.0)))
                .andExpect(jsonPath("$.currency").value("ILS"))
                .andExpect(jsonPath("$.provider").value("BIT"))
                .andExpect(jsonPath("$.status").value("PAID"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response);
    }

    private long createApprovedAndPaidBooking(
            String ownerToken,
            String driverToken,
            String location,
            String startTime,
            String endTime
    ) throws Exception {
        long parkingId = createParking(ownerToken, location);

        long bookingId = createBooking(
                driverToken,
                parkingId,
                startTime,
                endTime
        ).get("id").asLong();

        approveBooking(ownerToken, bookingId);
        createPayment(driverToken, bookingId);

        return bookingId;
    }

    @Test
    void AT50_ownerCanViewDashboard() throws Exception {
        String ownerToken = registerAndGetToken(uniqueEmail("owner"), "OWNER");

        mockMvc.perform(get("/api/reports/owner-dashboard")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRevenue", notNullValue()))
                .andExpect(jsonPath("$.totalReservations", notNullValue()));
    }

    @Test
    void AT51_ownerDashboardIncludesRevenueFromPaidBookings() throws Exception {
        String ownerToken = registerAndGetToken(uniqueEmail("owner"), "OWNER");
        String driverToken = registerAndGetToken(uniqueEmail("driver"), "DRIVER");

        createApprovedAndPaidBooking(
                ownerToken,
                driverToken,
                "AT51 Owner Revenue Spot",
                "2035-06-01T08:00:00",
                "2035-06-01T09:00:00"
        );

        mockMvc.perform(get("/api/reports/owner-dashboard")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRevenue", greaterThan(0.0)));
    }

    @Test
    void AT52_ownerDashboardIncludesReservationCount() throws Exception {
        String ownerToken = registerAndGetToken(uniqueEmail("owner"), "OWNER");
        String driverToken = registerAndGetToken(uniqueEmail("driver"), "DRIVER");

        createApprovedAndPaidBooking(
                ownerToken,
                driverToken,
                "AT52 Owner Reservation Count Spot",
                "2035-06-01T09:00:00",
                "2035-06-01T10:00:00"
        );

        mockMvc.perform(get("/api/reports/owner-dashboard")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReservations", greaterThanOrEqualTo(1)));
    }

    @Test
    void AT53_driverCanViewReport() throws Exception {
        String driverToken = registerAndGetToken(uniqueEmail("driver"), "DRIVER");

        mockMvc.perform(get("/api/reports/driver-report")
                        .header("Authorization", bearer(driverToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpenses", notNullValue()))
                .andExpect(jsonPath("$.totalBookings", notNullValue()));
    }

    @Test
    void AT54_driverReportIncludesExpensesFromPaidBookings() throws Exception {
        String ownerToken = registerAndGetToken(uniqueEmail("owner"), "OWNER");
        String driverToken = registerAndGetToken(uniqueEmail("driver"), "DRIVER");

        createApprovedAndPaidBooking(
                ownerToken,
                driverToken,
                "AT54 Driver Expenses Spot",
                "2035-06-01T10:00:00",
                "2035-06-01T11:00:00"
        );

        mockMvc.perform(get("/api/reports/driver-report")
                        .header("Authorization", bearer(driverToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpenses", greaterThan(0.0)));
    }

    @Test
    void AT55_driverReportIncludesBookingCount() throws Exception {
        String ownerToken = registerAndGetToken(uniqueEmail("owner"), "OWNER");
        String driverToken = registerAndGetToken(uniqueEmail("driver"), "DRIVER");

        createApprovedAndPaidBooking(
                ownerToken,
                driverToken,
                "AT55 Driver Booking Count Spot",
                "2035-06-01T11:00:00",
                "2035-06-01T12:00:00"
        );

        mockMvc.perform(get("/api/reports/driver-report")
                        .header("Authorization", bearer(driverToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBookings", greaterThanOrEqualTo(1)));
    }

    @Test
    void AT56_ownerReportDataIsScopedToAuthenticatedOwner() throws Exception {
        String realOwnerToken = registerAndGetToken(uniqueEmail("owner1"), "OWNER");
        String otherOwnerToken = registerAndGetToken(uniqueEmail("owner2"), "OWNER");
        String driverToken = registerAndGetToken(uniqueEmail("driver"), "DRIVER");

        createApprovedAndPaidBooking(
                realOwnerToken,
                driverToken,
                "AT56 Owner Scoped Report Spot",
                "2035-06-01T12:00:00",
                "2035-06-01T13:00:00"
        );

        String realOwnerResponse = mockMvc.perform(get("/api/reports/owner-dashboard")
                        .header("Authorization", bearer(realOwnerToken)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String otherOwnerResponse = mockMvc.perform(get("/api/reports/owner-dashboard")
                        .header("Authorization", bearer(otherOwnerToken)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode realOwnerReport = objectMapper.readTree(realOwnerResponse);
        JsonNode otherOwnerReport = objectMapper.readTree(otherOwnerResponse);

        assertTrue(realOwnerReport.get("totalReservations").asInt() >= 1);
        assertEquals(0, otherOwnerReport.get("totalReservations").asInt());
    }

    @Test
    void AT57_driverReportDataIsScopedToAuthenticatedDriver() throws Exception {
        String ownerToken = registerAndGetToken(uniqueEmail("owner"), "OWNER");
        String realDriverToken = registerAndGetToken(uniqueEmail("driver1"), "DRIVER");
        String otherDriverToken = registerAndGetToken(uniqueEmail("driver2"), "DRIVER");

        createApprovedAndPaidBooking(
                ownerToken,
                realDriverToken,
                "AT57 Driver Scoped Report Spot",
                "2035-06-01T13:00:00",
                "2035-06-01T14:00:00"
        );

        String realDriverResponse = mockMvc.perform(get("/api/reports/driver-report")
                        .header("Authorization", bearer(realDriverToken)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String otherDriverResponse = mockMvc.perform(get("/api/reports/driver-report")
                        .header("Authorization", bearer(otherDriverToken)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode realDriverReport = objectMapper.readTree(realDriverResponse);
        JsonNode otherDriverReport = objectMapper.readTree(otherDriverResponse);

        assertTrue(realDriverReport.get("totalBookings").asInt() >= 1);
        assertEquals(0, otherDriverReport.get("totalBookings").asInt());
    }

    @Test
    void AT58_unauthenticatedUserCannotAccessReports() throws Exception {
        mockMvc.perform(get("/api/reports/owner-dashboard"))
                .andExpect(status().is4xxClientError());

        mockMvc.perform(get("/api/reports/driver-report"))
                .andExpect(status().is4xxClientError());
    }
}