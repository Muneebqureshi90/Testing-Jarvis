package com.example.blog;

import com.example.blog.exception.ResourceNotFoundException;
import com.example.blog.model.dto.CreatePostRequest;
import com.example.blog.model.dto.PostResponse;
import com.example.blog.model.dto.UpdatePostRequest;
import com.example.blog.model.entity.Post;
import com.example.blog.model.entity.User;
import com.example.blog.repository.PostRepository;
import com.example.blog.repository.UserRepository;
import com.example.blog.service.PostService;
import com.example.blog.service.impl.PostServiceImpl;
import com.example.blog.service.mapper.BlogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BlogMapper blogMapper;

    @InjectMocks
    private PostServiceImpl postService;

    @Captor
    private ArgumentCaptor<Post> postCaptor;

    private User author;
    private Post post;

    @BeforeEach
    void setUp() {
        author = User.builder()
                .id(1L)
                .uuid("123e4567-e89b-12d3-a456-426614174000")
                .username("testuser")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .active(true)
                .verified(false)
                .build();

        post = Post.builder()
                .id(1L)
                .title("Test Post")
                .content("Test content")
                .author(author)
                .published(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void createPost_ShouldSavePost_WithCorrectAuthor() {
        // Given
        CreatePostRequest request = new CreatePostRequest();
        request.setTitle("New Post");
        request.setContent("New content");
        request.setPublished(true);

        Long userId = author.getId();

        when(userRepository.findById(userId)).thenReturn(Optional.of(author));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post p = invocation.getArgument(0);
            p.setId(1L);
            p.setCreatedAt(Instant.now());
            p.setUpdatedAt(Instant.now());
            return p;
        });

        PostResponse postResponse = PostResponse.builder()
                .id(1L)
                .title("New Post")
                .content("New content")
                .published(true)
                .build();

        when(blogMapper.toPostResponse(any(Post.class))).thenReturn(postResponse);

        // When
        PostResponse result = postService.createPost(request, userId);

        // Then
        assertNotNull(result);
        assertEquals("New Post", result.getTitle());

        verify(postRepository).save(postCaptor.capture());
        Post savedPost = postCaptor.getValue();
        assertEquals("New Post", savedPost.getTitle());
        assertEquals(author, savedPost.getAuthor());
        assertTrue(savedPost.isPublished());
    }

    @Test
    void createPost_ShouldThrowResourceNotFoundException_WhenUserNotFound() {
        // Given
        CreatePostRequest request = new CreatePostRequest();
        request.setTitle("New Post");
        request.setContent("New content");

        Long userId = 999L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> postService.createPost(request, userId));
        verify(postRepository, never()).save(any());
    }

    @Test
    void createPost_ShouldThrowResourceNotFoundException_WhenUserInactive() {
        // Given
        CreatePostRequest request = new CreatePostRequest();
        request.setTitle("New Post");
        request.setContent("New content");

        Long userId = 999L;
        User inactiveUser = User.builder()
                .id(userId)
                .username("inactive")
                .active(false)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(inactiveUser));

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> postService.createPost(request, userId));
        verify(postRepository, never()).save(any());
    }

    @Test
    void getPostById_ShouldReturnPost_WhenExists() {
        // Given
        post.setId(1L);
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(blogMapper.toPostResponse(post)).thenReturn(PostResponse.builder().id(1L).title("Test Post").build());

        // When
        PostResponse result = postService.getPostById(1L);

        // Then
        assertNotNull(result);
        assertEquals("Test Post", result.getTitle());
    }

    @Test
    void getPostById_ShouldThrowResourceNotFoundException_WhenPostNotFound() {
        // Given
        Long postId = 999L;
        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> postService.getPostById(postId));
    }

    @Test
    void getAllPosts_ShouldReturnAllPostsOrderedByCreatedAtDesc() {
        // Given
        Post post2 = Post.builder()
                .id(2L)
                .title("Second Post")
                .content("Second content")
                .author(author)
                .published(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        List<Post> posts = Arrays.asList(post2, post);

        when(postRepository.findAllByOrderByCreatedAtDesc()).thenReturn(posts);

        PostResponse postResponse1 = PostResponse.builder().id(1L).title("Test Post").build();
        PostResponse postResponse2 = PostResponse.builder().id(2L).title("Second Post").build();

        when(blogMapper.toPostResponse(post)).thenReturn(postResponse1);
        when(blogMapper.toPostResponse(post2)).thenReturn(postResponse2);

        // When
        List<PostResponse> results = postService.getAllPosts();

        // Then
        assertEquals(2, results.size());
        assertEquals("Second Post", results.get(0).getTitle()); // Most recent first
        assertEquals("Test Post", results.get(1).getTitle());
    }

    @Test
    void getAllPosts_ShouldReturnEmptyList_WhenNoPosts() {
        // Given
        when(postRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

        // When
        List<PostResponse> results = postService.getAllPosts();

        // Then
        assertEquals(0, results.size());
    }

    @Test
    void updatePost_ShouldUpdateAndReturnPost_WhenValidOwner() {
        // Given
        Long postId = 1L;
        UpdatePostRequest request = new UpdatePostRequest();
        request.setTitle("Updated Title");
        request.setContent("Updated content");
        request.setPublished(true);

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post p = invocation.getArgument(0);
            p.setUpdatedAt(Instant.now());
            return p;
        });

        PostResponse updatedResponse = PostResponse.builder()
                .id(postId)
                .title("Updated Title")
                .content("Updated content")
                .published(true)
                .build();
        when(blogMapper.toPostResponse(any(Post.class))).thenReturn(updatedResponse);

        // When
        PostResponse result = postService.updatePost(postId, request, author.getId());

        // Then
        assertNotNull(result);
        assertEquals("Updated Title", result.getTitle());
        assertEquals("Updated content", result.getContent());
        assertTrue(result.isPublished());
    }

    @Test
    void updatePost_ShouldThrowResourceNotFoundException_WhenPostNotFound() {
        // Given
        Long postId = 999L;
        UpdatePostRequest request = new UpdatePostRequest();
        request.setTitle("Updated");
        request.setContent("Updated content");

        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> postService.updatePost(postId, request, author.getId()));
    }

    @Test
    void updatePost_ShouldThrowAccessDeniedException_WhenUserIsNotAuthor() {
        // Given
        Long postId = 1L;
        Long differentUserId = 2L;

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        UpdatePostRequest request = new UpdatePostRequest();
        request.setTitle("Updated Title");
        request.setContent("Updated content");

        // When & Then
        assertThrows(org.springframework.security.AccessDeniedException.class,
                () -> postService.updatePost(postId, request, differentUserId));
        verify(postRepository, never()).save(any());
    }

    @Test
    void deletePost_ShouldDeletePost_WhenValidOwner() {
        // Given
        Long postId = 1L;
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        doNothing().when(postRepository).delete(post);

        // When
        postService.deletePost(postId, author.getId());

        // Then
        verify(postRepository).delete(post);
    }

    @Test
    void deletePost_ShouldThrowResourceNotFoundException_WhenPostNotFound() {
        // Given
        Long postId = 999L;
        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> postService.deletePost(postId, author.getId()));
        verify(postRepository, never()).delete(any());
    }

    @Test
    void deletePost_ShouldThrowAccessDeniedException_WhenUserIsNotAuthor() {
        // Given
        Long postId = 1L;
        Long differentUserId = 2L;

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        // When & Then
        assertThrows(org.springframework.security.AccessDeniedException.class,
                () -> postService.deletePost(postId, differentUserId));
        verify(postRepository, never()).delete(any());
    }
}
