package com.example.demo.acceptance;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OwnerParkingAcceptanceTest extends AcceptanceTestBase {

    @Test
    void AT04_ownerCanCreateParkingSpot() throws Exception {
        String ownerToken = registerAndGetToken(uniqueEmail("owner"), "OWNER");

        String body = """
                {
                  "location": "Ben Gurion University, Beer Sheva",
                  "lat": 31.2622,
                  "lng": 34.8015,
                  "pricePerHour": 12.5,
                  "covered": true,
                  "availableFrom": "2026-06-01T08:00:00",
                  "availableTo": "2026-06-01T18:00:00"
                }
                """;

        mockMvc.perform(post("/api/parking-spots")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.location").value("Ben Gurion University, Beer Sheva"))
                .andExpect(jsonPath("$.pricePerHour").value(12.5))
                .andExpect(jsonPath("$.covered").value(true))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void AT05_ownerCanViewHisOwnParkingSpots() throws Exception {
        String ownerToken = registerAndGetToken(uniqueEmail("owner"), "OWNER");

        String body = """
                {
                  "location": "Owner Private Spot",
                  "lat": 31.25,
                  "lng": 34.79,
                  "pricePerHour": 10,
                  "covered": false
                }
                """;

        mockMvc.perform(post("/api/parking-spots")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/parking-spots/my")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].location").value("Owner Private Spot"));
    }

    @Test
    void AT06_ownerCanUpdateParkingSpotAvailabilityAndPrice() throws Exception {
        String ownerToken = registerAndGetToken(uniqueEmail("owner"), "OWNER");

        String createBody = """
                {
                  "location": "Spot Before Update",
                  "lat": 31.25,
                  "lng": 34.79,
                  "pricePerHour": 10,
                  "covered": false
                }
                """;

        String createResponse = mockMvc.perform(post("/api/parking-spots")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long parkingId = objectMapper.readTree(createResponse).get("id").asLong();

        String updateBody = """
                {
                  "location": "Spot After Update",
                  "lat": 31.25,
                  "lng": 34.79,
                  "pricePerHour": 20,
                  "covered": true,
                  "availableFrom": "2026-06-01T09:00:00",
                  "availableTo": "2026-06-01T17:00:00",
                  "active": true
                }
                """;

        mockMvc.perform(put("/api/parking-spots/" + parkingId)
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.location").value("Spot After Update"))
                .andExpect(jsonPath("$.pricePerHour").value(20))
                .andExpect(jsonPath("$.covered").value(true));
    }

    @Test
    void AT07_ownerCanDeactivateParkingSpotSoItIsNotActive() throws Exception {
        String ownerToken = registerAndGetToken(uniqueEmail("owner"), "OWNER");

        String createBody = """
                {
                  "location": "Spot To Deactivate",
                  "lat": 31.25,
                  "lng": 34.79,
                  "pricePerHour": 10,
                  "covered": false
                }
                """;

        String createResponse = mockMvc.perform(post("/api/parking-spots")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long parkingId = objectMapper.readTree(createResponse).get("id").asLong();

        String updateBody = """
                {
                  "location": "Spot To Deactivate",
                  "lat": 31.25,
                  "lng": 34.79,
                  "pricePerHour": 10,
                  "covered": false,
                  "active": false
                }
                """;

        mockMvc.perform(put("/api/parking-spots/" + parkingId)
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }
}