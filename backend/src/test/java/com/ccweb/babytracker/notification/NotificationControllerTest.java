package com.ccweb.babytracker.notification;

import com.ccweb.babytracker.auth.AuthService;
import com.ccweb.babytracker.auth.dto.AuthResponse;
import com.ccweb.babytracker.auth.dto.LoginRequest;
import com.ccweb.babytracker.auth.dto.RegisterRequest;
import com.ccweb.babytracker.domain.notification.Notification;
import com.ccweb.babytracker.domain.notification.NotificationRepository;
import com.ccweb.babytracker.domain.user.User;
import com.ccweb.babytracker.domain.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class NotificationControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired AuthService authService;
    @Autowired UserRepository userRepo;
    @Autowired NotificationRepository notificationRepo;

    private String token;
    private String notificationId;

    @BeforeEach
    void setUp() {
        authService.register(new RegisterRequest("mom@test.com", "Secret123!", "Mom"));
        AuthResponse login = authService.login(
                new LoginRequest("mom@test.com", "Secret123!"), new MockHttpServletResponse());
        token = login.accessToken();

        User user = userRepo.findByEmail("mom@test.com").orElseThrow();

        Notification n = new Notification();
        n.setUser(user);
        n.setMessage("疫苗接種提醒：B型肝炎第1劑即將到期");
        n.setType("VACCINE_REMINDER");
        notificationId = notificationRepo.save(n).getId().toString();
    }

    @Test
    void listNotifications() throws Exception {
        mvc.perform(get("/api/notifications")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].message").value("疫苗接種提醒：B型肝炎第1劑即將到期"))
                .andExpect(jsonPath("$[0].read").value(false));
    }

    @Test
    void markNotificationAsRead() throws Exception {
        mvc.perform(patch("/api/notifications/" + notificationId + "/read")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));
    }

    @Test
    void unreadCountEndpoint() throws Exception {
        mvc.perform(get("/api/notifications/unread-count")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));
    }
}
