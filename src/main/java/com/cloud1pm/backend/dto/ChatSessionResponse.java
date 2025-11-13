// src/main/java/com/cloud1pm/backend/dto/ChatSessionResponse.java
package com.cloud1pm.backend.dto;

import com.cloud1pm.backend.entity.ChatSession;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 채팅 목록 조회 응답 DTO
@Getter
@Builder
public class ChatSessionResponse {
    private Long sessionId;
    private String title;
    private LocalDateTime updatedAt;

    public static ChatSessionResponse fromEntity(ChatSession session) {
        return ChatSessionResponse.builder()
                .sessionId(session.getId())
                .title(session.getTitle())
                .updatedAt(session.getUpdatedAt())
                .build();
    }
}