package com.example.blog.repository;

import com.example.blog.model.entity.User;
import com.example.blog.model.entity.Post;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByUsername_shouldReturnUser_whenExists() {
        // Arrange
        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .passwordHash("hash")
                .firstName("Test")
                .lastName("User")
                .active(true)
                .build();
        userRepository.save(user);

        // Act
        Optional<User> found = userRepository.findByUsername("testuser");

        // Assert
        assertTrue(found.isPresent());
        assertEquals("testuser", found.get().getUsername());
        assertEquals("test@example.com", found.get().getEmail());
    }

    @Test
    void findByUsername_shouldReturnEmpty_whenNotExists() {
        // Act
        Optional<User> found = userRepository.findByUsername("nonexistent");

        // Assert
        assertTrue(found.isEmpty());
    }

    @Test
    void findByEmail_shouldReturnUser_whenExists() {
        // Arrange
        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .passwordHash("hash")
                .firstName("Test")
                .lastName("User")
                .build();
        userRepository.save(user);

        // Act
        Optional<User> found = userRepository.findByEmail("test@example.com");

        // Assert
        assertTrue(found.isPresent());
        assertEquals("testuser", found.get().getUsername());
    }

    @Test
    void findByUuid_shouldReturnUser_whenExists() {
        // Arrange
        User user = User.builder()
                .uuid("123e4567-e89b-12d3-a456-426614174000")
                .username("testuser")
                .email("test@example.com")
                .passwordHash("hash")
                .build();
        userRepository.save(user);

        // Act
        Optional<User> found = userRepository.findByUuid("123e4567-e89b-12d3-a456-426614174000");

        // Assert
        assertTrue(found.isPresent());
        assertEquals("testuser", found.get().getUsername());
    }

    @Test
    void existsByUsername_shouldReturnTrue_whenUsernameExists() {
        // Arrange
        User user = User.builder()
                .username("existinguser")
                .email("existing@example.com")
                .passwordHash("hash")
                .build();
        userRepository.save(user);

        // Act
        boolean exists = userRepository.existsByUsername("existinguser");

        // Assert
        assertTrue(exists);
    }

    @Test
    void existsByUsername_shouldReturnFalse_whenUsernameNotExists() {
        // Act
        boolean exists = userRepository.existsByUsername("nonexistent");

        // Assert
        assertFalse(exists);
    }

    @Test
    void existsByEmail_shouldReturnTrue_whenEmailExists() {
        // Arrange
        User user = User.builder()
                .username("testuser")
                .email("existing@example.com")
                .passwordHash("hash")
                .build();
        userRepository.save(user);

        // Act
        boolean exists = userRepository.existsByEmail("existing@example.com");

        // Assert
        assertTrue(exists);
    }

    @Test
    void existsByEmail_shouldReturnFalse_whenEmailNotExists() {
        // Act
        boolean exists = userRepository.existsByEmail("nonexistent@example.com");

        // Assert
        assertFalse(exists);
    }
}
