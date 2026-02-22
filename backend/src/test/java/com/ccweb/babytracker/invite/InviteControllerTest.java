package com.ccweb.babytracker.invite;

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
class InviteControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired AuthService authService;

    private String momToken;
    private String dadToken;

    @BeforeEach
    void setUp() {
        authService.register(new RegisterRequest("mom@test.com", "Secret123!", "Mom"));
        AuthResponse momLogin = authService.login(
                new LoginRequest("mom@test.com", "Secret123!"), new MockHttpServletResponse());
        momToken = momLogin.accessToken();

        authService.register(new RegisterRequest("dad@test.com", "Secret123!", "Dad"));
        AuthResponse dadLogin = authService.login(
                new LoginRequest("dad@test.com", "Secret123!"), new MockHttpServletResponse());
        dadToken = dadLogin.accessToken();
    }

    private String createBaby(String token) throws Exception {
        String body = mvc.perform(post("/api/babies")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"小明","birthDate":"2025-11-01","gender":"M"}
                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return om.readTree(body).get("id").asText();
    }

    @Test
    void ownerCanInviteMember() throws Exception {
        String babyId = createBaby(momToken);

        mvc.perform(post("/api/babies/" + babyId + "/invites")
                .header("Authorization", "Bearer " + momToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"dad@test.com","role":"viewer"}
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void inviteeCanAcceptInvite() throws Exception {
        String babyId = createBaby(momToken);

        String inviteBody = mvc.perform(post("/api/babies/" + babyId + "/invites")
                .header("Authorization", "Bearer " + momToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"dad@test.com","role":"viewer"}
                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String inviteToken = om.readTree(inviteBody).get("token").asText();

        mvc.perform(post("/api/invites/" + inviteToken + "/accept")
                .header("Authorization", "Bearer " + dadToken))
                .andExpect(status().isOk());

        mvc.perform(get("/api/babies")
                .header("Authorization", "Bearer " + dadToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("小明"));
    }

    @Test
    void nonOwnerCannotInvite() throws Exception {
        String babyId = createBaby(momToken);

        mvc.perform(post("/api/babies/" + babyId + "/invites")
                .header("Authorization", "Bearer " + dadToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"other@test.com","role":"viewer"}
                """))
                .andExpect(status().isForbidden());
    }
}
