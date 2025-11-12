package com.cloud1pm.backend.service;

import com.cloud1pm.backend.entity.ChatMessage;
import com.cloud1pm.backend.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RiskAnalysisService {

    private final ChatMessageRepository chatMessageRepository;

    /**
     * 최근 대화를 분석하여 위험도 계산 (1-10)
     * 최근 20개 메시지의 부정 비율을 계산
     */
    @Transactional(readOnly = true)
    public int calculateRiskLevel(Long userId) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        List<ChatMessage> recentMessages = chatMessageRepository
                .findUserMessagesForRiskAnalysis(userId, startDate);

        if (recentMessages.isEmpty()) {
            return 1; // 대화 기록이 없으면 위험도 최소
        }

        // 최근 20개 메시지만 분석
        List<ChatMessage> messagesToAnalyze = recentMessages.stream()
                .limit(20)
                .toList();

        long negativeCount = messagesToAnalyze.stream()
                .filter(m -> "negative".equals(m.getSentiment()))
                .count();

        double negativeRatio = (double) negativeCount / messagesToAnalyze.size();

        // 부정 감정 점수 평균 계산
        double avgNegativeScore = messagesToAnalyze.stream()
                .filter(m -> "negative".equals(m.getSentiment()))
                .mapToDouble(ChatMessage::getSentimentScore)
                .average()
                .orElse(0.0);

        // 부정 비율과 감정 점수를 조합하여 위험도 계산
        double riskScore = (negativeRatio * 10) + (Math.abs(avgNegativeScore) * 3);

        int riskLevel = (int) Math.ceil(Math.min(10, Math.max(1, riskScore)));

        return riskLevel;
    }
}