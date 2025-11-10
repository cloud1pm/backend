package com.cloud1pm.backend.controller;

import com.cloud1pm.backend.dto.*;
import com.cloud1pm.backend.service.CommunityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map; // 좋아요 개수 가져오기 응답을 위해 추가

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

    // 게시물 수정 (추가됨)
    @PutMapping("/posts/{postId}")
    public ResponseEntity<PostResponse> updatePost(
            @PathVariable Long postId,
            Authentication authentication,
            @RequestBody CreatePostRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        PostResponse response = communityService.updatePost(postId, userId, request);
        return ResponseEntity.ok(response);
    }

    // 게시물 삭제 (추가됨)
    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long postId,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        communityService.deletePost(postId, userId);
        return ResponseEntity.ok().build();
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

    // 댓글 삭제 (추가됨)
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        communityService.deleteComment(commentId, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/posts/{postId}/like")
    public ResponseEntity<Void> toggleLike(
            @PathVariable Long postId,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        communityService.toggleLike(postId, userId);
        return ResponseEntity.ok().build();
    }

    // 좋아요 개수 가져오기 (추가됨)
    @GetMapping("/posts/{postId}/likes/count")
    public ResponseEntity<Map<String, Integer>> getLikeCount(@PathVariable Long postId) {
        int count = communityService.getLikeCount(postId);
        return ResponseEntity.ok(Map.of("likeCount", count));
    }
}