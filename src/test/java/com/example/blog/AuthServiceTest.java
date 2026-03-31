package com.example.blog;

import com.example.blog.exception.ConflictException;
import com.example.blog.model.dto.LoginRequest;
import com.example.blog.model.dto.LoginResponse;
import com.example.blog.model.dto.RegisterUserRequest;
import com.example.blog.model.dto.UserResponse;
import com.example.blog.model.entity.User;
import com.example.blog.repository.UserRepository;
import com.example.blog.security.JwtUtil;
import com.example.blog.service.AuthService;
import com.example.blog.service.impl.AuthServiceImpl;
import com.example.blog.service.mapper.BlogMapper;
import com.example.blog.util.PasswordEncoderUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private BlogMapper blogMapper;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthServiceImpl authService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    private final String username = "testuser";
    private final String email = "test@example.com";
    private final String password = "password123";
    private final String encodedPassword = "$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj12NHe1zhCO";

    @Test
    void register_ShouldSaveUser_WhenUsernameAndEmailAreUnique() {
        // Given
        RegisterUserRequest request = new RegisterUserRequest();
        request.setUsername(username);
        request.setEmail(email);
        request.setPassword(password);
        request.setFirstName("Test");
        request.setLastName("User");
        request.setPhone("+1234567890");

        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            user.setUuid("123e4567-e89b-12d3-a456-426614174000");
            user.setCreatedAt(Instant.now());
            user.setUpdatedAt(Instant.now());
            return user;
        });

        UserResponse expectedResponse = UserResponse.builder()
                .id(1L)
                .uuid("123e4567-e89b-12d3-a456-426614174000")
                .username(username)
                .email(email)
                .firstName("Test")
                .lastName("User")
                .phone("+1234567890")
                .isActive(true)
                .isVerified(false)
                .createdAt(Instant.now())
                .build();

        when(blogMapper.toUserResponse(any(User.class))).thenReturn(expectedResponse);

        // When
        UserResponse result = authService.register(request);

        // Then
        assertNotNull(result);
        assertEquals(username, result.getUsername());
        assertEquals(email, result.getEmail());

        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals(username, savedUser.getUsername());
        assertEquals(email, savedUser.getEmail());
        assertTrue(PasswordEncoderUtil.matches(password, savedUser.getPasswordHash()));
    }

    @Test
    void register_ShouldThrowConflictException_WhenUsernameExists() {
        // Given
        RegisterUserRequest request = new RegisterUserRequest();
        request.setUsername(username);
        request.setEmail(email);
        request.setPassword(password);
        request.setFirstName("Test");
        request.setLastName("User");

        when(userRepository.existsByUsername(username)).thenReturn(true);

        // When & Then
        assertThrows(ConflictException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_ShouldThrowConflictException_WhenEmailExists() {
        // Given
        RegisterUserRequest request = new RegisterUserRequest();
        request.setUsername(username);
        request.setEmail(email);
        request.setPassword(password);
        request.setFirstName("Test");
        request.setLastName("User");

        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(userRepository.existsByEmail(email)).thenReturn(true);

        // When & Then
        assertThrows(ConflictException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void authenticate_ShouldReturnLoginResponse_WhenCredentialsValid() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);

        Authentication authentication = mock(Authentication.class);
        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                username, encodedPassword, List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))
        );

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        User user = User.builder()
                .id(1L)
                .username(username)
                .email(email)
                .passwordHash(encodedPassword)
                .firstName("Test")
                .lastName("User")
                .active(true)
                .build();
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        when(jwtUtil.generateAccessToken(any(Authentication.class))).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(any(Authentication.class))).thenReturn("refresh-token");
        when(jwtUtil.getExpirationMs()).thenReturn(86400000L);

        UserResponse userResponse = UserResponse.builder()
                .id(1L)
                .username(username)
                .email(email)
                .build();
        when(blogMapper.toUserResponse(any(User.class))).thenReturn(userResponse);

        // When
        LoginResponse result = authService.authenticate(request);

        // Then
        assertNotNull(result);
        assertEquals("access-token", result.getAccessToken());
        assertEquals("refresh-token", result.getRefreshToken());
        assertEquals("Bearer", result.getTokenType());
        assertEquals(86400L, result.getExpiresIn());
        assertNotNull(result.getUser());
        assertEquals(username, result.getUser().getUsername());
    }

    @Test
    void authenticate_ShouldThrowException_WhenUserNotFoundAfterAuth() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);

        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> authService.authenticate(request));
    }

    @Test
    void refreshToken_ShouldReturnNewTokens_WhenValidToken() {
        // Given
        String refreshToken = "valid-refresh-token";
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(refreshToken);

        String username = "testuser";
        Long userId = 1L;

        when(jwtUtil.extractUsername(refreshToken)).thenReturn(username);
        when(jwtUtil.extractUserId(refreshToken)).thenReturn(userId);

        User user = User.builder()
                .id(userId)
                .username(username)
                .email(email)
                .passwordHash(encodedPassword)
                .active(true)
                .build();
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                username, encodedPassword, List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))
        );
        when(jwtUtil.validateToken(eq(refreshToken), any(UserDetails.class))).thenReturn(true);

        when(jwtUtil.generateAccessToken(anyString(), anyLong())).thenReturn("new-access-token");
        when(jwtUtil.generateRefreshToken(anyString(), anyLong())).thenReturn("new-refresh-token");
        when(jwtUtil.getExpirationMs()).thenReturn(86400000L);

        UserResponse userResponse = UserResponse.builder()
                .id(userId)
                .username(username)
                .email(email)
                .build();
        when(blogMapper.toUserResponse(any(User.class))).thenReturn(userResponse);

        // When
        LoginResponse result = authService.refreshToken(request);

        // Then
        assertNotNull(result);
        assertEquals("new-access-token", result.getAccessToken());
        assertEquals("new-refresh-token", result.getRefreshToken());
        assertEquals("Bearer", result.getTokenType());
        assertEquals(86400L, result.getExpiresIn());
    }

    @Test
    void refreshToken_ShouldThrowException_WhenInvalidToken() {
        // Given
        String refreshToken = "invalid-token";
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(refreshToken);

        when(jwtUtil.extractUsername(refreshToken)).thenThrow(new io.jsonwebtoken.MalformedJwtException("Invalid token"));

        // When & Then
        assertThrows(RuntimeException.class, () -> authService.refreshToken(request));
    }
}
