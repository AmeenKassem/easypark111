package com.example.demo.controller;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        log.warn("action=exception client_error path={} msg={}", req.getRequestURI(), ex.getMessage());

        Map<String, String> body = new HashMap<>();
        // Frontend expects "message"
        body.put("message", ex.getMessage());
        // Keep "error" too (won’t hurt, helps other callers)
        body.put("error", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatus(ResponseStatusException ex) {
        log.warn("Client/status error: {}", ex.getReason());

        Map<String, String> body = new HashMap<>();
        body.put("error", ex.getReason() != null ? ex.getReason() : ex.getMessage());

        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> body = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err ->
                body.put(err.getField(), err.getDefaultMessage())
        );

        log.warn("action=exception validation_error path={} details={}", req.getRequestURI(), body);

        // Also provide a generic "message" so the UI can show something consistent
        Map<String, String> out = new HashMap<>();
        out.put("message", "Validation error");
        out.put("error", "Validation error");
        // Include field-level errors too
        out.putAll(body);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(out);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        log.warn("action=exception access_denied path={} msg={}", req.getRequestURI(), ex.getMessage());

        Map<String, String> body = new HashMap<>();
        body.put("message", "Forbidden");
        body.put("error", "Forbidden");

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleOther(Exception ex, HttpServletRequest req) {
        log.error("action=exception server_error path={}", req.getRequestURI(), ex);

        Map<String, String> body = new HashMap<>();
        body.put("message", "Internal server error");
        body.put("error", "Internal server error");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
