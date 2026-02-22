package com.ccweb.babytracker.baby;

import com.ccweb.babytracker.auth.AuthService;
import com.ccweb.babytracker.auth.dto.AuthResponse;
import com.ccweb.babytracker.auth.dto.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BabyControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired AuthService authService;

    private String token;

    @BeforeEach
    void setUp() {
        MockHttpServletResponse mockResp = new MockHttpServletResponse();
        authService.register(new RegisterRequest("mom@test.com", "Secret123!", "Mom"));
        AuthResponse login = authService.login(
                new com.ccweb.babytracker.auth.dto.LoginRequest("mom@test.com", "Secret123!"),
                mockResp);
        token = login.accessToken();
    }

    @Test
    void createBabyAndList() throws Exception {
        // Create
        mvc.perform(post("/api/babies")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"小明","birthDate":"2025-11-01","gender":"M"}
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("小明"))
                .andExpect(jsonPath("$.birthDate").value("2025-11-01"));

        // List
        mvc.perform(get("/api/babies")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("小明"));
    }

    @Test
    void getBabyById() throws Exception {
        String responseBody = mvc.perform(post("/api/babies")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"小花","birthDate":"2025-06-15","gender":"F"}
                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String id = om.readTree(responseBody).get("id").asText();

        mvc.perform(get("/api/babies/" + id)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("小花"));
    }

    @Test
    void unauthorizedRequestIsRejected() throws Exception {
        mvc.perform(get("/api/babies"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cannotAccessAnotherUsersBaby() throws Exception {
        // Create baby as mom
        String responseBody = mvc.perform(post("/api/babies")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"小明","birthDate":"2025-11-01"}
                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = om.readTree(responseBody).get("id").asText();

        // Register another user
        MockHttpServletResponse mockResp2 = new MockHttpServletResponse();
        authService.register(new RegisterRequest("dad@test.com", "Secret123!", "Dad"));
        AuthResponse login2 = authService.login(
                new com.ccweb.babytracker.auth.dto.LoginRequest("dad@test.com", "Secret123!"),
                mockResp2);
        String dadToken = login2.accessToken();

        // Dad cannot access mom's baby
        mvc.perform(get("/api/babies/" + id)
                .header("Authorization", "Bearer " + dadToken))
                .andExpect(status().isNotFound());
    }
}
