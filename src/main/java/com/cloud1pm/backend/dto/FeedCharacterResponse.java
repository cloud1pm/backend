// src/main/java/com/cloud1pm/backend/dto/FeedCharacterResponse.java
package com.cloud1pm.backend.dto;

import lombok.Builder;
import lombok.Getter;

// 캐릭터 밥 주기 응답 DTO
@Getter
@Builder
public class FeedCharacterResponse {
    private int newRiceCount;
    private int newLevel;
    private String message; // 사용자에게 보여줄 메시지 (예: 밥 부족, 레벨업 등)
}