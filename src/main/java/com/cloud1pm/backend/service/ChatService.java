// backend/src/main/java/com/cloud1pm/backend/service/ChatService.java
package com.cloud1pm.backend.service;

import com.cloud1pm.backend.dto.ChatRequest;
import com.cloud1pm.backend.dto.ChatResponse;
import com.cloud1pm.backend.dto.EmotionTrendResponse;
import com.cloud1pm.backend.entity.ChatMessage;
import com.cloud1pm.backend.entity.RiskSolution; // 추가
import com.cloud1pm.backend.entity.User;
import com.cloud1pm.backend.repository.ChatMessageRepository;
import com.cloud1pm.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 추가
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j // 추가
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    // private final SentimentAnalysisService sentimentAnalysisService; // 기존 키워드 기반 서비스 제거
    private final RiskAnalysisService riskAnalysisService;
    private final GeminiService geminiService; // [수정] GeminiService 주입
    private final UserService userService; // [수정] UserService 주입 (위험도별 해결방안을 가져오기 위해)

    @Transactional
    public ChatResponse sendMessage(Long userId, ChatRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 1. Gemini를 사용하여 챗봇 응답 및 감정 분석
        Map<String, Object> geminiResult = geminiService.generateChatResponseAndAnalyzeSentiment(request.getMessage());
        String botResponseFromGemini = (String) geminiResult.get("botResponse");
        String sentiment = (String) geminiResult.get("sentiment");
        Double sentimentScore = (Double) geminiResult.get("score");

        // 사용자 메시지 저장
        ChatMessage userMessage = ChatMessage.builder()
                .user(user)
                .message(request.getMessage())
                .isUserMessage(true)
                .sentiment(sentiment) // [수정] Gemini 분석 결과 저장
                .sentimentScore(sentimentScore) // [수정] Gemini 분석 결과 저장
                .build();
        chatMessageRepository.save(userMessage);

        // 2. 위험도 계산 (기존 로직 유지)
        int riskLevel = riskAnalysisService.calculateRiskLevel(userId);

        // 3. 챗봇 응답 생성 (위험도 기반 추천 로직 추가)
        // Gemini 응답에 사용자가 설정한 위험도별 해결 방안을 추가합니다.
        String finalBotResponse = botResponseFromGemini + getRiskRecommendation(userId, riskLevel);

        ChatMessage botMessage = ChatMessage.builder()
                .user(user)
                .message(finalBotResponse)
                .isUserMessage(false)
                .build();
        chatMessageRepository.save(botMessage);

        return ChatResponse.builder()
                .message(finalBotResponse)
                .sentiment(sentiment)
                .riskLevel(riskLevel)
                .build();
    }

    /**
     * [추가] 위험도 기반으로 사용자가 설정한 해결 방안을 가져와 챗봇 응답에 추가합니다.
     */
    private String getRiskRecommendation(Long userId, int riskLevel) {
        // 위험도 척도(1-10)와 정확히 일치하는 해결 방안을 찾습니다.
        List<RiskSolution> solutions = userService.getRiskSolutions(userId, riskLevel);

        if (!solutions.isEmpty()) {
            // 해당 위험도에 대한 사용자가 설정한 해결 방안을 응답에 추가합니다.
            String recommendation = solutions.get(0).getSolution(); // 첫 번째 솔루션 사용
            return "\n\n" +
                    "---" + "\n" +
                    "⚠️ **위험도 알림: " + riskLevel + "/10**\n" +
                    "이 정도의 힘든 상황에서 당신이 스스로 설정한 해결 방안은: \n" +
                    "**\"" + recommendation + "\"**\n" +
                    "작은 것부터 시도해 보는 건 어떨까요? 힘내요!";
        }

        // 위험도가 높지만 설정된 솔루션이 없을 경우 일반적인 안내
        if (riskLevel >= 8) {
            return "\n\n" +
                    "---" + "\n" +
                    "⚠️ **위험도 알림: " + riskLevel + "/10**\n" +
                    "지금 많이 힘드시군요. 전문가의 도움이 필요하다고 느낄 때는 언제든 상담 센터에 연락해 보세요. 제가 옆에서 응원할게요.";
        }

        return ""; // 평소에는 추가 추천 메시지 없음
    }

    // [삭제] 기존의 하드코딩된 generateBotResponse는 GeminiService로 대체되어 사용하지 않습니다.

    @Transactional(readOnly = true)
    public EmotionTrendResponse getEmotionTrend(Long userId, int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        List<ChatMessage> messages = chatMessageRepository.findRecentMessages(userId, startDate);

        Map<LocalDate, List<ChatMessage>> groupedByDate = messages.stream()
                .filter(m -> m.getIsUserMessage() && m.getSentiment() != null)
                .collect(Collectors.groupingBy(m -> m.getCreatedAt().toLocalDate()));

        List<EmotionTrendResponse.DailyEmotion> dailyEmotions = groupedByDate.entrySet().stream()
                .map(entry -> {
                    double avgScore = entry.getValue().stream()
                            .mapToDouble(ChatMessage::getSentimentScore)
                            .average()
                            .orElse(0.0);

                    String dominantSentiment = entry.getValue().stream()
                            .collect(Collectors.groupingBy(ChatMessage::getSentiment, Collectors.counting()))
                            .entrySet().stream()
                            .max(Map.Entry.comparingByValue())
                            .map(Map.Entry::getKey)
                            .orElse("neutral");

                    return new EmotionTrendResponse.DailyEmotion(
                            entry.getKey(), dominantSentiment, avgScore);
                })
                .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
                .collect(Collectors.toList());

        return new EmotionTrendResponse(dailyEmotions);
    }
}