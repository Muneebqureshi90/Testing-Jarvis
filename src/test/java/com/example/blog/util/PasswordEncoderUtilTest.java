package com.example.blog.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordEncoderUtilTest {

    @Test
    void hash_ShouldReturnEncodedPassword_WhenGivenPlainPassword() {
        // Given
        String plainPassword = "password123";

        // When
        String encoded = PasswordEncoderUtil.hash(plainPassword);

        // Then
        assertNotNull(encoded);
        assertFalse(encoded.isEmpty());
        assertNotEquals(plainPassword, encoded);
    }

    @Test
    void matches_ShouldReturnTrue_WhenPasswordsMatch() {
        // Given
        String plainPassword = "password123";
        String encodedPassword = PasswordEncoderUtil.hash(plainPassword);

        // When
        boolean matches = PasswordEncoderUtil.matches(plainPassword, encodedPassword);

        // Then
        assertTrue(matches);
    }

    @Test
    void matches_ShouldReturnFalse_WhenPasswordsDoNotMatch() {
        // Given
        String plainPassword = "password123";
        String differentPassword = "different456";
        String encodedPassword = PasswordEncoderUtil.hash(differentPassword);

        // When
        boolean matches = PasswordEncoderUtil.matches(plainPassword, encodedPassword);

        // Then
        assertFalse(matches);
    }

    @Test
    void matches_ShouldReturnFalse_WhenPlainPasswordIsWrong() {
        // Given
        String plainPassword = "password123";
        String encodedPassword = PasswordEncoderUtil.hash(plainPassword);

        // When
        boolean matches = PasswordEncoderUtil.matches("wrongpassword", encodedPassword);

        // Then
        assertFalse(matches);
    }

    @Test
    void hash_ShouldGenerateDifferentHashes_forSamePassword() {
        // Given
        String plainPassword = "password123";

        // When
        String encoded1 = PasswordEncoderUtil.hash(plainPassword);
        String encoded2 = PasswordEncoderUtil.hash(plainPassword);

        // Then
        assertNotEquals(encoded1, encoded2); // BCrypt uses salt, so hashes differ
        assertTrue(PasswordEncoderUtil.matches(plainPassword, encoded1));
        assertTrue(PasswordEncoderUtil.matches(plainPassword, encoded2));
    }
}
