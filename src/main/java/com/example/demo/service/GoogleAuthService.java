package com.example.demo.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.util.Collections;

import com.example.demo.dto.GoogleLoginRequest;

@Service
public class GoogleAuthService {

    private static final Logger log = LoggerFactory.getLogger(GoogleAuthService.class);

    @Value("${google.client-id}")
    private String clientId;

    public GoogleLoginRequest verifyToken(String tokenString) {
        // Do NOT log tokenString
        log.info("action=google_verify start");

        try {
            GoogleIdTokenVerifier verifier =
                    new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                            .setAudience(Collections.singletonList(clientId))
                            .build();

            GoogleIdToken idToken = verifier.verify(tokenString);
            if (idToken == null) {
                log.warn("action=google_verify fail reason=INVALID_ID_TOKEN");
                throw new IllegalArgumentException("Invalid Google ID Token");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();

            GoogleLoginRequest dto = new GoogleLoginRequest();
            dto.setEmail(payload.getEmail());
            dto.setFullName((String) payload.get("name"));
            dto.setGoogleUserId(payload.getSubject());

            log.info("action=google_verify success email={} hasName={} hasSub={}",
                    safeEmail(dto.getEmail()),
                    dto.getFullName() != null && !dto.getFullName().isBlank(),
                    dto.getGoogleUserId() != null && !dto.getGoogleUserId().isBlank());

            return dto;

        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception e) {
            log.error("action=google_verify fail reason={}", e.getClass().getSimpleName(), e);
            throw new RuntimeException("Google token verification failed", e);
        }
    }

    private String safeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
