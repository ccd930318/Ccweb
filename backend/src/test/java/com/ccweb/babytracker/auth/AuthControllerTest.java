package com.ccweb.babytracker.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AuthControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    @Test
    void registerCreatesUserAndReturnsToken() throws Exception {
        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(Map.of(
                        "email", "alice@example.com",
                        "password", "Secret123!",
                        "name", "Alice"
                ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isString());
    }

    @Test
    void loginWithCorrectCredentialsReturnsToken() throws Exception {
        // Register first
        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(Map.of(
                        "email", "bob@example.com",
                        "password", "Secret123!",
                        "name", "Bob"
                )))).andExpect(status().isCreated());

        // Login
        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(Map.of(
                        "email", "bob@example.com",
                        "password", "Secret123!"
                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString());
    }

    @Test
    void loginWithWrongPasswordReturns401() throws Exception {
        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(Map.of(
                        "email", "nobody@example.com",
                        "password", "wrong"
                ))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registerWithDuplicateEmailReturns409() throws Exception {
        String body = om.writeValueAsString(Map.of(
                "email", "dupe@example.com",
                "password", "Secret123!",
                "name", "Dupe"
        ));
        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isConflict());
    }
}
