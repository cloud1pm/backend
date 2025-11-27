package com.cloud1pm.backend.repository;

import com.cloud1pm.backend.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findAllBySessionIdOrderByCreatedAtAsc(Long sessionId);

    @Query("SELECT c FROM ChatMessage c WHERE c.userId = :userId " +
            "AND c.createdAt >= :startDate ORDER BY c.createdAt DESC")
    List<ChatMessage> findRecentMessages(@Param("userId") Long userId,
                                         @Param("startDate") LocalDateTime startDate);

    @Query("SELECT c FROM ChatMessage c WHERE c.userId = :userId " +
            "AND c.isUserMessage = true AND c.sentiment IS NOT NULL " +
            "AND c.createdAt >= :startDate ORDER BY c.createdAt DESC")
    List<ChatMessage> findUserMessagesForRiskAnalysis(@Param("userId") Long userId,
                                                      @Param("startDate") LocalDateTime startDate);

    @Query("SELECT c FROM ChatMessage c " +
       "WHERE c.userId = :userId " +
       "AND c.isUserMessage = true " +
       "AND c.sentiment IS NOT NULL " +
       "AND c.createdAt >= :startDate " +
       "ORDER BY c.createdAt ASC")
                List<ChatMessage> findUserMessagesForEmotionTrend(
        @Param("userId") Long userId,
        @Param("startDate") LocalDateTime startDate);

                                                }