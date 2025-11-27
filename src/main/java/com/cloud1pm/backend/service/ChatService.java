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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

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
    private final UserService userService;
    private final ChatSessionRepository chatSessionRepository;

    // HTTP 호출을 위한 RestTemplate
    private final RestTemplate restTemplate;

    // AI 서비스 URL (쿠버네티스 Service DNS 이름)
    @Value("${ai.service.url:http://ai-analysis-service:8080}")
    private String aiServiceUrl;

    // ... (createNewSession, getChatSessions 등 세션 관련 메서드는 기존과 동일 유지) ...
    // 편의를 위해 중복 코드는 생략하고 수정된 sendMessage/processMessage 부분만 작성합니다.

    @Transactional
    public ChatSessionResponse createNewSession(Long userId, String title) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
        String sessionTitle = (title != null && !title.isEmpty()) ? title : "새로운 채팅";
        ChatSession session = ChatSession.builder().user(user).title(sessionTitle).build();
        session = chatSessionRepository.save(session);
        return ChatSessionResponse.fromEntity(session);
    }

    @Transactional(readOnly = true)
    public List<ChatSessionResponse> getChatSessions(Long userId) {
        return chatSessionRepository.findAllByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(ChatSessionResponse::fromEntity).collect(Collectors.toList());
    }

    @Transactional
    public ChatSessionResponse updateSessionTitle(Long sessionId, String newTitle, Long userId) {
        ChatSession session = chatSessionRepository.findById(sessionId).orElseThrow();
        if(!session.getUser().getId().equals(userId)) throw new SecurityException("Access denied");
        session.setTitle(newTitle);
        return ChatSessionResponse.fromEntity(session);
    }

    @Transactional
    public void deleteSession(Long sessionId, Long userId) {
        ChatSession session = chatSessionRepository.findById(sessionId).orElseThrow();
        if(!session.getUser().getId().equals(userId)) throw new SecurityException("Access denied");
        chatSessionRepository.delete(session);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getChatHistory(Long sessionId, Long userId) {
        ChatSession session = chatSessionRepository.findById(sessionId).orElseThrow();
        if(!session.getUser().getId().equals(userId)) throw new SecurityException("Access denied");
        return chatMessageRepository.findAllBySessionIdOrderByCreatedAtAsc(sessionId).stream()
                .map(ChatMessageResponse::fromEntity).collect(Collectors.toList());
    }

    // --- 수정된 부분: sendMessage ---
    @Transactional
    public ChatResponse sendMessage(Long userId, ChatRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ChatSession newSession = ChatSession.builder()
                .user(user)
                .title(request.getMessage().substring(0, Math.min(request.getMessage().length(), 20)))
                .build();
        ChatSession session = chatSessionRepository.save(newSession);

        return processChatLogic(user, session, request.getMessage());
    }

    // --- 수정된 부분: processMessage ---
    @Transactional
    public ChatResponse processMessage(Long userId, Long sessionId, ChatRequest chatRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("ChatSession not found"));

        if (!session.getUser().getId().equals(userId)) {
            throw new SecurityException("Access denied.");
        }

        return processChatLogic(user, session, chatRequest.getMessage());
    }

    private ChatResponse processChatLogic(User user, ChatSession session, String userMessageText) {
        // 1. AI 서비스 호출 (Gemini 로직 분리)
        AIAnalysisRequest aiRequest = new AIAnalysisRequest(userMessageText);
        AIAnalysisResponse aiResponse;

        try {
            // 내부 네트워크(K8s Service)를 통해 AI 컨테이너 호출
            aiResponse = restTemplate.postForObject(
                    aiServiceUrl + "/internal/ai/analyze",
                    aiRequest,
                    AIAnalysisResponse.class
            );
        } catch (Exception e) {
            log.error("Failed to connect to AI Service", e);
            aiResponse = AIAnalysisResponse.builder()
                    .botResponse("현재 AI 서비스 연결이 지연되고 있습니다.")
                    .sentiment("neutral")
                    .score(0.0)
                    .build();
        }

        String botResponse = aiResponse.getBotResponse();
        String sentiment = aiResponse.getSentiment();
        Double sentimentScore = aiResponse.getScore();

        // 2. 사용자 메시지 저장
        ChatMessage userMessage = ChatMessage.builder()
                .session(session)
                .userId(user.getId())
                .message(userMessageText)
                .isUserMessage(true)
                .sentiment(sentiment)
                .sentimentScore(sentimentScore)
                .build();
        chatMessageRepository.save(userMessage);

        // 3. 위험도 분석 및 추천
        int riskLevel = riskAnalysisService.calculateRiskLevel(user.getId());
        String finalBotResponse = botResponse + getRiskRecommendation(user.getId(), riskLevel);

        // 4. 봇 메시지 저장
        ChatMessage botMessage = ChatMessage.builder()
                .session(session)
                .userId(user.getId())
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

    private String getRiskRecommendation(Long userId, int riskLevel) {
        // [수정] 위험도가 1~5인 경우(낮음~보통)에는 추천 로직을 수행하지 않고 빈 문자열 반환
        if (riskLevel <= 5) {
            return "";
        }

        // 위험도 6~10일 때만 저장된 해결 방안 조회
        List<RiskSolutionResponse> solutions = userService.getRiskSolutions(userId, riskLevel);

        if (!solutions.isEmpty()) {
            String rec = solutions.get(0).getSolution();
            return "\n\n---\n⚠️ **위험도 알림: " + riskLevel + "/10**\n" +
                    "스스로 설정한 해결 방안: **\"" + rec + "\"**\n작은 것부터 시도해 보세요!";
        }

        // 해결 방안이 없는데 고위험군(8 이상)인 경우
        if (riskLevel >= 8) {
            return "\n\n---\n⚠️ **위험도 알림: " + riskLevel + "/10**\n" +
                    "전문가의 도움이 필요할 때는 상담 센터에 연락해 보세요.";
        }
        return "";
    }

    // Emotion Trend 계산 로직 (기존 코드 유지)
    @Transactional(readOnly = true)
    public EmotionTrendResponse getEmotionTrend(Long userId, int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        List<ChatMessage> messages = chatMessageRepository.findUserMessagesForEmotionTrend(userId, startDate);

        Map<LocalDate, List<ChatMessage>> groupedByDate = messages.stream()
                .filter(m -> m.getIsUserMessage() && m.getSentiment() != null)
                .collect(Collectors.groupingBy(m -> m.getCreatedAt().toLocalDate()));

        List<EmotionTrendResponse.DailyEmotion> dailyEmotions = groupedByDate.entrySet().stream()
                .map(entry -> {
                    double avgScore = entry.getValue().stream()
                            .mapToDouble(m -> convertSentimentToFinalScore(m.getSentiment(), m.getSentimentScore()))
                            .average().orElse(0.0);

                    String dominantSentiment = avgScore > 0.1 ? "positive" : (avgScore < -0.1 ? "negative" : "neutral");
                    Integer avgRiskLevel = mapSentimentScoreToRiskLevel(avgScore);
                    return new EmotionTrendResponse.DailyEmotion(entry.getKey(), dominantSentiment, avgScore, avgRiskLevel);
                })
                .sorted(Comparator.comparing(EmotionTrendResponse.DailyEmotion::getDate))
                .collect(Collectors.toList());

        Double overallAverageRisk = dailyEmotions.stream().mapToDouble(EmotionTrendResponse.DailyEmotion::getAverageRiskLevel).average().orElse(0.0);
        return new EmotionTrendResponse((double) Math.round(overallAverageRisk), dailyEmotions);
    }

    private double convertSentimentToScore(String sentiment) { /* 기존 동일 */
        if (sentiment == null) return 0;
        switch (sentiment.toLowerCase().trim()) {
            case "positive": case "긍정": return 1.0;
            case "negative": case "부정": return -1.0;
            default: return 0.0;
        }
    }
    private double convertSentimentToFinalScore(String sentiment, Double geminiScore) {
        double dir = convertSentimentToScore(sentiment);
        double raw = (geminiScore != null) ? geminiScore : 0.0;
        return dir * 0.7 + raw * 0.3;
    }
    private Integer mapSentimentScoreToRiskLevel(Double score) {
        if (score == null) return 1;
        double risk = 1 + (1.0 - score) * 4.5;
        return (int) Math.round(Math.max(1, Math.min(10, risk)));
    }
}