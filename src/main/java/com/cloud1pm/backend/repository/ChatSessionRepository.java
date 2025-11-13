// src/main/java/com/cloud1pm/backend/repository/ChatSessionRepository.java
package com.cloud1pm.backend.repository;

import com.cloud1pm.backend.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    // 사용자의 모든 채팅 세션을 최근 업데이트 순으로 조회
    List<ChatSession> findAllByUserIdOrderByUpdatedAtDesc(Long userId);
}