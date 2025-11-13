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

    // 게시물 수정 (추가됨)
    @Transactional
    public PostResponse updatePost(Long postId, Long userId, CreatePostRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        if (!post.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: Only the author can update the post.");
        }

        post.setTitle(request.getTitle());
        post.setContent(request.getContent());

        Post updatedPost = postRepository.save(post);
        return convertToPostResponse(updatedPost, userId);
    }

    // 게시물 삭제 (추가됨)
    @Transactional
    public void deletePost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        if (!post.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: Only the author can delete the post.");
        }

        // JPA Cascade 설정이 되어 있다고 가정하고 Post만 삭제
        postRepository.delete(post);
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

    // 댓글 삭제 (추가됨)
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        if (!comment.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: Only the author can delete the comment.");
        }

        commentRepository.delete(comment);
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

    // 좋아요 개수 가져오기 (추가됨)
    @Transactional(readOnly = true)
    public int getLikeCount(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        return post.getLikeCount();
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
                .commentId(comment.getId())
                .content(comment.getContent())
                .authorName(comment.getUser().getName())
                .authorProfileImage(comment.getUser().getProfileImageUrl())
                .createdAt(comment.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<PostResponse> getMyPosts(Long userId) {
        List<Post> posts = postRepository.findAllByUserIdOrderByCreatedAtDesc(userId);

        return posts.stream()
                .map(post -> convertToPostResponse(post, userId)) // fromEntity 대신 기존 메서드 사용
                .collect(Collectors.toList());
    }
}