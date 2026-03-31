package com.example.blog.repository;

import com.example.blog.model.entity.Post;
import com.example.blog.model.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class PostRepositoryTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByAuthorIdOrderByCreatedAtDesc_shouldReturnPostsOrderedDesc() {
        // Arrange
        User author = User.builder()
                .username("testuser")
                .email("test@example.com")
                .passwordHash("hash")
                .build();
        userRepository.save(author);

        Post post1 = Post.builder()
                .title("Older Post")
                .content("Older content")
                .author(author)
                .published(true)
                .createdAt(Instant.now().minusSeconds(3600))
                .build();

        Post post2 = Post.builder()
                .title("Newer Post")
                .content("Newer content")
                .author(author)
                .published(true)
                .createdAt(Instant.now())
                .build();

        postRepository.save(post1);
        postRepository.save(post2);

        // Act
        List<Post> results = postRepository.findByAuthorIdOrderByCreatedAtDesc(author.getId());

        // Assert
        assertEquals(2, results.size());
        assertEquals("Newer Post", results.get(0).getTitle());
        assertEquals("Older Post", results.get(1).getTitle());
    }

    @Test
    void findAllByOrderByCreatedAtDesc_shouldReturnAllPostsOrderedDesc() {
        // Arrange
        User author = User.builder()
                .username("testuser")
                .email("test@example.com")
                .passwordHash("hash")
                .build();
        userRepository.save(author);

        Post post1 = Post.builder()
                .title("Post A")
                .content("Content A")
                .author(author)
                .published(true)
                .createdAt(Instant.now().minusSeconds(100))
                .build();

        Post post2 = Post.builder()
                .title("Post B")
                .content("Content B")
                .author(author)
                .published(false)
                .createdAt(Instant.now().minusSeconds(50))
                .build();

        postRepository.save(post1);
        postRepository.save(post2);

        // Act
        List<Post> results = postRepository.findAllByOrderByCreatedAtDesc();

        // Assert
        assertEquals(2, results.size());
        assertEquals("Post B", results.get(0).getTitle());
        assertEquals("Post A", results.get(1).getTitle());
    }

    @Test
    void findById_shouldReturnPost_whenExists() {
        // Arrange
        User author = User.builder()
                .username("testuser")
                .email("test@example.com")
                .passwordHash("hash")
                .build();
        userRepository.save(author);

        Post post = Post.builder()
                .title("Test Post")
                .content("Test content")
                .author(author)
                .published(true)
                .build();
        postRepository.save(post);

        // Act
        Optional<Post> found = postRepository.findById(post.getId());

        // Assert
        assertTrue(found.isPresent());
        assertEquals("Test Post", found.get().getTitle());
        assertEquals(author.getId(), found.get().getAuthor().getId());
    }

    @Test
    void findById_shouldReturnEmpty_whenNotExists() {
        // Act
        Optional<Post> found = postRepository.findById(9999L);

        // Assert
        assertTrue(found.isEmpty());
    }

    @Test
    void delete_shouldRemovePost() {
        // Arrange
        User author = User.builder()
                .username("testuser")
                .email("test@example.com")
                .passwordHash("hash")
                .build();
        userRepository.save(author);

        Post post = Post.builder()
                .title("To Delete")
                .content("Delete me")
                .author(author)
                .build();
        postRepository.save(post);

        Long postId = post.getId();
        assertTrue(postRepository.findById(postId).isPresent());

        // Act
        postRepository.delete(post);

        // Assert
        assertTrue(postRepository.findById(postId).isEmpty());
    }
}
