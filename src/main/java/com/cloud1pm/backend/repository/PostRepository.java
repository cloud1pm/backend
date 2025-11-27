// === PostRepository.java ===
package com.cloud1pm.backend.repository;

import com.cloud1pm.backend.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findAllByUserIdOrderByCreatedAtDesc(Long userId);
    List<Post> findAllByOrderByCreatedAtDesc();
    @Query("SELECT COUNT(p) FROM Post p WHERE p.user.id = :userId AND p.createdAt >= :date")
    Long countTodayPostsByUser(@Param("userId") Long userId, @Param("date") LocalDateTime date);
}