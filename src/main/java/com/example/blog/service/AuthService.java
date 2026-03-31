package com.example.blog.service;

import com.example.blog.model.dto.*;
import com.example.blog.model.entity.User;

public interface AuthService {
    UserResponse register(RegisterUserRequest request);
    LoginResponse authenticate(LoginRequest request);
    LoginResponse refreshToken(RefreshTokenRequest request);
}