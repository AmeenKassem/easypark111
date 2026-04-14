package com.example.demo.controller;

import com.example.demo.dto.CreateParkingRequest;
import com.example.demo.dto.UpdateParkingRequest;
import com.example.demo.model.AvailabilityType;
import com.example.demo.model.Parking;
import com.example.demo.service.ParkingService;
import com.example.demo.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ParkingController.class)
@AutoConfigureMockMvc(addFilters = false)
class ParkingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ParkingService parkingService;

    @MockitoBean
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private void setEntityId(Object entity, Long id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set ID for testing", e);
        }
    }

    @Test
    void create_ShouldReturn200_WhenValid() throws Exception {
        Long userId = 100L;
        TestingAuthenticationToken auth = new TestingAuthenticationToken(userId, "PWD", "ROLE_OWNER");

        CreateParkingRequest req = new CreateParkingRequest();
        req.setLocation("Tel Aviv");
        req.setPricePerHour(20.0);
        req.setCovered(true);
        req.setAvailabilityType("SPECIFIC");

        Parking mockParking = new Parking();
        setEntityId(mockParking, 10L);
        mockParking.setOwnerId(userId);
        mockParking.setLocation("Tel Aviv");
        mockParking.setPricePerHour(20.0);
        mockParking.setAvailabilityType(AvailabilityType.SPECIFIC);

        when(parkingService.create(eq(userId), any(CreateParkingRequest.class))).thenReturn(mockParking);

        mockMvc.perform(post("/api/parking-spots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void update_ShouldReturn200_WhenValid() throws Exception {
        Long userId = 100L;
        Long parkingId = 10L;
        TestingAuthenticationToken auth = new TestingAuthenticationToken(userId, "PWD", "ROLE_OWNER");

        UpdateParkingRequest req = new UpdateParkingRequest();
        req.setLocation("Updated Loc");
        req.setPricePerHour(30.0);
        req.setActive(true);

        Parking mockParking = new Parking();
        setEntityId(mockParking, parkingId);
        mockParking.setOwnerId(userId);
        mockParking.setLocation("Updated Loc");
        mockParking.setPricePerHour(30.0);
        mockParking.setAvailabilityType(AvailabilityType.SPECIFIC);

        when(parkingService.update(eq(userId), eq(parkingId), any(UpdateParkingRequest.class)))
                .thenReturn(mockParking);

        mockMvc.perform(put("/api/parking-spots/{id}", parkingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.location").value("Updated Loc"));
    }

    @Test
    void delete_ShouldReturn200() throws Exception {
        Long userId = 100L;
        Long parkingId = 10L;
        TestingAuthenticationToken auth = new TestingAuthenticationToken(userId, "PWD", "ROLE_OWNER");
        doNothing().when(parkingService).delete(userId, parkingId);

        mockMvc.perform(delete("/api/parking-spots/{id}", parkingId)
                        .principal(auth))
                .andExpect(status().isOk());
    }

    @Test
    void mySpots_ShouldReturnList() throws Exception {
        Long userId = 100L;
        TestingAuthenticationToken auth = new TestingAuthenticationToken(userId, "PWD", "ROLE_OWNER");
        Parking p = new Parking();
        setEntityId(p, 11L);
        p.setOwnerId(userId);
        p.setLocation("My Spot");
        p.setAvailabilityType(AvailabilityType.SPECIFIC);

        when(parkingService.listMine(userId)).thenReturn(List.of(p));

        mockMvc.perform(get("/api/parking-spots/my")
                        .principal(auth))
                .andExpect(status().isOk());
    }

    @Test
    void search_ShouldReturnList() throws Exception {
        Parking p = new Parking();
        setEntityId(p, 12L);
        p.setLocation("Search Result");
        p.setPricePerHour(15.0);
        p.setAvailabilityType(AvailabilityType.SPECIFIC);

        when(parkingService.search(eq(true), eq(10.0), eq(50.0)))
                .thenReturn(List.of(p));

        mockMvc.perform(get("/api/parking-spots/search")
                        .param("covered", "true")
                        .param("minPrice", "10.0")
                        .param("maxPrice", "50.0"))
                .andExpect(status().isOk());
    }
}