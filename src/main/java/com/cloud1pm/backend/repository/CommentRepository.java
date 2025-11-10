// === CommentRepository.java ===
package com.cloud1pm.backend.repository;

import com.cloud1pm.backend.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPostIdOrderByCreatedAtAsc(Long postId);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.user.id = :userId AND c.createdAt >= :date")
    Long countTodayCommentsByUser(@Param("userId") Long userId, @Param("date") LocalDateTime date);
}