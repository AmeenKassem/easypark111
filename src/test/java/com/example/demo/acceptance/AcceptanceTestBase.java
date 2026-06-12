package com.example.demo.acceptance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public abstract class AcceptanceTestBase {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected String registerAndGetToken(String email, String role) throws Exception {
        String body = """
                {
                  "fullName": "Acceptance Test User",
                  "email": "%s",
                  "phone": "0500000000",
                  "password": "Password123!",
                  "role": "%s"
                }
                """.formatted(email, role);

        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        return json.get("token").asText();
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }

    protected String uniqueEmail(String prefix) {
        return prefix + System.currentTimeMillis() + "@easypark.test";
    }
}