package com.gray.singifyback.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.profiles.active=test")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void register_newUser_returns200WithToken() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "register_test@singify.com",
                              "username": "register_test",
                              "password": "password123"
                            }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value("register_test"))
                .andExpect(jsonPath("$.email").value("register_test@singify.com"));
    }

    @Test
    void login_validCredentials_returns200WithToken() throws Exception {
        // Register first
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "login_test@singify.com",
                              "username": "login_test",
                              "password": "password123"
                            }
                        """))
                .andExpect(status().isOk());

        // Then login
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "login_test@singify.com",
                              "password": "password123"
                            }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value("login_test"));
    }

    @Test
    void login_wrongPassword_returns403() throws Exception {
        // Register first
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "wrongpass_test@singify.com",
                              "username": "wrongpass_test",
                              "password": "correct_password"
                            }
                        """))
                .andExpect(status().isOk());

        // Login with wrong password
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "wrongpass_test@singify.com",
                              "password": "wrong_password"
                            }
                        """))
                .andExpect(status().is4xxClientError());
    }
}
