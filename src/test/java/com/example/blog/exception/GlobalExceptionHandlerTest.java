package com.example.blog.exception;

import com.example.blog.model.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final ObjectMapperTestHelper mapper = new ObjectMapperTestHelper();

    @Test
    void handleResourceNotFound_shouldReturn404() {
        // Arrange
        ResourceNotFoundException ex = new ResourceNotFoundException("User not found");
        WebRequest request = mockWebRequest("/api/v1/users/1");

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFound(ex, request);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(HttpStatus.NOT_FOUND.value(), body.getStatus());
        assertEquals("Not Found", body.getError());
        assertEquals("User not found", body.getMessage());
        assertEquals("/api/v1/users/1", body.getPath());
    }

    @Test
    void handleConflict_shouldReturn409() {
        // Arrange
        ConflictException ex = new ConflictException("Email already exists");
        WebRequest request = mockWebRequest("/api/v1/auth/register");

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleConflict(ex, request);

        // Assert
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(HttpStatus.CONFLICT.value(), body.getStatus());
        assertEquals("Conflict", body.getError());
        assertEquals("Email already exists", body.getMessage());
    }

    @Test
    void handleAccessDenied_shouldReturn403() {
        // Arrange
        org.springframework.security.access.AccessDeniedException ex =
                new org.springframework.security.access.AccessDeniedException("Access denied");
        WebRequest request = mockWebRequest("/api/v1/admin");

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleAccessDenied(ex, request);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(HttpStatus.FORBIDDEN.value(), body.getStatus());
        assertEquals("Forbidden", body.getError());
        assertEquals("You do not have permission to perform this action", body.getMessage());
    }

    @Test
    void handleBadCredentials_shouldReturn401() {
        // Arrange
        org.springframework.security.authentication.BadCredentialsException ex =
                new org.springframework.security.authentication.BadCredentialsException("Bad credentials");
        WebRequest request = mockWebRequest("/api/v1/auth/login");

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleBadCredentials(ex, request);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(HttpStatus.UNAUTHORIZED.value(), body.getStatus());
        assertEquals("Unauthorized", body.getError());
        assertEquals("Invalid username or password", body.getMessage());
    }

    @Test
    void handleGenericException_shouldReturn500() {
        // Arrange
        Exception ex = new RuntimeException("Unexpected error");
        WebRequest request = mockWebRequest("/api/v1/test");

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleGenericException(ex, request);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), body.getStatus());
        assertEquals("Internal Server Error", body.getError());
        assertEquals("An unexpected error occurred", body.getMessage());
    }

    private WebRequest mockWebRequest(String path) {
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        when(servletRequest.getRequestURI()).thenReturn(path);
        return new ServletWebRequest(servletRequest);
    }

    // Helper to inline ObjectMapper for sparse usage
    static class ObjectMapperTestHelper {
        private final com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

        String toJson(Object obj) throws Exception {
            return mapper.writeValueAsString(obj);
        }
    }
}
