package com.example.blog.integration;

import com.example.blog.model.entity.Post;
import com.example.blog.model.entity.User;
import com.example.blog.repository.PostRepository;
import com.example.blog.repository.UserRepository;
import com.example.blog.service.PostService;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NPlusOneQueryTest {

    @Autowired
    private PostService postService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void getAllPosts_shouldNotCauseNPlusOneQueries() {
        // Arrange: create multiple posts with different authors
        userRepository.deleteAll();
        postRepository.deleteAll();

        for (int i = 1; i <= 5; i++) {
            User user = User.builder()
                    .username("user" + i)
                    .email("user" + i + "@example.com")
                    .passwordHash("hash")
                    .firstName("User" + i)
                    .lastName("Test")
                    .active(true)
                    .build();
            userRepository.save(user);

            Post post = Post.builder()
                    .title("Post " + i)
                    .content("Content " + i)
                    .author(user)
                    .published(true)
                    .build();
            postRepository.save(post);
        }

        // Clear persistence context to ensure fresh queries
        entityManager.flush();
        entityManager.clear();

        // Enable statistics
        Session session = entityManager.unwrap(Session.class);
        Statistics stats = session.getSessionFactory().getStatistics();
        stats.clear();

        // Act
        List<PostResponseMock> posts = postService.getAllPosts();

        // Assert: verify number of queries
        int queryCount = stats.getQueryExecutionCount();
        System.out.println("Queries executed: " + queryCount);
        System.out.println("Query plan: " + stats.getQueries());

        // We expect find all posts to be done in 1 query (not N+1)
        // However, toPostResponse mapping may trigger additional queries for author if lazy loaded
        // For a simple list without includes, we should have minimal queries
        assertTrue(queryCount <= 2, "Expected 1-2 queries but got " + queryCount + " - possible N+1 issue");
    }

    // Simple DTO mock for testing
    static class PostResponseMock {
        Long id;
        String title;
        String authorName;

        PostResponseMock(Post post) {
            this.id = post.getId();
            this.title = post.getTitle();
            this.authorName = post.getAuthor().getUsername(); // This triggers lazy load
        }
    }
}
