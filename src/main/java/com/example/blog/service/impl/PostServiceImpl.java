package com.example.blog.service.impl;

import com.example.blog.exception.ResourceNotFoundException;
import com.example.blog.model.dto.*;
import com.example.blog.model.entity.Post;
import com.example.blog.model.entity.User;
import com.example.blog.repository.PostRepository;
import com.example.blog.repository.UserRepository;
import com.example.blog.service.PostService;
import com.example.blog.service.mapper.BlogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PostServiceImpl implements PostService {

    private static final Logger logger = LoggerFactory.getLogger(PostServiceImpl.class);

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final BlogMapper blogMapper;

    public PostServiceImpl(PostRepository postRepository, UserRepository userRepository, BlogMapper blogMapper) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.blogMapper = blogMapper;
    }

    @Override
    @Transactional
    public PostResponse createPost(CreatePostRequest request, Long userId) {
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (!author.isActive()) {
            throw new ResourceNotFoundException("User account is deactivated");
        }

        Post post = Post.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .author(author)
                .published(request.getPublished() != null ? request.getPublished() : false)
                .build();

        Post savedPost = postRepository.save(post);
        logger.info("Post created: id={}, title={}, author={}", savedPost.getId(), savedPost.getTitle(), author.getUsername());

        return blogMapper.toPostResponse(savedPost);
    }

    @Override
    @Transactional(readOnly = true)
    public PostResponse getPostById(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id));
        return blogMapper.toPostResponse(post);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostResponse> getAllPosts() {
        return postRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(blogMapper::toPostResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostResponse> getAllPosts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return postRepository.findAllByOrderByCreatedAtDesc(pageable).map(blogMapper::toPostResponse);
    }

    @Override
    @Transactional
    public PostResponse updatePost(Long id, UpdatePostRequest request, Long userId) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id));

        // Check ownership
        if (!post.getAuthor().getId().equals(userId)) {
            throw new org.springframework.security.AccessDeniedException("You cannot edit posts you do not own");
        }

        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        if (request.getPublished() != null) {
            post.setPublished(request.getPublished());
        }

        Post updatedPost = postRepository.save(post);
        logger.info("Post updated: id={}, author={}", updatedPost.getId(), post.getAuthor().getUsername());

        return blogMapper.toPostResponse(updatedPost);
    }

    @Override
    @Transactional
    public void deletePost(Long id, Long userId) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id));

        // Check ownership
        if (!post.getAuthor().getId().equals(userId)) {
            throw new org.springframework.security.AccessDeniedException("You cannot delete posts you do not own");
        }

        postRepository.delete(post);
        logger.info("Post deleted: id={}, author={}", id, post.getAuthor().getUsername());
    }
}