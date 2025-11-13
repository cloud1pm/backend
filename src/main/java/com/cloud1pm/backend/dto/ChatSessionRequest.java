// src/main/java/com/cloud1pm/backend/dto/ChatSessionRequest.java
package com.cloud1pm.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 채팅 명 설정/수정 요청 DTO
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessionRequest {
    private String title;
}