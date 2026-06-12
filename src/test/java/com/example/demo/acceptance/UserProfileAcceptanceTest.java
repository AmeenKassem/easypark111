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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserProfileAcceptanceTest extends AcceptanceTestBase {

    @MockBean
    private EmailService emailService;

    private String loginAndGetToken(String email, String password) throws Exception {
        String body = """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("token").asText();
    }
    private String shortEmail(String prefix) {
        long suffix = Math.abs(System.nanoTime() % 1_000_000_000L);
        return prefix + suffix + "@e.t";
    }
    @Test
    void AT59_userCanViewOwnProfile() throws Exception {
        String email = uniqueEmail("profile");
        String token = registerAndGetToken(email, "DRIVER");

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.fullName").value("Acceptance Test User"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.phone").value("0500000000"))
                .andExpect(jsonPath("$.role").value("BOTH"));
    }

    @Test
    void AT60_userCanUpdateOwnProfile() throws Exception {
        String oldEmail = shortEmail("old");
        String newEmail = shortEmail("new");
        String token = registerAndGetToken(oldEmail, "DRIVER");

        String body = """
                {
                  "fullName": "Updated Acceptance User",
                  "email": "%s",
                  "phone": "0522222222"
                }
                """.formatted(newEmail);

        String response = mockMvc.perform(put("/api/users/me")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Profile updated successfully"))
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.user.fullName").value("Updated Acceptance User"))
                .andExpect(jsonPath("$.user.email").value(newEmail))
                .andExpect(jsonPath("$.user.phone").value("0522222222"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String refreshedToken = objectMapper.readTree(response).get("token").asText();

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", bearer(refreshedToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Updated Acceptance User"))
                .andExpect(jsonPath("$.email").value(newEmail))
                .andExpect(jsonPath("$.phone").value("0522222222"));
    }

    @Test
    void AT61_profileUpdateWithEmptyFullNameIsRejected() throws Exception {
        String token = registerAndGetToken(uniqueEmail("profile"), "DRIVER");

        String body = """
                {
                  "fullName": "   "
                }
                """;

        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void AT62_profileUpdateWithInvalidEmailIsRejected() throws Exception {
        String token = registerAndGetToken(uniqueEmail("profile"), "DRIVER");

        String body = """
                {
                  "email": "not-an-email"
                }
                """;

        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void AT63_profileUpdateToExistingEmailIsRejected() throws Exception {
        String firstEmail = shortEmail("f");
        String secondEmail = shortEmail("s");

        String firstToken = registerAndGetToken(firstEmail, "DRIVER");
        registerAndGetToken(secondEmail, "DRIVER");

        String body = """
                {
                  "email": "%s"
                }
                """.formatted(secondEmail);

        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", bearer(firstToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void AT64_userCanChangePasswordAndLoginWithNewPassword() throws Exception {
        String email = uniqueEmail("password");
        String token = registerAndGetToken(email, "DRIVER");

        String body = """
                {
                  "currentPassword": "Password123!",
                  "newPassword": "NewPassword123!"
                }
                """;

        String response = mockMvc.perform(put("/api/users/me/password")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password updated successfully"))
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.user.email").value(email))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String refreshedToken = objectMapper.readTree(response).get("token").asText();
        assertTrue(refreshedToken.length() > 20);

        loginAndGetToken(email, "NewPassword123!");
    }

    @Test
    void AT65_changePasswordWithWrongCurrentPasswordIsRejected() throws Exception {
        String token = registerAndGetToken(uniqueEmail("password"), "DRIVER");

        String body = """
                {
                  "currentPassword": "WrongPassword123!",
                  "newPassword": "NewPassword123!"
                }
                """;

        mockMvc.perform(put("/api/users/me/password")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void AT66_changePasswordWithTooShortNewPasswordIsRejected() throws Exception {
        String token = registerAndGetToken(uniqueEmail("password"), "DRIVER");

        String body = """
                {
                  "currentPassword": "Password123!",
                  "newPassword": "short"
                }
                """;

        mockMvc.perform(put("/api/users/me/password")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void AT67_userCanUpdateOwnRoleAndReceiveRefreshedToken() throws Exception {
        String token = registerAndGetToken(uniqueEmail("role"), "DRIVER");

        String body = """
                {
                  "role": "OWNER"
                }
                """;

        String response = mockMvc.perform(put("/api/users/me/role")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Role updated successfully"))
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.user.role").value("OWNER"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        String refreshedToken = json.get("token").asText();

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", bearer(refreshedToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("OWNER"));
    }

    @Test
    void AT68_updateRoleWithInvalidRoleIsRejected() throws Exception {
        String token = registerAndGetToken(uniqueEmail("role"), "DRIVER");

        String body = """
                {
                  "role": "ADMIN"
                }
                """;

        mockMvc.perform(put("/api/users/me/role")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void AT69_profileDataIsScopedToAuthenticatedUser() throws Exception {
        String firstEmail = uniqueEmail("profile1");
        String secondEmail = uniqueEmail("profile2");

        String firstToken = registerAndGetToken(firstEmail, "DRIVER");
        String secondToken = registerAndGetToken(secondEmail, "DRIVER");

        String firstResponse = mockMvc.perform(get("/api/users/me")
                        .header("Authorization", bearer(firstToken)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String secondResponse = mockMvc.perform(get("/api/users/me")
                        .header("Authorization", bearer(secondToken)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode firstUser = objectMapper.readTree(firstResponse);
        JsonNode secondUser = objectMapper.readTree(secondResponse);

        assertEquals(firstEmail, firstUser.get("email").asText());
        assertEquals(secondEmail, secondUser.get("email").asText());
        assertNotEquals(firstUser.get("id").asLong(), secondUser.get("id").asLong());
    }

    @Test
    void AT70_unauthenticatedUserCannotAccessProfileEndpoints() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().is4xxClientError());

        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().is4xxClientError());

        mockMvc.perform(put("/api/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().is4xxClientError());

        mockMvc.perform(put("/api/users/me/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().is4xxClientError());
    }
}