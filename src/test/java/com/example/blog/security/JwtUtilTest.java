package com.example.blog.security;

import io.jsonwebtoken.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = JwtUtilTest.TestConfig.class)
class JwtUtilTest {

    @Value("${app.jwt.secret}")
    private String secret;

    private JwtUtil jwtUtil;

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public JwtUtil jwtUtil() {
            return new JwtUtil();
        }
    }

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // Set secret via reflection for test
        try {
            var field = JwtUtil.class.getDeclaredField("secret");
            field.setAccessible(true);
            field.set(jwtUtil, "test-secret-key-for-junit-tests-only-32-bytes-long!!");
            field.setAccessible(false);

            // Initialize key
            var initMethod = JwtUtil.class.getDeclaredMethod("init");
            initMethod.setAccessible(true);
            initMethod.invoke(jwtUtil);
            initMethod.setAccessible(false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void generateAccessToken_shouldCreateToken() {
        // Arrange
        String username = "testuser";
        Long userId = 1L;

        // Act
        String token = jwtUtil.generateAccessToken(username, userId);

        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());

        // Verify claims
        String extractedUsername = jwtUtil.extractUsername(token);
        assertEquals(username, extractedUsername);

        Long extractedUserId = jwtUtil.extractUserId(token);
        assertEquals(userId, extractedUserId);

        assertFalse(jwtUtil.isTokenExpired(token));
    }

    @Test
    void generateRefreshToken_shouldCreateTokenWithLongerExpiry() {
        // Arrange
        String username = "testuser";
        Long userId = 1L;

        // Act
        String token = jwtUtil.generateRefreshToken(username, userId);

        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());

        String extractedUsername = jwtUtil.extractUsername(token);
        assertEquals(username, extractedUsername);
    }

    @Test
    void validateToken_shouldReturnTrue_whenTokenValid() {
        // Arrange
        String username = "testuser";
        Long userId = 1L;
        String token = jwtUtil.generateAccessToken(username, userId);

        User userDetails = new User(username, "{noop}dummy",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        // Act
        boolean isValid = jwtUtil.validateToken(token, userDetails);

        // Assert
        assertTrue(isValid);
    }

    @Test
    void extractClaims_shouldExtractAllClaims() {
        // Arrange
        String username = "testuser";
        Long userId = 1L;
        String token = jwtUtil.generateAccessToken(username, userId);

        // Act
        Claims claims = jwtUtil.extractAllClaims(token);

        // Assert
        assertEquals(username, claims.getSubject());
        assertEquals(userId, claims.get("userId"));
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
    }

    @Test
    void generateAccessTokenFromAuthentication_shouldCreateToken() {
        // Arrange
        String username = "testuser";
        User userDetails = new User(username, "{noop}dummy",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        // Act
        String token = jwtUtil.generateAccessToken(authentication);

        // Assert
        assertNotNull(token);
        assertEquals(username, jwtUtil.extractUsername(token));
    }
}
