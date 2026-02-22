package com.ccweb.babytracker.vaccination;

import com.ccweb.babytracker.auth.AuthService;
import com.ccweb.babytracker.auth.dto.AuthResponse;
import com.ccweb.babytracker.auth.dto.LoginRequest;
import com.ccweb.babytracker.auth.dto.RegisterRequest;
import com.ccweb.babytracker.domain.vaccination.VaccineSchedule;
import com.ccweb.babytracker.domain.vaccination.VaccineScheduleRepository;
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
class VaccinationControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired AuthService authService;
    @Autowired VaccineScheduleRepository scheduleRepo;

    private String token;
    private String babyId;
    private Long scheduleId;

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

        // Seed a schedule entry for tests
        VaccineSchedule vs = new VaccineSchedule();
        vs.setVaccineName("B型肝炎疫苗");
        vs.setDoseNumber(1);
        vs.setRecommendedAgeMonths(0);
        vs.setNotes("出生時接種");
        scheduleId = scheduleRepo.save(vs).getId();
    }

    @Test
    void listSchedule() throws Exception {
        mvc.perform(get("/api/vaccines/schedule")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].vaccineName").value("B型肝炎疫苗"));
    }

    @Test
    void recordVaccination() throws Exception {
        mvc.perform(post("/api/babies/" + babyId + "/vaccinations")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("""
                    {"scheduleId":%d,"administeredDate":"2025-11-01","notes":"左大腿"}
                """, scheduleId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.vaccineName").value("B型肝炎疫苗"))
                .andExpect(jsonPath("$.administeredDate").value("2025-11-01"));
    }

    @Test
    void listVaccinationsForBaby() throws Exception {
        mvc.perform(post("/api/babies/" + babyId + "/vaccinations")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("""
                    {"scheduleId":%d,"administeredDate":"2025-11-01"}
                """, scheduleId)))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/babies/" + babyId + "/vaccinations")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].vaccineName").value("B型肝炎疫苗"));
    }

    @Test
    void nonMemberCannotAccessVaccinations() throws Exception {
        authService.register(new RegisterRequest("other@test.com", "Secret123!", "Other"));
        AuthResponse other = authService.login(
                new LoginRequest("other@test.com", "Secret123!"), new MockHttpServletResponse());

        mvc.perform(get("/api/babies/" + babyId + "/vaccinations")
                .header("Authorization", "Bearer " + other.accessToken()))
                .andExpect(status().isForbidden());
    }
}
