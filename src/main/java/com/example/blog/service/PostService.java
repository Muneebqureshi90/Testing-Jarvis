package com.example.blog.service;

import com.example.blog.model.dto.CreatePostRequest;
import com.example.blog.model.dto.PostResponse;
import com.example.blog.model.dto.UpdatePostRequest;
import com.example.blog.model.entity.Post;
import org.springframework.data.domain.Page;

import java.util.List;

public interface PostService {
    PostResponse createPost(CreatePostRequest request, Long userId);
    PostResponse getPostById(Long id);
    List<PostResponse> getAllPosts();
    Page<PostResponse> getAllPosts(int page, int size);
    PostResponse updatePost(Long id, UpdatePostRequest request, Long userId);
    void deletePost(Long id, Long userId);
}