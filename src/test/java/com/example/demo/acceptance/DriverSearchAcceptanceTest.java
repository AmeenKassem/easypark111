package com.example.demo.acceptance;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DriverSearchAcceptanceTest extends AcceptanceTestBase {

    @Test
    void AT08_driverCanSearchAvailableParkingByFilters() throws Exception {
        String ownerToken = registerAndGetToken(uniqueEmail("owner"), "OWNER");
        String driverToken = registerAndGetToken(uniqueEmail("driver"), "DRIVER");

        String body = """
                {
                  "location": "Covered Search Spot",
                  "lat": 31.2622,
                  "lng": 34.8015,
                  "pricePerHour": 15,
                  "covered": true
                }
                """;

        mockMvc.perform(post("/api/parking-spots")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/parking-spots/search")
                        .header("Authorization", bearer(driverToken))
                        .param("covered", "true")
                        .param("maxPrice", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(1)));
    }
}