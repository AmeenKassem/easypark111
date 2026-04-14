package com.example.demo.security;

import com.example.demo.model.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    @Value("${security.jwt.secret}")
    private String secret;

    @Value("${security.jwt.expiration-minutes}")
    private long expiryMinutes;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant exp = now.plus(Duration.ofMinutes(expiryMinutes));

        String jwt = Jwts.builder()
                // CRITICAL: Set Subject as User ID (String) to prevent NumberFormatException in the Filter
                .setSubject(String.valueOf(user.getId()))
                
                // CRITICAL: Add Role claim to prevent NullPointerException in the Filter
                .claim("role", user.getRole().name())
                
                // OPTIONAL: Add email and name for frontend convenience (decoding token directly)
                .claim("email", user.getEmail())
                .claim("fullName", user.getFullName())
                
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();

        log.debug("action=jwt_generate success userId={} role={} exp={}", user.getId(), user.getRole(), exp);
        return jwt;
    }

    public Claims parseClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException ex) {
            log.warn("action=jwt_parse fail reason={}", ex.getClass().getSimpleName());
            throw ex;
        }
    }

    public Long getUserId(String token) {
        return Long.valueOf(parseClaims(token).getSubject());
    }

    public String getRole(String token) {
        return (String) parseClaims(token).get("role");
    }
}
