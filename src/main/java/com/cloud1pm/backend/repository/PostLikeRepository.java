// === PostLikeRepository.java ===
package com.cloud1pm.backend.repository;

import com.cloud1pm.backend.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    Optional<PostLike> findByPostIdAndUserId(Long postId, Long userId);
    boolean existsByPostIdAndUserId(Long postId, Long userId);

    @Query("SELECT COUNT(pl) FROM PostLike pl WHERE pl.user.id = :userId AND pl.createdAt >= :date")
    Long countTodayLikesByUser(@Param("userId") Long userId, @Param("date") LocalDateTime date);
}