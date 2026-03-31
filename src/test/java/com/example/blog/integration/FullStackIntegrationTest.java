package com.example.blog.integration;

import com.example.blog.model.dto.*;
import com.example.blog.repository.PostRepository;
import com.example.blog.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full-stack integration test covering the complete user journey:
 * 1. Register a new user
 * 2. Login with those credentials
 * 3. Create a blog post
 * 4. Edit the post
 * 5. Delete the post
 *
 * This test verifies JWT authentication, authorization, and ownership enforcement in a single workflow.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FullStackIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private String jwtToken;
    private Long userId;
    private Long postId;

    @BeforeEach
    void setUp() throws Exception {
        // Clean up
        postRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void fullUserJourney_registerLoginCreateEditDelete() throws Exception {
        // ========================================
        // STEP 1: Register a new user
        // ========================================
        RegisterUserRequest registerRequest = new RegisterUserRequest();
        registerRequest.setUsername("integrationuser");
        registerRequest.setEmail("integration@example.com");
        registerRequest.setPassword("SecurePass123!");
        registerRequest.setFirstName("Integration");
        registerRequest.setLastName("User");
        registerRequest.setPhone("+1234567890");

        String registerResponse = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username", is("integrationuser")))
                .andExpect(jsonPath("$.email", is("integration@example.com")))
                .andExpect(jsonPath("$.isActive", is(true)))
                .andReturn().getResponse().getContentAsString();

        // Extract userId from response (alternatively retrieve from DB)
        userId = objectMapper.readTree(registerResponse).path("id").asLong();

        // Verify user exists in DB
        assertTrue(userRepository.findByUsername("integrationuser").isPresent());

        // ========================================
        // STEP 2: Login
        // ========================================
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("integrationuser");
        loginRequest.setPassword("SecurePass123!");

        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andReturn().getResponse().getContentAsString();

        jwtToken = objectMapper.readTree(loginResponse).path("accessToken").asText();
        assertNotNull(jwtToken);

        // ========================================
        // STEP 3: Create a post
        // ========================================
        CreatePostRequest createPostRequest = new CreatePostRequest();
        createPostRequest.setTitle("My Integration Post");
        createPostRequest.setContent("This post was created during full integration test.");
        createPostRequest.setPublished(true);

        String createResponse = mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPostRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("My Integration Post")))
                .andExpect(jsonPath("$.content", is("This post was created during full integration test.")))
                .andExpect(jsonPath("$.published", is(true)))
                .andExpect(jsonPath("$.author.username", is("integrationuser")))
                .andReturn().getResponse().getContentAsString();

        postId = objectMapper.readTree(createResponse).path("id").asLong();

        // Verify post in DB
        assertTrue(postRepository.findById(postId).isPresent());

        // ========================================
        // STEP 4: Edit the post
        // ========================================
        UpdatePostRequest updateRequest = new UpdatePostRequest();
        updateRequest.setTitle("Updated Integration Post");
        updateRequest.setContent("This content has been updated.");
        updateRequest.setPublished(false);

        mockMvc.perform(put("/api/v1/posts/{id}", postId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Updated Integration Post")))
                .andExpect(jsonPath("$.content", is("This content has been updated.")))
                .andExpect(jsonPath("$.published", is(false)));

        // Verify update in DB
        assertTrue(postRepository.findById(postId).isPresent());
        assertEquals("Updated Integration Post", postRepository.findById(postId).get().getTitle());

        // ========================================
        // STEP 5: Delete the post
        // ========================================
        mockMvc.perform(delete("/api/v1/posts/{id}", postId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNoContent());

        // Verify deletion
        assertTrue(postRepository.findById(postId).isEmpty());
    }

    @Test
    void fullUserJourney_withInvalidToken_shouldFailAtCreate() throws Exception {
        // Register and login
        RegisterUserRequest registerRequest = new RegisterUserRequest();
        registerRequest.setUsername("user2");
        registerRequest.setEmail("user2@example.com");
        registerRequest.setPassword("Pass123!");
        registerRequest.setFirstName("User");
        registerRequest.setLastName("Two");
        registerRequest.setPhone("+1234567890");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Attempt to create post without token (or with invalid token)
        CreatePostRequest createPostRequest = new CreatePostRequest();
        createPostRequest.setTitle("No Auth Post");
        createPostRequest.setContent("Should fail");
        createPostRequest.setPublished(true);

        mockMvc.perform(post("/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPostRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void fullUserJourney_ownershipEnforcement_otherUserCannotEditOrDelete() throws Exception {
        // Create first user and login
        RegisterUserRequest user1Reg = new RegisterUserRequest();
        user1Reg.setUsername("owner");
        user1Reg.setEmail("owner@example.com");
        user1Reg.setPassword("Pass123!");
        user1Reg.setFirstName("Owner");
        user1Reg.setLastName("User");
        user1Reg.setPhone("+1234567890");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user1Reg)))
                .andExpect(status().isCreated());

        // Login as user1
        LoginRequest user1Login = new LoginRequest();
        user1Login.setUsername("owner");
        user1Login.setPassword("Pass123!");
        String user1Token = objectMapper.readTree(
                mockMvc.perform(post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(user1Login)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString()
        ).path("accessToken").asText();

        // User1 creates a post
        CreatePostRequest createPost = new CreatePostRequest();
        createPost.setTitle("Owner Post");
        createPost.setContent("Owned by user1");
        createPost.setPublished(true);

        String postResponse = mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPost)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long postId = objectMapper.readTree(postResponse).path("id").asLong();

        // Create second user and login
        RegisterUserRequest user2Reg = new RegisterUserRequest();
        user2Reg.setUsername("intruder");
        user2Reg.setEmail("intruder@example.com");
        user2Reg.setPassword("Pass123!");
        user2Reg.setFirstName("Intruder");
        user2Reg.setLastName("User");
        user2Reg.setPhone("+1234567890");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user2Reg)))
                .andExpect(status().isCreated());

        LoginRequest user2Login = new LoginRequest();
        user2Login.setUsername("intruder");
        user2Login.setPassword("Pass123!");
        String user2Token = objectMapper.readTree(
                mockMvc.perform(post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(user2Login)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString()
        ).path("accessToken").asText();

        // Attempt to edit post as user2 (should fail)
        UpdatePostRequest updateRequest = new UpdatePostRequest();
        updateRequest.setTitle("Hacked Title");
        updateRequest.setContent("Hacked content");

        mockMvc.perform(put("/api/v1/posts/{id}", postId)
                        .header("Authorization", "Bearer " + user2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());

        // Attempt to delete post as user2 (should fail)
        mockMvc.perform(delete("/api/v1/posts/{id}", postId)
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isForbidden());

        // Verify post still exists unchanged
        assertTrue(postRepository.findById(postId).isPresent());
        assertEquals("Owner Post", postRepository.findById(postId).get().getTitle());
    }
}
