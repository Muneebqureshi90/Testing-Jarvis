package com.example.blog.security;

import com.example.blog.model.entity.User;
import com.example.blog.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = UserDetailsServiceImplTest.TestConfig.class)
class UserDetailsServiceImplTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        public UserDetailsService userDetailsService() {
            return new UserDetailsServiceImpl(null);
        }
    }

    @Autowired
    private UserDetailsService userDetailsService;

    @Test
    void loadUserByUsername_shouldReturnUserDetails_whenUserExistsAndActive() {
        // Given
        UserRepository mockRepo = mock(UserRepository.class);
        User user = User.builder()
                .username("testuser")
                .passwordHash("encodedPassword")
                .active(true)
                .build();
        when(mockRepo.findByUsername("testuser")).thenReturn(Optional.of(user));

        UserDetailsServiceImpl service = new UserDetailsServiceImpl(mockRepo);

        // When
        UserDetails userDetails = service.loadUserByUsername("testuser");

        // Then
        assertNotNull(userDetails);
        assertEquals("testuser", userDetails.getUsername());
        assertEquals("encodedPassword", userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }

    @Test
    void loadUserByUsername_shouldThrowException_whenUserNotFound() {
        // Given
        UserRepository mockRepo = mock(UserRepository.class);
        when(mockRepo.findByUsername("nonexistent")).thenReturn(Optional.empty());

        UserDetailsServiceImpl service = new UserDetailsServiceImpl(mockRepo);

        // When & Then
        assertThrows(org.springframework.security.core.userdetails.UsernameNotFoundException.class,
                () -> service.loadUserByUsername("nonexistent"));
    }

    @Test
    void loadUserByUsername_shouldThrowException_whenUserInactive() {
        // Given
        UserRepository mockRepo = mock(UserRepository.class);
        User user = User.builder()
                .username("inactiveuser")
                .passwordHash("encodedPassword")
                .active(false)
                .build();
        when(mockRepo.findByUsername("inactiveuser")).thenReturn(Optional.of(user));

        UserDetailsServiceImpl service = new UserDetailsServiceImpl(mockRepo);

        // When & Then
        assertThrows(org.springframework.security.core.userdetails.UsernameNotFoundException.class,
                () -> service.loadUserByUsername("inactiveuser"));
    }
}
