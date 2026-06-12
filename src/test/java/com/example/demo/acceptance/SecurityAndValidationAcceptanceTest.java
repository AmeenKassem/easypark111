package com.example.demo.acceptance;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAndValidationAcceptanceTest extends AcceptanceTestBase {

    @Test
    void AT09_registeredUserCanCreateParkingSpotBecauseUsersAreBothDriverAndOwner() throws Exception {
        String userToken = registerAndGetToken(uniqueEmail("user"), "DRIVER");

        String body = """
            {
              "location": "User Spot",
              "lat": 31.25,
              "lng": 34.79,
              "pricePerHour": 10,
              "covered": false
            }
            """;

        mockMvc.perform(post("/api/parking-spots")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.location").value("User Spot"));
    }
    @Test
    void AT11_userCannotUpdateParkingSpotOwnedByAnotherUser() throws Exception {
        String firstUserToken = registerAndGetToken(uniqueEmail("owner1"), "DRIVER");
        String secondUserToken = registerAndGetToken(uniqueEmail("owner2"), "DRIVER");

        String createBody = """
            {
              "location": "First User Spot",
              "lat": 31.25,
              "lng": 34.79,
              "pricePerHour": 10,
              "covered": false
            }
            """;

        String createResponse = mockMvc.perform(post("/api/parking-spots")
                        .header("Authorization", bearer(firstUserToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long parkingId = objectMapper.readTree(createResponse).get("id").asLong();

        String updateBody = """
            {
              "location": "Illegally Updated Spot",
              "lat": 31.25,
              "lng": 34.79,
              "pricePerHour": 20,
              "covered": true,
              "active": true
            }
            """;

        mockMvc.perform(put("/api/parking-spots/" + parkingId)
                        .header("Authorization", bearer(secondUserToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void AT10_unauthenticatedUserCannotAccessOwnerSpots() throws Exception {
        mockMvc.perform(get("/api/parking-spots/my"))
                .andExpect(status().isForbidden());
    }

    @Test
    void AT11_invalidParkingPriceIsRejected() throws Exception {
        String ownerToken = registerAndGetToken(uniqueEmail("owner"), "OWNER");

        String body = """
                {
                  "location": "Invalid Price Spot",
                  "lat": 31.25,
                  "lng": 34.79,
                  "pricePerHour": -5,
                  "covered": false
                }
                """;

        mockMvc.perform(post("/api/parking-spots")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void AT12_missingParkingLocationIsRejected() throws Exception {
        String ownerToken = registerAndGetToken(uniqueEmail("owner"), "OWNER");

        String body = """
                {
                  "pricePerHour": 10,
                  "covered": false
                }
                """;

        mockMvc.perform(post("/api/parking-spots")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}