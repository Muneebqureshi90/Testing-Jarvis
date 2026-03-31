package com.example.blog.service.impl;

import com.example.blog.exception.ConflictException;
import com.example.blog.model.dto.*;
import com.example.blog.model.entity.User;
import com.example.blog.repository.UserRepository;
import com.example.blog.security.JwtUtil;
import com.example.blog.service.AuthService;
import com.example.blog.service.mapper.BlogMapper;
import com.example.blog.util.PasswordEncoderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final BlogMapper blogMapper;

    public AuthServiceImpl(UserRepository userRepository, JwtUtil jwtUtil, BlogMapper blogMapper) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.blogMapper = blogMapper;
    }

    @Override
    @Transactional
    public UserResponse register(RegisterUserRequest request) {
        // Check for duplicate username
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username already exists");
        }

        // Check for duplicate email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already exists");
        }

        // Create user
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(PasswordEncoderUtil.hash(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .active(true)
                .verified(false)
                .build();

        User savedUser = userRepository.save(user);
        logger.info("User registered successfully: {}", savedUser.getUsername());

        return blogMapper.toUserResponse(savedUser);
    }

    @Override
    public LoginResponse authenticate(LoginRequest request) {
        Authentication authentication = org.springframework.security.authentication.UsernamePasswordAuthenticationManager
                .authenticate(request.getUsername(), request.getPassword());

        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found after authentication"));

        // Generate tokens using username and userId
        String accessToken = jwtUtil.generateAccessToken(user.getUsername(), user.getId());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername(), user.getId());

        LoginResponse response = LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.expirationMs / 1000)
                .user(blogMapper.toUserResponse(user))
                .build();

        logger.info("User logged in: {}", user.getUsername());
        return response;
    }

    @Override
    @Transactional
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        // Extract username and userId from refresh token
        String username = jwtUtil.extractUsername(refreshToken);
        Long userId = jwtUtil.extractUserId(refreshToken);

        // Validate refresh token
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!jwtUtil.validateToken(refreshToken, 
                new org.springframework.security.core.userdetails.User(
                        user.getUsername(),
                        user.getPasswordHash(),
                        new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")))) {
            throw new RuntimeException("Invalid refresh token");
        }

        // Generate new tokens
        String newAccessToken = jwtUtil.generateAccessToken(username, userId);
        String newRefreshToken = jwtUtil.generateRefreshToken(username, userId);

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.expirationMs / 1000)
                .user(blogMapper.toUserResponse(user))
                .build();
    }
}