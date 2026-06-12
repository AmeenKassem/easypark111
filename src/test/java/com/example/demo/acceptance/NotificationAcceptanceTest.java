package com.example.demo.acceptance;

import com.example.demo.service.EmailService;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationAcceptanceTest extends AcceptanceTestBase {

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
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response);
    }

    private JsonNode updateBookingStatus(String ownerToken, long bookingId, String status) throws Exception {
        String body = """
                {
                  "status": "%s"
                }
                """.formatted(status);

        String response = mockMvc.perform(put("/api/bookings/" + bookingId + "/status")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value((int) bookingId))
                .andExpect(jsonPath("$.status").value(status))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response);
    }

    private JsonNode cancelBooking(String driverToken, long bookingId) throws Exception {
        String response = mockMvc.perform(put("/api/bookings/" + bookingId + "/cancel")
                        .header("Authorization", bearer(driverToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value((int) bookingId))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
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

    private boolean responseArrayContainsMessagePart(String responseBody, String expectedPart) throws Exception {
        JsonNode array = objectMapper.readTree(responseBody);

        for (JsonNode item : array) {
            if (item.has("message") && item.get("message").asText().contains(expectedPart)) {
                return true;
            }
        }

        return false;
    }

    private boolean allNotificationsAreRead(String responseBody) throws Exception {
        JsonNode array = objectMapper.readTree(responseBody);

        for (JsonNode item : array) {
            if (item.has("read") && !item.get("read").asBoolean()) {
                return false;
            }
        }

        return true;
    }

    @Test
    void AT41_ownerReceivesNotificationAfterBookingRequest() throws Exception {
        String ownerToken = registerAndGetToken(uniqueEmail("owner"), "OWNER");
        String driverToken = registerAndGetToken(uniqueEmail("driver"), "DRIVER");

        long parkingId = createParking(ownerToken, "AT41 Owner Notification Spot");

        createBooking(
                driverToken,
                parkingId,
                "2035-06-01T08:00:00",
                "2035-06-01T09:00:00"
        );

        String notifications = listNotifications(ownerToken);

        assertTrue(
                responseArrayContainsTitle(notifications, "New booking request"),
                "Owner should receive a notification after a driver creates a booking request"
        );
        assertTrue(
                responseArrayContainsMessagePart(notifications, "AT41 Owner Notification Spot"),
                "Notification message should mention the booked parking spot"
        );
    }

    @Test
    void AT42_driverReceivesNotificationAfterBookingApproval() throws Exception {
        String ownerToken = registerAndGetToken(uniqueEmail("owner"), "OWNER");
        String driverToken = registerAndGetToken(uniqueEmail("driver"), "DRIVER");

        long parkingId = createParking(ownerToken, "AT42 Approval Notification Spot");

        long bookingId = createBooking(
                driverToken,
                parkingId,
                "2035-06-01T09:00:00",
                "2035-06-01T10:00:00"
        ).get("id").asLong();

        updateBookingStatus(ownerToken, bookingId, "APPROVED");

        String notifications = listNotifications(driverToken);

        assertTrue(
                responseArrayContainsTitle(notifications, "Booking Approved"),
                "Driver should receive a notification when owner approves booking"
        );
        assertTrue(
                responseArrayContainsMessagePart(notifications, "AT42 Approval Notification Spot"),
                "Approval notification should mention the parking spot"
        );
    }

    @Test
    void AT43_driverReceivesNotificationAfterBookingRejection() throws Exception {
        String ownerToken = registerAndGetToken(uniqueEmail("owner"), "OWNER");
        String driverToken = registerAndGetToken(uniqueEmail("driver"), "DRIVER");

        long parkingId = createParking(ownerToken, "AT43 Rejection Notification Spot");

        long bookingId = createBooking(
                driverToken,
                parkingId,
                "2035-06-01T10:00:00",
                "2035-06-01T11:00:00"
        ).get("id").asLong();

        updateBookingStatus(ownerToken, bookingId, "REJECTED");

        String notifications = listNotifications(driverToken);

        assertTrue(
                responseArrayContainsTitle(notifications, "Booking Rejected"),
                "Driver should receive a notification when owner rejects booking"
        );
        assertTrue(
                responseArrayContainsMessagePart(notifications, "AT43 Rejection Notification Spot"),
                "Rejection notification should mention the parking spot"
        );
    }

    @Test
    void AT44_ownerReceivesNotificationAfterBookingCancellation() throws Exception {
        String ownerToken = registerAndGetToken(uniqueEmail("owner"), "OWNER");
        String driverToken = registerAndGetToken(uniqueEmail("driver"), "DRIVER");

        long parkingId = createParking(ownerToken, "AT44 Cancellation Notification Spot");

        long bookingId = createBooking(
                driverToken,
                parkingId,
                "2035-06-01T11:00:00",
                "2035-06-01T12:00:00"
        ).get("id").asLong();

        cancelBooking(driverToken, bookingId);

        String notifications = listNotifications(ownerToken);

        assertTrue(
                responseArrayContainsTitle(notifications, "Booking Cancelled"),
                "Owner should receive a notification when driver cancels booking"
        );
        assertTrue(
                responseArrayContainsMessagePart(notifications, "AT44 Cancellation Notification Spot"),
                "Cancellation notification should mention the parking spot"
        );
    }

    @Test
    void AT45_userCanViewUnreadNotificationCount() throws Exception {
        String ownerToken = registerAndGetToken(uniqueEmail("owner"), "OWNER");
        String driverToken = registerAndGetToken(uniqueEmail("driver"), "DRIVER");

        long parkingId = createParking(ownerToken, "AT45 Unread Count Spot");

        createBooking(
                driverToken,
                parkingId,
                "2035-06-01T12:00:00",
                "2035-06-01T13:00:00"
        );

        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count", greaterThanOrEqualTo(1)));
    }

    @Test
    void AT46_userCanMarkAllNotificationsAsRead() throws Exception {
        String ownerToken = registerAndGetToken(uniqueEmail("owner"), "OWNER");
        String driverToken = registerAndGetToken(uniqueEmail("driver"), "DRIVER");

        long parkingId = createParking(ownerToken, "AT46 Mark Read Spot");

        createBooking(
                driverToken,
                parkingId,
                "2035-06-01T13:00:00",
                "2035-06-01T14:00:00"
        );

        mockMvc.perform(put("/api/notifications/read-all")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));

        String notifications = listNotifications(ownerToken);

        assertTrue(
                allNotificationsAreRead(notifications),
                "All returned notifications should be marked as read"
        );
    }

    @Test
    void AT47_userCanClearOwnNotifications() throws Exception {
        String ownerToken = registerAndGetToken(uniqueEmail("owner"), "OWNER");
        String driverToken = registerAndGetToken(uniqueEmail("driver"), "DRIVER");

        long parkingId = createParking(ownerToken, "AT47 Clear Notifications Spot");

        createBooking(
                driverToken,
                parkingId,
                "2035-06-01T14:00:00",
                "2035-06-01T15:00:00"
        );

        mockMvc.perform(delete("/api/notifications")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk());

        String notifications = listNotifications(ownerToken);

        JsonNode array = objectMapper.readTree(notifications);
        assertTrue(array.isArray());
        assertEquals(0, array.size());
    }

    @Test
    void AT48_notificationsAreScopedToAuthenticatedUser() throws Exception {
        String realOwnerToken = registerAndGetToken(uniqueEmail("owner1"), "OWNER");
        String otherOwnerToken = registerAndGetToken(uniqueEmail("owner2"), "OWNER");
        String driverToken = registerAndGetToken(uniqueEmail("driver"), "DRIVER");

        long parkingId = createParking(realOwnerToken, "AT48 Scoped Notification Spot");

        createBooking(
                driverToken,
                parkingId,
                "2035-06-01T15:00:00",
                "2035-06-01T16:00:00"
        );

        String realOwnerNotifications = listNotifications(realOwnerToken);
        String otherOwnerNotifications = listNotifications(otherOwnerToken);

        assertTrue(
                responseArrayContainsTitle(realOwnerNotifications, "New booking request"),
                "Real owner should see their booking notification"
        );

        assertFalse(
                responseArrayContainsMessagePart(otherOwnerNotifications, "AT48 Scoped Notification Spot"),
                "Another user should not see notifications belonging to the real owner"
        );
    }

    @Test
    void AT49_unauthenticatedUserCannotAccessNotifications() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().is4xxClientError());

        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().is4xxClientError());

        mockMvc.perform(put("/api/notifications/read-all"))
                .andExpect(status().is4xxClientError());

        mockMvc.perform(delete("/api/notifications"))
                .andExpect(status().is4xxClientError());
    }
}