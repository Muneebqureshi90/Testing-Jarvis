package com.example.blog.service.mapper;

import com.example.blog.model.dto.AuthorInfo;
import com.example.blog.model.dto.PostResponse;
import com.example.blog.model.dto.UserResponse;
import com.example.blog.model.entity.Post;
import com.example.blog.model.entity.User;
import org.springframework.stereotype.Component;

@Component
public class BlogMapper {

    public UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .uuid(user.getUuid())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .isActive(user.isActive())
                .isVerified(user.isVerified())
                .createdAt(user.getCreatedAt())
                .build();
    }

    public AuthorInfo toAuthorInfo(User user) {
        return AuthorInfo.builder()
                .id(user.getId())
                .uuid(user.getUuid())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build();
    }

    public PostResponse toPostResponse(Post post) {
        return PostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .author(toAuthorInfo(post.getAuthor()))
                .published(post.isPublished())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}