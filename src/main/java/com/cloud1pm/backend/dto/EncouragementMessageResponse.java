// src/main/java/com/cloud1pm/backend/dto/EncouragementMessageResponse.java 파일 생성

package com.cloud1pm.backend.dto;

import com.cloud1pm.backend.entity.EncouragementMessage;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class EncouragementMessageResponse {
    private Long id;
    private String message;
    private String emotion;
    private LocalDate date;
    private LocalDateTime createdAt;

    // User 엔티티 대신 필요한 사용자 정보만 포함
    private Long userId;
    private String nickname;
    private String profileImageUrl;

    public static EncouragementMessageResponse from(EncouragementMessage message) {
        // user 필드에 접근하여 프록시를 초기화합니다. (트랜잭션 내부에서 안전하게 실행됨)
        return EncouragementMessageResponse.builder()
                .id(message.getId())
                .message(message.getMessage())
                .emotion(message.getEmotion())
                .date(message.getDate())
                .createdAt(message.getCreatedAt())
                .userId(message.getUser().getId()) // user 필드 접근
                .nickname(message.getUser().getNickname()) // user 필드 접근
                .profileImageUrl(message.getUser().getProfileImageUrl()) // user 필드 접근
                .build();
    }
}