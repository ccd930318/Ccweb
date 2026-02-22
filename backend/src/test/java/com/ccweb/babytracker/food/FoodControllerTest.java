package com.ccweb.babytracker.food;

import com.ccweb.babytracker.auth.AuthService;
import com.ccweb.babytracker.auth.dto.AuthResponse;
import com.ccweb.babytracker.auth.dto.LoginRequest;
import com.ccweb.babytracker.auth.dto.RegisterRequest;
import com.ccweb.babytracker.domain.food.FoodItem;
import com.ccweb.babytracker.domain.food.FoodItemRepository;
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
class FoodControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired AuthService authService;
    @Autowired FoodItemRepository foodItemRepo;

    private String token;
    private String babyId;
    private Long foodItemId;

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

        FoodItem item = new FoodItem();
        item.setName("紅蘿蔔泥");
        item.setCategory("蔬菜");
        item.setRecommendedAgeMonths(6);
        foodItemId = foodItemRepo.save(item).getId();
    }

    @Test
    void listFoodCatalogue() throws Exception {
        mvc.perform(get("/api/foods")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("紅蘿蔔泥"));
    }

    @Test
    void logFoodIntroduction() throws Exception {
        mvc.perform(post("/api/babies/" + babyId + "/food-logs")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("""
                    {"foodItemId":%d,"introducedDate":"2025-05-01","reaction":"接受良好"}
                """, foodItemId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.foodName").value("紅蘿蔔泥"))
                .andExpect(jsonPath("$.introducedDate").value("2025-05-01"));
    }

    @Test
    void listFoodLogs() throws Exception {
        mvc.perform(post("/api/babies/" + babyId + "/food-logs")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("""
                    {"foodItemId":%d,"introducedDate":"2025-05-01"}
                """, foodItemId)))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/babies/" + babyId + "/food-logs")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].foodName").value("紅蘿蔔泥"));
    }

    @Test
    void nonMemberCannotAccessFoodLogs() throws Exception {
        authService.register(new RegisterRequest("other@test.com", "Secret123!", "Other"));
        AuthResponse other = authService.login(
                new LoginRequest("other@test.com", "Secret123!"), new MockHttpServletResponse());

        mvc.perform(get("/api/babies/" + babyId + "/food-logs")
                .header("Authorization", "Bearer " + other.accessToken()))
                .andExpect(status().isForbidden());
    }
}
