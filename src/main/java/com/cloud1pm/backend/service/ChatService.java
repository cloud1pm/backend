package com.cloud1pm.backend.service;

import com.cloud1pm.backend.dto.*;
import com.cloud1pm.backend.entity.ChatMessage;
import com.cloud1pm.backend.entity.ChatSession;
import com.cloud1pm.backend.entity.User;
import com.cloud1pm.backend.repository.ChatMessageRepository;
import com.cloud1pm.backend.repository.ChatSessionRepository; // 추가
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

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final RiskAnalysisService riskAnalysisService;
    private final GeminiService geminiService;
    private final UserService userService;
    private final ChatSessionRepository chatSessionRepository; // [FIX 1]: ChatSessionRepository 주입

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

    // [수정]: 세션이 없는 경우 새 세션을 생성하도록 로직 추가 및 필수 필드 누락 수정
    @Transactional
    public ChatResponse sendMessage(Long userId, ChatRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // [FIX 2]: SessionId가 없으므로, 새 세션을 생성하여 사용합니다.
        ChatSession newSession = ChatSession.builder()
                .user(user)
                .title(request.getMessage().substring(0, Math.min(request.getMessage().length(), 20))) // 메시지 앞부분으로 세션명 지정
                .build();
        ChatSession session = chatSessionRepository.save(newSession);

        // 1. Gemini를 사용하여 챗봇 응답 및 감정 분석
        Map<String, Object> geminiResult = geminiService.generateChatResponseAndAnalyzeSentiment(request.getMessage());
        String botResponseFromGemini = (String) geminiResult.get("botResponse");
        String sentiment = (String) geminiResult.get("sentiment");
        Double sentimentScore = (Double) geminiResult.get("score");

        // 사용자 메시지 저장
        ChatMessage userMessage = ChatMessage.builder()
                .session(session) // [FIX 3]: session 필드 추가
                .userId(userId) // [FIX 4]: userId 필드 추가
                .message(request.getMessage())
                .isUserMessage(true)
                .sentiment(sentiment)
                .sentimentScore(sentimentScore)
                .build();
        chatMessageRepository.save(userMessage);

        // 2. 위험도 계산 (기존 로직 유지)
        int riskLevel = riskAnalysisService.calculateRiskLevel(userId);

        // 3. 챗봇 응답 생성 (위험도 기반 추천 로직 추가)
        String finalBotResponse = botResponseFromGemini + getRiskRecommendation(userId, riskLevel);

        ChatMessage botMessage = ChatMessage.builder()
                .session(session) // [FIX 5]: session 필드 추가
                .userId(userId) // [FIX 6]: userId 필드 추가
                .message(finalBotResponse)
                .isUserMessage(false)
                .build();
        chatMessageRepository.save(botMessage);

        // 세션의 updatedAt을 갱신합니다.
        session.setUpdatedAt(LocalDateTime.now());
        chatSessionRepository.save(session);

        return ChatResponse.builder()
                .message(finalBotResponse)
                .sentiment(sentiment)
                .riskLevel(riskLevel)
                .build();
    }

    // [수정]: 존재하지 않는 메서드 호출 (generateResponse, analyzeSentiment, evaluateRisk)을 수정하고,
    // sendMessage의 구조와 유사하게 유효한 로직으로 대체합니다.
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

        // 2. 챗봇 응답 생성에 사용할 이전 채팅 기록 가져오기 (context)
        // 현재 GeminiService의 generateChatResponseAndAnalyzeSentiment는 history List<Content>를 받도록 되어 있으나,
        // 구현이 String userMessage만 받으므로, history를 사용한 컨텍스트 로직은 생략합니다.
        // List<ChatMessage> recentMessages = chatMessageRepository.findAllBySessionIdOrderByCreatedAtAsc(sessionId);
        // String chatHistory = recentMessages.stream()
        //         .map(msg -> (msg.getIsUserMessage() ? "User: " : "Bot: ") + msg.getMessage())
        //         .collect(Collectors.joining("\n"));
        // String fullPrompt = chatHistory + "\nUser: " + userMessage;

        // 3. Gemini 호출 (응답 및 감정 분석)
        // [FIX 7]: 존재하지 않는 geminiService.generateResponse 대신 유효한 메서드 호출
        Map<String, Object> geminiResult = geminiService.generateChatResponseAndAnalyzeSentiment(userMessage);
        String botResponseFromGemini = (String) geminiResult.get("botResponse");
        String sentiment = (String) geminiResult.get("sentiment");
        Double sentimentScore = (Double) geminiResult.get("score"); // [FIX 8]: sentimentScore 누락분 추가

        // 4. 위험도 계산
        // [FIX 9]: 존재하지 않는 riskAnalysisService.evaluateRisk 대신 유효한 메서드 호출
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
                .sentimentScore(sentimentScore) // [FIX 10]: sentimentScore 추가
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
                .message(finalBotResponse) // [FIX 11]: responseText -> message로 수정 (ChatResponse DTO에 따름)
                .sentiment(sentiment)
                .riskLevel(riskLevel)
                .build();
    }

    private String getRiskRecommendation(Long userId, int riskLevel) {
        // 위험도 척도(1-10)와 정확히 일치하는 해결 방안을 찾습니다.
        // [수정] 반환 타입을 List<RiskSolutionResponse>로 변경
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