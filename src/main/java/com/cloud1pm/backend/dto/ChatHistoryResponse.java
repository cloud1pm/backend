package com.cloud1pm.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatHistoryResponse {
    private Long sessionId;
    private String title;
    private String sessionSentiment; // 세션 전체의 지배적 감정
    private Double averageScore;     // 세션 전체 평균 감정 점수
    private Integer averageRisk;     // 세션 전체 평균 위험도
    private List<ChatMessageResponse> messages; // 기존 메시지 리스트
}