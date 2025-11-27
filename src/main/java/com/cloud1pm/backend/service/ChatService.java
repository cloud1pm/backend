package com.cloud1pm.backend.service;

import com.cloud1pm.backend.dto.*;
import com.cloud1pm.backend.entity.ChatMessage;
import com.cloud1pm.backend.entity.ChatSession;
import com.cloud1pm.backend.entity.User;
import com.cloud1pm.backend.repository.ChatMessageRepository;
import com.cloud1pm.backend.repository.ChatSessionRepository;
import com.cloud1pm.backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final RiskAnalysisService riskAnalysisService;
    private final GeminiService geminiService;
    private final UserService userService;
    private final ChatSessionRepository chatSessionRepository;

    // -----------------------------
    // 세션 생성
    // -----------------------------
    @Transactional
    public ChatSessionResponse createNewSession(Long userId, String title) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        String sessionTitle = (title != null && !title.isEmpty()) ? title : "새로운 채팅";

        ChatSession session = ChatSession.builder()
                .user(user)
                .title(sessionTitle)
                .build();

        session = chatSessionRepository.save(session);
        return ChatSessionResponse.fromEntity(session);
    }

    @Transactional(readOnly = true)
    public List<ChatSessionResponse> getChatSessions(Long userId) {
        List<ChatSession> sessions = chatSessionRepository.findAllByUserIdOrderByUpdatedAtDesc(userId);
        return sessions.stream()
                .map(ChatSessionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public ChatSessionResponse updateSessionTitle(Long sessionId, String newTitle, Long userId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("ChatSession not found with id: " + sessionId));

        if (!session.getUser().getId().equals(userId)) {
            throw new SecurityException("Access denied. Session does not belong to user.");
        }

        session.setTitle(newTitle);
        return ChatSessionResponse.fromEntity(session);
    }

    @Transactional
    public void deleteSession(Long sessionId, Long userId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("ChatSession not found with id: " + sessionId));

        if (!session.getUser().getId().equals(userId)) {
            throw new SecurityException("Access denied. Session does not belong to user.");
        }

        chatSessionRepository.delete(session);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getChatHistory(Long sessionId, Long userId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("ChatSession not found with id: " + sessionId));

        if (!session.getUser().getId().equals(userId)) {
            throw new SecurityException("Access denied. Session does not belong to user.");
        }

        List<ChatMessage> messages = chatMessageRepository.findAllBySessionIdOrderByCreatedAtAsc(sessionId);
        return messages.stream()
                .map(ChatMessageResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // -----------------------------
    // 메시지 처리
    // -----------------------------
    @Transactional
    public ChatResponse sendMessage(Long userId, ChatRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ChatSession newSession = ChatSession.builder()
                .user(user)
                .title(request.getMessage().substring(0, Math.min(request.getMessage().length(), 20)))
                .build();
        ChatSession session = chatSessionRepository.save(newSession);

        Map<String, Object> geminiResult = geminiService.generateChatResponseAndAnalyzeSentiment(request.getMessage());
        String botResponseFromGemini = (String) geminiResult.get("botResponse");
        String sentiment = (String) geminiResult.get("sentiment");
        Double sentimentScore = (Double) geminiResult.get("score");

        ChatMessage userMessage = ChatMessage.builder()
                .session(session)
                .userId(userId)
                .message(request.getMessage())
                .isUserMessage(true)
                .sentiment(sentiment)
                .sentimentScore(sentimentScore)
                .build();
        chatMessageRepository.save(userMessage);

        int riskLevel = riskAnalysisService.calculateRiskLevel(userId);

        String finalBotResponse = botResponseFromGemini + getRiskRecommendation(userId, riskLevel);

        ChatMessage botMessage = ChatMessage.builder()
                .session(session)
                .userId(userId)
                .message(finalBotResponse)
                .isUserMessage(false)
                .build();
        chatMessageRepository.save(botMessage);

        session.setUpdatedAt(LocalDateTime.now());
        chatSessionRepository.save(session);

        return ChatResponse.builder()
                .message(finalBotResponse)
                .sentiment(sentiment)
                .riskLevel(riskLevel)
                .build();
    }

    @Transactional
    public ChatResponse processMessage(Long userId, Long sessionId, ChatRequest chatRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("ChatSession not found with id: " + sessionId));

        if (!session.getUser().getId().equals(userId)) {
            throw new SecurityException("Access denied.");
        }

        String userMessage = chatRequest.getMessage();
        Map<String, Object> geminiResult = geminiService.generateChatResponseAndAnalyzeSentiment(userMessage);

        String botResponse = (String) geminiResult.get("botResponse");
        String sentiment = (String) geminiResult.get("sentiment");
        Double sentimentScore = (Double) geminiResult.get("score");

        int riskLevel = riskAnalysisService.calculateRiskLevel(userId);
        String finalBotResponse = botResponse + getRiskRecommendation(userId, riskLevel);

        ChatMessage userMsgEntity = ChatMessage.builder()
                .session(session)
                .userId(userId)
                .message(userMessage)
                .isUserMessage(true)
                .sentiment(sentiment)
                .sentimentScore(sentimentScore)
                .build();
        chatMessageRepository.save(userMsgEntity);

        ChatMessage botMsgEntity = ChatMessage.builder()
                .session(session)
                .userId(userId)
                .message(finalBotResponse)
                .isUserMessage(false)
                .build();
        chatMessageRepository.save(botMsgEntity);

        session.setUpdatedAt(LocalDateTime.now());
        chatSessionRepository.save(session);

        return ChatResponse.builder()
                .message(finalBotResponse)
                .sentiment(sentiment)
                .riskLevel(riskLevel)
                .build();
    }

    // -----------------------------
    // 위험도 추천 메시지
    // -----------------------------
    private String getRiskRecommendation(Long userId, int riskLevel) {
        List<RiskSolutionResponse> solutions = userService.getRiskSolutions(userId, riskLevel);

        if (!solutions.isEmpty()) {
            String rec = solutions.get(0).getSolution();
            return "\n\n---\n⚠️ **위험도 알림: " + riskLevel + "/10**\n" +
                    "이 정도의 힘든 상황에서 당신이 스스로 설정한 해결 방안은:\n" +
                    "**\"" + rec + "\"**\n작은 것부터 시도해 보는 건 어떨까요? 힘내요!";
        }

        if (riskLevel >= 8) {
            return "\n\n---\n⚠️ **위험도 알림: " + riskLevel + "/10**\n" +
                    "지금 많이 힘드시군요. 전문가의 도움이 필요할 때는 상담 센터에 연락해 보세요.";
        }

        return "";
    }

    // -----------------------------
    // 기존 문자열 기반 점수 변환 (수정 없음)
    // -----------------------------
    private double convertSentimentToScore(String sentiment) {
        if (sentiment == null) return 0;

        String s = sentiment.toLowerCase().trim();

        switch (s) {
            case "positive":
            case "긍정":
                return 1.0;

            case "negative":
            case "부정":
                return -1.0;

            case "neutral":
            case "중립":
                return 0.0;

            default:
                return 0.0;
        }
    }

    // -----------------------------
    // ★ 추가된 부분: 문자열 + Gemini 점수 결합
    // -----------------------------
    private double convertSentimentToFinalScore(String sentiment, Double geminiScore) {

        double dir = convertSentimentToScore(sentiment);   // 기존 방식(방향)
        double raw = (geminiScore != null) ? geminiScore : 0.0;  // Gemini의 강도 값

        // 문자열 70%, Gemini 점수 30% 비율로 합산
        return dir * 0.7 + raw * 0.3;
    }

    // -----------------------------
    // Emotion Trend 계산
    // -----------------------------
    @Transactional(readOnly = true)
    public EmotionTrendResponse getEmotionTrend(Long userId, int days) {

        LocalDateTime startDate = LocalDateTime.now().minusDays(days);

        List<ChatMessage> messages =
                chatMessageRepository.findUserMessagesForEmotionTrend(userId, startDate);

        Map<LocalDate, List<ChatMessage>> groupedByDate = messages.stream()
                .filter(m -> m.getIsUserMessage() && m.getSentiment() != null)
                .collect(Collectors.groupingBy(m -> m.getCreatedAt().toLocalDate()));

        List<EmotionTrendResponse.DailyEmotion> dailyEmotions = groupedByDate.entrySet().stream()
                .map(entry -> {

                    // 변경된 부분: avg 계산에 convertSentimentToFinalScore 사용
                    double avgScore = entry.getValue().stream()
                            .mapToDouble(m -> convertSentimentToFinalScore(
                                    m.getSentiment(),
                                    m.getSentimentScore()
                            ))
                            .average()
                            .orElse(0.0);

                    String dominantSentiment;
                    if (avgScore > 0.1) dominantSentiment = "positive";
                    else if (avgScore < -0.1) dominantSentiment = "negative";
                    else dominantSentiment = "neutral";

                    Integer avgRiskLevel = mapSentimentScoreToRiskLevel(avgScore);

                    return new EmotionTrendResponse.DailyEmotion(
                            entry.getKey(), dominantSentiment, avgScore, avgRiskLevel);
                })
                .sorted(Comparator.comparing(EmotionTrendResponse.DailyEmotion::getDate))
                .collect(Collectors.toList());

        Double overallAverageRisk = dailyEmotions.stream()
                .mapToDouble(EmotionTrendResponse.DailyEmotion::getAverageRiskLevel)
                .average()
                .orElse(0.0);

        Double finalOverallRisk = (double) Math.round(overallAverageRisk);

        return new EmotionTrendResponse(finalOverallRisk, dailyEmotions);
    }

    // -----------------------------
    // 기존 위험도 변환 (수정 없음)
    // -----------------------------
    private Integer mapSentimentScoreToRiskLevel(Double score) {
        if (score == null) return 1;

        double risk = 1 + (1.0 - score) * 4.5;

        return (int) Math.round(Math.max(1, Math.min(10, risk)));
    }
}
