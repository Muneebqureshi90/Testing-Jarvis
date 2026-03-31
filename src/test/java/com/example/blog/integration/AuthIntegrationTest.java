package com.example.blog.integration;

import com.example.blog.model.dto.*;
import com.example.blog.model.entity.User;
import com.example.blog.repository.UserRepository;
import com.example.blog.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private String validJwtToken;

    @BeforeEach
    void setUp() throws Exception {
        // Clean up
        userRepository.deleteAll();

        // Register a test user
        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .passwordHash("$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj12NHe1zhCO") // Password: password123
                .firstName("Test")
                .lastName("User")
                .active(true)
                .verified(false)
                .build();
        userRepository.save(user);
        Long userId = user.getId();

        // Generate JWT token manually for testing protected endpoints
        validJwtToken = jwtUtil.generateAccessToken(user.getUsername(), userId);
    }

    @Test
    void register_shouldCreateUser_andReturn201() throws Exception {
        // Arrange
        RegisterUserRequest request = new RegisterUserRequest();
        request.setUsername("newuser");
        request.setEmail("newuser@example.com");
        request.setPassword("Password123!");
        request.setFirstName("New");
        request.setLastName("User");
        request.setPhone("+1234567890");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\n" +
                                "  \"username\":\"newuser\",\n" +
                                "  \"email\":\"newuser@example.com\",\n" +
                                "  \"password\":\"Password123!\",\n" +
                                "  \"firstName\":\"New\",\n" +
                                "  \"lastName\":\"User\",\n" +
                                "  \"phone\":\"+1234567890\"\n" +
                                "}"))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username", is("newuser")))
                .andExpect(jsonPath("$.email", is("newuser@example.com")))
                .andExpect(jsonPath("$.isActive", is(true)))
                .andExpect(jsonPath("$.isVerified", is(false)));

        // Verify user saved in DB
        assertTrue(userRepository.findByUsername("newuser").isPresent());
    }

    @Test
    void login_shouldReturnTokens_whenCredentialsValid() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\n" +
                                "  \"username\":\"testuser\",\n" +
                                "  \"password\":\"password123\"\n" +
                                "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.expiresIn", is(86400)))
                .andExpect(jsonPath("$.user.username", is("testuser")));
    }

    @Test
    void login_shouldReturn401_whenInvalidPassword() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\n" +
                                "  \"username\":\"testuser\",\n" +
                                "  \"password\":\"wrongpass\"\n" +
                                "}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("Unauthorized")));
    }

    @Test
    void refreshToken_shouldReturnNewTokens_whenValidRefreshToken() throws Exception {
        // First login to get refresh token
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\",\"password\":\"password123\"}"))
                .andReturn().getResponse().getContentAsString();

        // Extract refresh token from response (simplified - in practice use ObjectMapper)
        com.fasterxml.jackson.databind.JsonNode json = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(response);
        String refreshToken = json.get("refreshToken").asText();

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()));
    }

    @Test
    void protectedEndpoint_shouldRequireAuthentication() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/posts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_shouldAllowAccess_withValidToken() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/posts")
                        .header("Authorization", "Bearer " + validJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0))); // No posts created yet
    }
}
