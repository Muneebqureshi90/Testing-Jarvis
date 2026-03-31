package com.example.blog.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthorInfo {
    private Long id;
    private String uuid;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
}