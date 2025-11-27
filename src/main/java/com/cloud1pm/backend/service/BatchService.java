package com.cloud1pm.backend.service;

import com.cloud1pm.backend.entity.AlertSetting;
import com.cloud1pm.backend.entity.ChatMessage;
import com.cloud1pm.backend.entity.ChatSession;
import com.cloud1pm.backend.entity.User;
import com.cloud1pm.backend.repository.AlertSettingRepository;
import com.cloud1pm.backend.repository.ChatMessageRepository;
import com.cloud1pm.backend.repository.ChatSessionRepository; // [추가]
import com.cloud1pm.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BatchService {

    private final AlertSettingRepository alertSettingRepository;
    private final UserRepository userRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatSessionRepository chatSessionRepository; // [추가] 세션 조회용

    // 1. 알림 전송 로직
    @Transactional
    public void runAlarmJob() {
        LocalTime now = LocalTime.now().truncatedTo(ChronoUnit.MINUTES);
        log.info("=== [Batch Job Start] Alarm Check for time: {} ===", now);

        List<AlertSetting> targets = alertSettingRepository.findAllByAlertTimeAndIsEnabledTrue(now);

        if (targets.isEmpty()) {
            log.info("No alerts scheduled for this time.");
        } else {
            for (AlertSetting setting : targets) {
                User user = setting.getUser();
                log.info(">>> [SEND ALARM] User: {}", user.getNickname());

                // [수정] 가장 최근 채팅 세션에 알림 메시지 저장
                saveSystemMessage(user, "⏰ [알림] 감정 기록을 남길 시간입니다!\n오늘 하루는 어떠셨나요? 잠깐 들러서 이야기해주세요. 📝");
            }
        }
        log.info("=== [Batch Job End] Alarm Check Finished ===");
    }

    // 2. 주간 보고서 생성 로직
    @Transactional
    public void runWeeklyReportJob() {
        log.info("=== [Batch Job Start] Weekly Report Generation ===");

        LocalDateTime oneWeekAgo = LocalDateTime.now().minusDays(7);
        List<User> users = userRepository.findAll();

        int reportCount = 0;

        for (User user : users) {
            // 지난 7일간의 사용자 메시지 조회 (감정 점수 있는 것만)
            List<ChatMessage> messages = chatMessageRepository.findUserMessagesForEmotionTrend(user.getId(), oneWeekAgo);

            if (messages.isEmpty()) {
                continue;
            }

            // 감정 통계 계산
            double avgScore = messages.stream()
                    .mapToDouble(m -> m.getSentimentScore() != null ? m.getSentimentScore() : 0.0)
                    .average()
                    .orElse(0.0);

            long positiveCount = messages.stream().filter(m -> "positive".equalsIgnoreCase(m.getSentiment())).count();
            long negativeCount = messages.stream().filter(m -> "negative".equalsIgnoreCase(m.getSentiment())).count();

            // [수정] 주간 보고서 메시지 생성
            String reportMessage = String.format("""
                    📊 [주간 감정 보고서]
                    
                    지난 한 주간 %s님의 마음 흐름이에요.
                    
                    - 📝 총 대화: %d건
                    - 😊 긍정 감정: %d건
                    - ☁️ 부정 감정: %d건
                    - ⭐ 평균 감정 점수: %.1f점
                    
                    다음 주도 당신의 편안한 하루를 응원할게요! 💪
                    """, user.getNickname(), messages.size(), positiveCount, negativeCount, avgScore * 10); // 점수 스케일링 (-10 ~ 10 표현 가정)

            // 채팅방에 전송
            saveSystemMessage(user, reportMessage);
            reportCount++;
        }

        log.info(">>> Weekly reports generated and sent for {} users.", reportCount);
        log.info("=== [Batch Job End] Weekly Report Finished ===");
    }

    // 시스템 메시지 저장 헬퍼 메서드
    private void saveSystemMessage(User user, String text) {
        // 사용자의 최근 채팅 세션을 찾음 (없으면 건너뛰거나 새로 만들 수도 있음 - 여기선 최근 세션에 추가)
        List<ChatSession> sessions = chatSessionRepository.findAllByUserIdOrderByUpdatedAtDesc(user.getId());

        if (!sessions.isEmpty()) {
            ChatSession latestSession = sessions.get(0);

            ChatMessage message = ChatMessage.builder()
                    .session(latestSession)
                    .userId(user.getId())
                    .message(text)
                    .isUserMessage(false) // 봇 메시지로 처리
                    .build(); // sentiment는 null (시스템 메시지)

            chatMessageRepository.save(message);

            // 세션 업데이트 시간 갱신 (채팅 목록 상단으로 이동)
            latestSession.setUpdatedAt(LocalDateTime.now());
            chatSessionRepository.save(latestSession);
        }
    }
}