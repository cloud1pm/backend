// src/main/java/com/cloud1pm/backend/dto/ChatMessageResponse.java
package com.cloud1pm.backend.dto;

import com.cloud1pm.backend.entity.ChatMessage;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 채팅 기록 조회 응답 DTO
@Getter
@Builder
public class ChatMessageResponse {
    private Long messageId;
    private String message;
    private Boolean isUserMessage;
    private String sentiment;
    private LocalDateTime createdAt;

    public static ChatMessageResponse fromEntity(ChatMessage message) {
        return ChatMessageResponse.builder()
                .messageId(message.getId())
                .message(message.getMessage())
                .isUserMessage(message.getIsUserMessage())
                .sentiment(message.getSentiment())
                .createdAt(message.getCreatedAt())
                .build();
    }
}