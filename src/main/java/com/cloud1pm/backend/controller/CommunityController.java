package com.cloud1pm.backend.controller;

import com.cloud1pm.backend.dto.*;
import com.cloud1pm.backend.service.CommunityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;

    @PostMapping("/posts")
    public ResponseEntity<PostResponse> createPost(
            Authentication authentication,
            @RequestBody CreatePostRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        PostResponse response = communityService.createPost(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/posts")
    public ResponseEntity<List<PostResponse>> getAllPosts(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        List<PostResponse> posts = communityService.getAllPosts(userId);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/posts/{postId}")
    public ResponseEntity<PostDetailResponse> getPost(
            @PathVariable Long postId,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        PostDetailResponse response = communityService.getPost(postId, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable Long postId,
            Authentication authentication,
            @RequestBody CreateCommentRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        CommentResponse response = communityService.createComment(postId, userId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/posts/{postId}/like")
    public ResponseEntity<Void> toggleLike(
            @PathVariable Long postId,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        communityService.toggleLike(postId, userId);
        return ResponseEntity.ok().build();
    }
}