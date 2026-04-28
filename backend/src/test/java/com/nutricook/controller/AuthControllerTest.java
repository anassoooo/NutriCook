package com.nutricook.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

  @Autowired MockMvc mockMvc;

  @Test
  void register_returnsCreatedWithToken() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {"email":"newuser@example.com","password":"Password123!"}
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.token").isNotEmpty())
        .andExpect(jsonPath("$.email").value("newuser@example.com"))
        .andExpect(jsonPath("$.role").value("USER"));
  }

  @Test
  void register_duplicateEmail_returns409() throws Exception {
    String body = """
        {"email":"dup@example.com","password":"Password123!"}
        """;
    mockMvc
        .perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isCreated());

    mockMvc
        .perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isConflict());
  }

  @Test
  void login_withValidCredentials_returnsToken() throws Exception {
    // Register first
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {"email":"login@example.com","password":"Password123!"}
                """))
        .andExpect(status().isCreated());

    // Then login
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {"email":"login@example.com","password":"Password123!"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").isNotEmpty())
        .andExpect(jsonPath("$.email").value("login@example.com"));
  }

  @Test
  void login_withWrongPassword_returns401() throws Exception {
    // Register first
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {"email":"wrongpass@example.com","password":"Password123!"}
                """))
        .andExpect(status().isCreated());

    // Login with wrong password
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {"email":"wrongpass@example.com","password":"WrongPassword!"}
                """))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void register_withShortPassword_returns422() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {"email":"short@example.com","password":"123"}
                """))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void register_withInvalidEmail_returns422() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {"email":"not-an-email","password":"Password123!"}
                """))
        .andExpect(status().isUnprocessableEntity());
  }
}
