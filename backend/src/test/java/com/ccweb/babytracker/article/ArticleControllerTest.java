package com.ccweb.babytracker.article;

import com.ccweb.babytracker.auth.AuthService;
import com.ccweb.babytracker.auth.dto.AuthResponse;
import com.ccweb.babytracker.auth.dto.LoginRequest;
import com.ccweb.babytracker.auth.dto.RegisterRequest;
import com.ccweb.babytracker.domain.user.User;
import com.ccweb.babytracker.domain.user.UserRepository;
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
class ArticleControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired AuthService authService;
    @Autowired UserRepository userRepo;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        // Create admin user
        authService.register(new RegisterRequest("admin@test.com", "Secret123!", "Admin"));
        User admin = userRepo.findByEmail("admin@test.com").orElseThrow();
        admin.setRole("ADMIN");
        userRepo.save(admin);
        AuthResponse adminLogin = authService.login(
                new LoginRequest("admin@test.com", "Secret123!"), new MockHttpServletResponse());
        adminToken = adminLogin.accessToken();

        // Create regular user
        authService.register(new RegisterRequest("user@test.com", "Secret123!", "User"));
        AuthResponse userLogin = authService.login(
                new LoginRequest("user@test.com", "Secret123!"), new MockHttpServletResponse());
        userToken = userLogin.accessToken();
    }

    @Test
    void adminCanCreateArticle() throws Exception {
        mvc.perform(post("/api/articles")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"寶寶副食品入門","content":"詳細說明...","category":"nutrition"}
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("寶寶副食品入門"));
    }

    @Test
    void regularUserCannotCreateArticle() throws Exception {
        mvc.perform(post("/api/articles")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"test","content":"test","category":"test"}
                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void anyUserCanListAndReadArticles() throws Exception {
        // Admin creates
        mvc.perform(post("/api/articles")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"睡眠訓練","content":"內容...","category":"sleep"}
                """))
                .andExpect(status().isCreated());

        // Regular user lists
        mvc.perform(get("/api/articles")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("睡眠訓練"));
    }
}
