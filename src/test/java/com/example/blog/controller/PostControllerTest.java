package com.example.blog.controller;

import com.example.blog.model.dto.CreatePostRequest;
import com.example.blog.model.dto.PostResponse;
import com.example.blog.model.dto.UpdatePostRequest;
import com.example.blog.service.PostService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PostController.class)
@ActiveProfiles("test")
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PostService postService;

    private User createTestUser(Long userId) {
        return new User(
                userId.toString(),
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    private PostResponse createPostResponse(Long id, String title, String content, Long authorId, boolean published) {
        PostResponse response = PostResponse.builder()
                .id(id)
                .title(title)
                .content(content)
                .published(published)
                .build();
        return response;
    }

    @Test
    void getAllPosts_shouldReturnList_whenPostsExist() throws Exception {
        // Arrange
        PostResponse post1 = createPostResponse(1L, "Post 1", "Content 1", 1L, true);
        PostResponse post2 = createPostResponse(2L, "Post 2", "Content 2", 1L, false);

        when(postService.getAllPosts()).thenReturn(List.of(post1, post2));

        // Act & Assert
        mockMvc.perform(get("/api/v1/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title", is("Post 1")))
                .andExpect(jsonPath("$[1].title", is("Post 2")));
    }

    @Test
    void getAllPosts_shouldReturnEmptyList_whenNoPosts() throws Exception {
        // Arrange
        when(postService.getAllPosts()).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/v1/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getAllPosts_shouldReturn400_whenInvalidPagination() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/posts")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/posts")
                        .param("size", "0"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/posts")
                        .param("size", "101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPostById_shouldReturnPost_whenExists() throws Exception {
        // Arrange
        PostResponse response = createPostResponse(1L, "Test Post", "Test content", 1L, true);
        when(postService.getPostById(1L)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/posts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.title", is("Test Post")));
    }

    @Test
    void getPostById_shouldReturn404_whenPostNotFound() throws Exception {
        // Arrange
        when(postService.getPostById(999L))
                .thenThrow(new com.example.blog.exception.ResourceNotFoundException("Post not found"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/posts/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("Not Found")));
    }

    @Test
    void getPostById_shouldReturn400_whenInvalidId() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/posts/abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPost_shouldReturn201_whenValidRequest_andAuthenticated() throws Exception {
        // Arrange
        CreatePostRequest request = new CreatePostRequest();
        request.setTitle("New Post");
        request.setContent("New content");
        request.setPublished(true);

        PostResponse response = createPostResponse(1L, "New Post", "New content", 1L, true);

        when(postService.createPost(any(CreatePostRequest.class), eq(1L))).thenReturn(response);

        User authUser = createTestUser(1L);

        // Act & Assert
        mockMvc.perform(post("/api/v1/posts")
                        .with(authentication(authUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("New Post")))
                .andExpect(jsonPath("$.published", is(true)));
    }

    @Test
    void createPost_shouldReturn400_whenUnauthenticated() throws Exception {
        // Arrange
        CreatePostRequest request = new CreatePostRequest();
        request.setTitle("New Post");
        request.setContent("New content");

        // Act & Assert
        mockMvc.perform(post("/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createPost_shouldReturn400_whenInvalidRequest() throws Exception {
        // Arrange
        CreatePostRequest request = new CreatePostRequest();
        request.setTitle(""); // Empty title
        request.setContent("Content");

        User authUser = createTestUser(1L);

        // Act & Assert
        mockMvc.perform(post("/api/v1/posts")
                        .with(authentication(authUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPost_shouldReturn404_whenUserNotFound() throws Exception {
        // Arrange
        CreatePostRequest request = new CreatePostRequest();
        request.setTitle("New Post");
        request.setContent("Content");

        when(postService.createPost(any(CreatePostRequest.class), eq(999L)))
                .thenThrow(new com.example.blog.exception.ResourceNotFoundException("User not found"));

        User authUser = createTestUser(999L);

        // Act & Assert
        mockMvc.perform(post("/api/v1/posts")
                        .with(authentication(authUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updatePost_shouldReturn200_whenValidRequest_andOwner() throws Exception {
        // Arrange
        Long postId = 1L;
        UpdatePostRequest request = new UpdatePostRequest();
        request.setTitle("Updated Title");
        request.setContent("Updated content");
        request.setPublished(true);

        PostResponse response = createPostResponse(postId, "Updated Title", "Updated content", 1L, true);

        when(postService.updatePost(eq(postId), any(UpdatePostRequest.class), eq(1L))).thenReturn(response);

        User authUser = createTestUser(1L);

        // Act & Assert
        mockMvc.perform(put("/api/v1/posts/{id}", postId)
                        .with(authentication(authUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Updated Title")))
                .andExpect(jsonPath("$.content", is("Updated content")));
    }

    @Test
    void updatePost_shouldReturn403_whenUserIsNotOwner() throws Exception {
        // Arrange
        Long postId = 1L;
        UpdatePostRequest request = new UpdatePostRequest();
        request.setTitle("Updated Title");
        request.setContent("Updated content");

        when(postService.updatePost(eq(postId), any(UpdatePostRequest.class), eq(2L)))
                .thenThrow(new org.springframework.security.AccessDeniedException("Access denied"));

        User authUser = createTestUser(2L); // Different user

        // Act & Assert
        mockMvc.perform(put("/api/v1/posts/{id}", postId)
                        .with(authentication(authUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updatePost_shouldReturn404_whenPostNotFound() throws Exception {
        // Arrange
        Long postId = 999L;
        UpdatePostRequest request = new UpdatePostRequest();
        request.setTitle("Updated Title");
        request.setContent("Updated content");

        when(postService.updatePost(eq(postId), any(UpdatePostRequest.class), eq(1L)))
                .thenThrow(new com.example.blog.exception.ResourceNotFoundException("Post not found"));

        User authUser = createTestUser(1L);

        // Act & Assert
        mockMvc.perform(put("/api/v1/posts/{id}", postId)
                        .with(authentication(authUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updatePost_shouldReturn400_whenUnauthenticated() throws Exception {
        // Arrange
        UpdatePostRequest request = new UpdatePostRequest();
        request.setTitle("Updated Title");
        request.setContent("Updated content");

        // Act & Assert
        mockMvc.perform(put("/api/v1/posts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deletePost_shouldReturn204_whenValidRequest_andOwner() throws Exception {
        // Arrange
        Long postId = 1L;
        doNothing().when(postService).deletePost(eq(postId), eq(1L));

        User authUser = createTestUser(1L);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/posts/{id}", postId)
                        .with(authentication(authUser)))
                .andExpect(status().isNoContent());
    }

    @Test
    void deletePost_shouldReturn403_whenUserIsNotOwner() throws Exception {
        // Arrange
        Long postId = 1L;
        when(postService.deletePost(eq(postId), eq(2L)))
                .thenThrow(new org.springframework.security.AccessDeniedException("Access denied"));

        User authUser = createTestUser(2L);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/posts/{id}", postId)
                        .with(authentication(authUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deletePost_shouldReturn404_whenPostNotFound() throws Exception {
        // Arrange
        Long postId = 999L;
        when(postService.deletePost(eq(postId), eq(1L)))
                .thenThrow(new com.example.blog.exception.ResourceNotFoundException("Post not found"));

        User authUser = createTestUser(1L);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/posts/{id}", postId)
                        .with(authentication(authUser)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletePost_shouldReturn400_whenUnauthenticated() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/v1/posts/1"))
                .andExpect(status().isUnauthorized());
    }
}
