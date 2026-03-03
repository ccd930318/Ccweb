package com.ccweb.babytracker.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class JwtUtilsTest {

    @Autowired
    JwtUtils jwtUtils;

    @Test
    void generateAndValidateAccessToken() {
        String token = jwtUtils.generateAccessToken("user-id-123", "USER");
        assertNotNull(token);
        assertTrue(jwtUtils.isValid(token));
        assertEquals("user-id-123", jwtUtils.extractUserId(token));
    }

    @Test
    void expiredTokenIsInvalid() {
        // Pre-built expired JWT (exp=1 second since epoch, always in the past)
        // Header: {"alg":"HS256"}, Payload: {"sub":"x","exp":1}
        String expiredToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ4IiwiZXhwIjoxfQ.invalid";
        assertFalse(jwtUtils.isValid(expiredToken));
    }

    @Test
    void tokenWithWrongSignatureIsInvalid() {
        String token = jwtUtils.generateAccessToken("user-id-123", "USER");
        // Tamper with the signature part
        String tamperedToken = token.substring(0, token.lastIndexOf('.')) + ".tampered";
        assertFalse(jwtUtils.isValid(tamperedToken));
    }
}
