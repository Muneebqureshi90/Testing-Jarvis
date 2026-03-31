package com.example.blog.integration;

import com.example.blog.model.dto.*;
import com.example.blog.repository.PostRepository;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PostIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private Long userId;
    private String jwtToken;
    private Long otherUserId;
    private String otherJwtToken;

    @BeforeEach
    void setUp() {
        // Clean up
        postRepository.deleteAll();
        userRepository.deleteAll();

        // Create test users
        var user = new com.example.blog.model.entity.User();
        user.setUsername("postowner");
        user.setEmail("owner@example.com");
        user.setPasswordHash("$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj12NHe1zhCO");
        user.setFirstName("Post");
        user.setLastName("Owner");
        user.setActive(true);
        userRepository.save(user);
        userId = user.getId();
        jwtToken = jwtUtil.generateAccessToken(user.getUsername(), userId);

        var otherUser = new com.example.blog.model.entity.User();
        otherUser.setUsername("otheruser");
        otherUser.setEmail("other@example.com");
        otherUser.setPasswordHash("$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj12NHe1zhCO");
        otherUser.setFirstName("Other");
        otherUser.setLastName("User");
        otherUser.setActive(true);
        userRepository.save(otherUser);
        otherUserId = otherUser.getId();
        otherJwtToken = jwtUtil.generateAccessToken(otherUser.getUsername(), otherUserId);
    }

    @Test
    void createPost_shouldReturn201_andSaveToDb() throws Exception {
        // Arrange
        CreatePostRequest request = new CreatePostRequest();
        request.setTitle("Integration Test Post");
        request.setContent("Content created during integration test");
        request.setPublished(true);

        // Act & Assert
        mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Integration Test Post\",\"content\":\"Content created during integration test\",\"published\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("Integration Test Post")))
                .andExpect(jsonPath("$.content", is("Content created during integration test")))
                .andExpect(jsonPath("$.published", is(true)))
                .andExpect(jsonPath("$.author.id", is((int) userId.intValue())));

        // Verify post exists in database
        assertTrue(postRepository.findAll().stream()
                .anyMatch(p -> p.getTitle().equals("Integration Test Post")));
    }

    @Test
    void getPostById_shouldReturnPost_whenExists() throws Exception {
        // Arrange: create a post directly in DB
        var post = new com.example.blog.model.entity.Post();
        post.setTitle("Direct Post");
        post.setContent("Direct content");
        post.setAuthor(userRepository.findById(userId).get());
        post.setPublished(true);
        postRepository.save(post);
        Long postId = post.getId();

        // Act & Assert
        mockMvc.perform(get("/api/v1/posts/{id}", postId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Direct Post")))
                .andExpect(jsonPath("$.content", is("Direct content")));
    }

    @Test
    void getPostById_shouldReturn404_whenNotExists() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/posts/999")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllPosts_shouldReturnAllPostsOrderedByCreatedAtDesc() throws Exception {
        // Arrange: create multiple posts
        var post1 = new com.example.blog.model.entity.Post();
        post1.setTitle("First Post");
        post1.setContent("First content");
        post1.setAuthor(userRepository.findById(userId).get());
        post1.setPublished(true);
        postRepository.save(post1);

        var post2 = new com.example.blog.model.entity.Post();
        post2.setTitle("Second Post");
        post2.setContent("Second content");
        post2.setAuthor(userRepository.findById(userId).get());
        post2.setPublished(false);
        postRepository.save(post2);

        // Act & Assert (second post created later should appear first)
        mockMvc.perform(get("/api/v1/posts")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title", is("Second Post")))
                .andExpect(jsonPath("$[1].title", is("First Post")));
    }

    @Test
    void updatePost_shouldReturn200_whenValidOwnerRequest() throws Exception {
        // Arrange: create a post
        var post = new com.example.blog.model.entity.Post();
        post.setTitle("Original Title");
        post.setContent("Original content");
        post.setAuthor(userRepository.findById(userId).get());
        post.setPublished(false);
        postRepository.save(post);
        Long postId = post.getId();

        UpdatePostRequest request = new UpdatePostRequest();
        request.setTitle("Updated Title");
        request.setContent("Updated content");
        request.setPublished(true);

        // Act & Assert
        mockMvc.perform(put("/api/v1/posts/{id}", postId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated Title\",\"content\":\"Updated content\",\"published\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Updated Title")))
                .andExpect(jsonPath("$.content", is("Updated content")))
                .andExpect(jsonPath("$.published", is(true)));
    }

    @Test
    void updatePost_shouldReturn403_whenUserIsNotAuthor() throws Exception {
        // Arrange: create a post as user1
        var post = new com.example.blog.model.entity.Post();
        post.setTitle("User1 Post");
        post.setContent("User1 content");
        post.setAuthor(userRepository.findById(userId).get());
        post.setPublished(false);
        postRepository.save(post);
        Long postId = post.getId();

        UpdatePostRequest request = new UpdatePostRequest();
        request.setTitle("Hacked Title");
        request.setContent("Hacked content");

        // Act & Assert: try to update as different user
        mockMvc.perform(put("/api/v1/posts/{id}", postId)
                        .header("Authorization", "Bearer " + otherJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Hacked Title\",\"content\":\"Hacked content\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deletePost_shouldReturn204_whenOwner() throws Exception {
        // Arrange: create a post
        var post = new com.example.blog.model.entity.Post();
        post.setTitle("To Delete");
        post.setContent("Delete me");
        post.setAuthor(userRepository.findById(userId).get());
        postRepository.save(post);
        Long postId = post.getId();

        // Act & Assert
        mockMvc.perform(delete("/api/v1/posts/{id}", postId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNoContent());

        // Verify deleted
        assertTrue(postRepository.findById(postId).isEmpty());
    }

    @Test
    void deletePost_shouldReturn403_whenNotOwner() throws Exception {
        // Arrange: create a post as user1
        var post = new com.example.blog.model.entity.Post();
        post.setTitle("User1 Post");
        post.setContent("User1 content");
        post.setAuthor(userRepository.findById(userId).get());
        postRepository.save(post);
        Long postId = post.getId();

        // Act & Assert: try to delete as user2
        mockMvc.perform(delete("/api/v1/posts/{id}", postId)
                        .header("Authorization", "Bearer " + otherJwtToken))
                .andExpect(status().isForbidden());

        // Verify not deleted
        assertTrue(postRepository.findById(postId).isPresent());
    }
}
