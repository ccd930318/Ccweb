package com.ccweb.babytracker.log;

import com.ccweb.babytracker.auth.AuthService;
import com.ccweb.babytracker.auth.dto.AuthResponse;
import com.ccweb.babytracker.auth.dto.LoginRequest;
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
class TrackingLogControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired AuthService authService;

    private String token;
    private String babyId;

    @BeforeEach
    void setUp() throws Exception {
        authService.register(new RegisterRequest("mom@test.com", "Secret123!", "Mom"));
        AuthResponse login = authService.login(
                new LoginRequest("mom@test.com", "Secret123!"), new MockHttpServletResponse());
        token = login.accessToken();

        String babyBody = mvc.perform(post("/api/babies")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"小明","birthDate":"2025-11-01","gender":"M"}
                """))
                .andReturn().getResponse().getContentAsString();
        babyId = om.readTree(babyBody).get("id").asText();
    }

    @Test
    void createFeedLog() throws Exception {
        mvc.perform(post("/api/babies/" + babyId + "/logs")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"type":"FEED","occurredAt":"2025-11-02T08:00:00Z","notes":"left breast 10min"}
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.type").value("FEED"))
                .andExpect(jsonPath("$.notes").value("left breast 10min"));
    }

    @Test
    void listLogsForBaby() throws Exception {
        mvc.perform(post("/api/babies/" + babyId + "/logs")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"type":"SLEEP","occurredAt":"2025-11-02T10:00:00Z"}
                """))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/babies/" + babyId + "/logs")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("SLEEP"));
    }

    @Test
    void nonMemberCannotAccessLogs() throws Exception {
        authService.register(new RegisterRequest("dad@test.com", "Secret123!", "Dad"));
        AuthResponse dadLogin = authService.login(
                new LoginRequest("dad@test.com", "Secret123!"), new MockHttpServletResponse());
        String dadToken = dadLogin.accessToken();

        mvc.perform(get("/api/babies/" + babyId + "/logs")
                .header("Authorization", "Bearer " + dadToken))
                .andExpect(status().isForbidden());
    }
}
