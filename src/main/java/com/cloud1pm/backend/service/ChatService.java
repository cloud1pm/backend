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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

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
    public ChatHistoryResponse getChatHistory(Long sessionId, Long userId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("ChatSession not found with id: " + sessionId));

        if (!session.getUser().getId().equals(userId)) {
            throw new SecurityException("Access denied. Session does not belong to user.");
        }

        // 1. 해당 세션의 모든 메시지 가져오기
        List<ChatMessage> messages = chatMessageRepository.findAllBySessionIdOrderByCreatedAtAsc(sessionId);

        // 2. 메시지 DTO 리스트로 변환
        List<ChatMessageResponse> messageResponses = messages.stream()
                .map(ChatMessageResponse::fromEntity)
                .collect(Collectors.toList());

        // 3. 통계 계산 (User 메시지 중 감정 점수가 있는 것만 대상)
        List<ChatMessage> userMessages = messages.stream()
                .filter(m -> m.getIsUserMessage() && m.getSentimentScore() != null)
                .collect(Collectors.toList());

        Double avgScore = 0.0;
        Integer avgRisk = 1; // 기본 위험도
        String overallSentiment = "neutral";

        if (!userMessages.isEmpty()) {
            avgScore = userMessages.stream()
                    .mapToDouble(ChatMessage::getSentimentScore)
                    .average()
                    .orElse(0.0);

            // 평균 점수를 기반으로 위험도와 감정 상태 결정
            avgRisk = mapSentimentScoreToRiskLevel(avgScore);
            overallSentiment = determineSentiment(avgScore);
        }

        // 4. ChatHistoryResponse 객체 생성하여 반환
        return ChatHistoryResponse.builder()
                .sessionId(session.getId())
                .title(session.getTitle())
                .sessionSentiment(overallSentiment)
                .averageScore(avgScore)
                .averageRisk(avgRisk)
                .messages(messageResponses)
                .build();
    }

    // 세션이 없는 경우 새 세션을 생성하도록 로직 추가 및 필수 필드 누락 수정
    @Transactional
    public ChatResponse sendMessage(Long userId, ChatRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 1. 날짜와 요일로 기본 타이틀 생성 (예: 2025년 11월 19일 (수))
        LocalDate now = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 (E)", Locale.KOREA);
        String baseTitle = now.format(formatter);

        // 2. 중복 타이틀 확인 및 넘버링 (예: (2), (3) ...)
        // 사용자의 기존 세션들을 가져와서 타이틀 충돌을 검사합니다.
        List<ChatSession> existingSessions = chatSessionRepository.findAllByUserIdOrderByUpdatedAtDesc(userId);

        String finalTitle = baseTitle;
        int count = 1;

        // 중복되는 타이틀이 없을 때까지 숫자를 증가시킴
        while (isTitleExists(existingSessions, finalTitle)) {
            count++;
            finalTitle = baseTitle + "(" + count + ")";
        }

        // 3. 결정된 타이틀로 새 세션 생성
        ChatSession newSession = ChatSession.builder()
                .user(user)
                .title(finalTitle)
                .build();
        ChatSession session = chatSessionRepository.save(newSession);

        // --- 이하 기존 로직과 동일 ---

        // 4. Gemini를 사용하여 챗봇 응답 및 감정 분석
        Map<String, Object> geminiResult = geminiService.generateChatResponseAndAnalyzeSentiment(request.getMessage());
        String botResponseFromGemini = (String) geminiResult.get("botResponse");
        String sentiment = (String) geminiResult.get("sentiment");
        Double sentimentScore = (Double) geminiResult.get("score");

        // 사용자 메시지 저장
        ChatMessage userMessage = ChatMessage.builder()
                .session(session)
                .userId(userId)
                .message(request.getMessage())
                .isUserMessage(true)
                .sentiment(sentiment)
                .sentimentScore(sentimentScore)
                .build();
        chatMessageRepository.save(userMessage);

        // 위험도 계산 및 응답 생성
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

    private boolean isTitleExists(List<ChatSession> sessions, String title) {
        return sessions.stream()
                .anyMatch(session -> session.getTitle().equals(title));
    }

    @Transactional
    public ChatResponse processMessage(Long userId, Long sessionId, ChatRequest chatRequest) {
        // 1. 세션 확인 및 사용자 검증
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("ChatSession not found with id: " + sessionId));

        if (!session.getUser().getId().equals(userId)) {
            throw new SecurityException("Access denied. Session does not belong to user.");
        }

        String userMessage = chatRequest.getMessage();

        Map<String, Object> geminiResult = geminiService.generateChatResponseAndAnalyzeSentiment(userMessage);
        String botResponseFromGemini = (String) geminiResult.get("botResponse");
        String sentiment = (String) geminiResult.get("sentiment");
        Double sentimentScore = (Double) geminiResult.get("score");

        // 4. 위험도 계산
        int riskLevel = riskAnalysisService.calculateRiskLevel(userId);

        // 5. 챗봇 응답 생성 (위험도 기반 추천 로직 추가)
        String finalBotResponse = botResponseFromGemini + getRiskRecommendation(userId, riskLevel);


        // 6. 메시지 저장 (사용자)
        ChatMessage userMsgEntity = ChatMessage.builder()
                .session(session)
                .userId(userId)
                .message(userMessage)
                .isUserMessage(true)
                .sentiment(sentiment)
                .sentimentScore(sentimentScore)
                .build();
        chatMessageRepository.save(userMsgEntity);

        // 7. 메시지 저장 (챗봇 응답)
        ChatMessage botMsgEntity = ChatMessage.builder()
                .session(session)
                .userId(userId)
                .message(finalBotResponse)
                .isUserMessage(false)
                .build();
        chatMessageRepository.save(botMsgEntity);

        // 세션의 updatedAt을 갱신합니다.
        session.setUpdatedAt(LocalDateTime.now());
        chatSessionRepository.save(session);

        // 8. 응답 반환
        return ChatResponse.builder()
                .message(finalBotResponse)
                .sentiment(sentiment)
                .riskLevel(riskLevel)
                .build();
    }

    private String getRiskRecommendation(Long userId, int riskLevel) {
        // 위험도 척도(1-10)와 정확히 일치하는 해결 방안을 찾습니다.
        List<RiskSolutionResponse> solutions = userService.getRiskSolutions(userId, riskLevel);

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

    /**
     * 감정 점수(Sentiment Score, -1.0 ~ 1.0)를 감정 위험도(1 ~ 10)로 변환
     * 점수가 낮을수록(부정적일수록) 위험도가 높아짐
     * (1~5: 긍정, 6~10: 위험)
     * @param sentimentScore 감정 점수 (-1.0 ~ 1.0)
     * @return 감정 위험도 (1 ~ 10)
     */
    private Integer mapSentimentScoreToRiskLevel(Double sentimentScore) {
        if (sentimentScore == null) return 1;

        // 변환 공식: -1.0 -> 10, 1.0 -> 1. 공식: risk = 5.5 - 4.5 * score
        double risk = 1 + (1.0 - sentimentScore) * 4.5;

        // 결과는 1에서 10 사이의 정수로 반올림하여 반환합니다.
        return (int) Math.round(Math.max(1, Math.min(10, risk)));
    }

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

                    // [수정] 분리된 메서드를 사용하여 판정 로직 통일
                    String dominantSentiment = determineSentiment(avgScore);

                    Integer avgRiskLevel = mapSentimentScoreToRiskLevel(avgScore);

                    return new EmotionTrendResponse.DailyEmotion(
                            entry.getKey(), dominantSentiment, avgScore, avgRiskLevel);
                })
                .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
                .collect(Collectors.toList());

        Double overallAverageRisk = dailyEmotions.stream()
                .mapToDouble(EmotionTrendResponse.DailyEmotion::getAverageRiskLevel)
                .average()
                .orElse(0.0);

        Double finalOverallAverageRisk = (double) Math.round(overallAverageRisk);

        return new EmotionTrendResponse(finalOverallAverageRisk, dailyEmotions);
    }

    /**
     * 감정 판정 로직 개선
     * 기존: -0.1 ~ 0.1 범위를 neutral로 잡아 -0.018 등이 neutral이 되는 문제 해결
     * 수정: 0을 기준으로 하거나 아주 좁은 범위만 neutral로 설정
     */
    private String determineSentiment(double score) {
        // 0.05 정도로 기준을 좁힘 (사용자의 요청에 따라 음수면 negative가 나오도록 조정)
        double epsilon = 0.01; // 아주 작은 오차 범위만 허용

        if (score > epsilon) {
            return "positive";
        } else if (score < -epsilon) {
            return "negative";
        } else {
            return "neutral"; // 거의 0에 가까울 때만 중립
        }
    }
}