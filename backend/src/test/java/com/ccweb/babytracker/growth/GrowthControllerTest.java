package com.ccweb.babytracker.growth;

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
class GrowthControllerTest {

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
    void createGrowthRecord() throws Exception {
        mvc.perform(post("/api/babies/" + babyId + "/growth")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"measurementDate":"2025-12-01","weightKg":7.5,"heightCm":68.0,"headCm":43.5}
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.measurementDate").value("2025-12-01"))
                .andExpect(jsonPath("$.weightKg").value(7.5))
                .andExpect(jsonPath("$.heightCm").value(68.0));
    }

    @Test
    void listGrowthRecords() throws Exception {
        mvc.perform(post("/api/babies/" + babyId + "/growth")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"measurementDate":"2025-12-01","weightKg":7.5}
                """))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/babies/" + babyId + "/growth")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].weightKg").value(7.5));
    }

    @Test
    void responseIncludesPercentileFields() throws Exception {
        String body = mvc.perform(post("/api/babies/" + babyId + "/growth")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"measurementDate":"2025-12-01","weightKg":7.5,"heightCm":68.0}
                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        var tree = om.readTree(body);
        // percentile fields must exist in the response (value may be null if WHO data missing)
        assert tree.has("weightPercentile");
        assert tree.has("heightPercentile");
        assert tree.has("headPercentile");
    }

    @Test
    void nonMemberCannotAccessGrowth() throws Exception {
        authService.register(new RegisterRequest("other@test.com", "Secret123!", "Other"));
        AuthResponse other = authService.login(
                new LoginRequest("other@test.com", "Secret123!"), new MockHttpServletResponse());

        mvc.perform(get("/api/babies/" + babyId + "/growth")
                .header("Authorization", "Bearer " + other.accessToken()))
                .andExpect(status().isForbidden());
    }
}
