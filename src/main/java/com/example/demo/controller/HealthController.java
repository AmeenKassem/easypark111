package com.example.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@CrossOrigin(origins = "*")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Checks only that the backend is awake
    @GetMapping
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
                "status", "OK",
                "service", "EasyPark backend",
                "time", Instant.now().toString()
        ));
    }

    // Checks backend + database
    @GetMapping("/db")
    public ResponseEntity<?> dbHealth() {
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);

            return ResponseEntity.ok(Map.of(
                    "status", "OK",
                    "database", "connected",
                    "result", result,
                    "time", Instant.now().toString()
            ));
        } catch (Exception ex) {
            return ResponseEntity.status(503).body(Map.of(
                    "status", "ERROR",
                    "database", "disconnected",
                    "message", ex.getMessage(),
                    "time", Instant.now().toString()
            ));
        }
    }
}