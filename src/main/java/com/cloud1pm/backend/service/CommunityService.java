package com.cloud1pm.backend.service;

import com.cloud1pm.backend.dto.*;
import com.cloud1pm.backend.entity.*;
import com.cloud1pm.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommunityService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    @Transactional
    public PostResponse createPost(Long userId, CreatePostRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Post post = Post.builder()
                .user(user)
                .title(request.getTitle())
                .content(request.getContent())
                .build();

        Post savedPost = postRepository.save(post);

        return convertToPostResponse(savedPost, userId);
    }

    @Transactional(readOnly = true)
    public List<PostResponse> getAllPosts(Long userId) {
        return postRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(post -> convertToPostResponse(post, userId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PostDetailResponse getPost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        List<CommentResponse> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId)
                .stream()
                .map(this::convertToCommentResponse)
                .collect(Collectors.toList());

        boolean isLiked = postLikeRepository.existsByPostIdAndUserId(postId, userId);

        return PostDetailResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .authorName(post.getUser().getName())
                .authorProfileImage(post.getUser().getProfileImageUrl())
                .likeCount(post.getLikeCount())
                .commentCount(comments.size())
                .isLiked(isLiked)
                .comments(comments)
                .createdAt(post.getCreatedAt())
                .build();
    }

    @Transactional
    public CommentResponse createComment(Long postId, Long userId, CreateCommentRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Comment comment = Comment.builder()
                .post(post)
                .user(user)
                .content(request.getContent())
                .build();

        Comment savedComment = commentRepository.save(comment);

        // 밥 획득
        userService.addRiceForComment(userId);

        return convertToCommentResponse(savedComment);
    }

    @Transactional
    public void toggleLike(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        postLikeRepository.findByPostIdAndUserId(postId, userId)
                .ifPresentOrElse(
                        like -> {
                            // 좋아요 취소
                            postLikeRepository.delete(like);
                            post.setLikeCount(post.getLikeCount() - 1);
                        },
                        () -> {
                            // 좋아요 추가
                            PostLike like = PostLike.builder()
                                    .post(post)
                                    .user(user)
                                    .build();
                            postLikeRepository.save(like);
                            post.setLikeCount(post.getLikeCount() + 1);

                            // 밥 획득
                            userService.addRiceForLike(userId);
                        }
                );

        postRepository.save(post);
    }

    private PostResponse convertToPostResponse(Post post, Long currentUserId) {
        boolean isLiked = postLikeRepository.existsByPostIdAndUserId(post.getId(), currentUserId);

        return PostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .authorName(post.getUser().getName())
                .authorProfileImage(post.getUser().getProfileImageUrl())
                .likeCount(post.getLikeCount())
                .commentCount(post.getComments().size())
                .isLiked(isLiked)
                .createdAt(post.getCreatedAt())
                .build();
    }

    private CommentResponse convertToCommentResponse(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .authorName(comment.getUser().getName())
                .authorProfileImage(comment.getUser().getProfileImageUrl())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}