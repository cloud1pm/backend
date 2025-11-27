package com.cloud1pm.backend.service;

import com.cloud1pm.backend.entity.AlertSetting;
import com.cloud1pm.backend.repository.AlertSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BatchService {

    private final AlertSettingRepository alertSettingRepository;

    // 1. 알림 전송 로직 (매 분 실행될 예정)
    @Transactional(readOnly = true)
    public void runAlarmJob() {
        // 초 단위 절삭 (HH:mm:00)
        LocalTime now = LocalTime.now().truncatedTo(ChronoUnit.MINUTES);
        log.info("=== [Batch Job Start] Alarm Check for time: {} ===", now);

        List<AlertSetting> targets = alertSettingRepository.findAllByAlertTimeAndIsEnabledTrue(now);

        if (targets.isEmpty()) {
            log.info("No alerts scheduled for this time.");
        } else {
            for (AlertSetting setting : targets) {
                // 실제 알림 전송 로직 (FCM 등) 구현
                log.info(">>> [SEND ALARM] User: {}, Message: 감정 기록을 남길 시간입니다!",
                        setting.getUser().getNickname());
            }
        }
        log.info("=== [Batch Job End] Alarm Check Finished ===");
    }

    // 2. 주간 보고서 생성 로직 (매주 월요일 실행될 예정)
    @Transactional
    public void runWeeklyReportJob() {
        log.info("=== [Batch Job Start] Weekly Report Generation ===");

        // 여기에 주간 통계 생성 및 저장 로직 구현
        // 예: User 테이블을 순회하며 지난주 ChatMessage 통계 계산 -> Report 엔티티 저장

        log.info(">>> Weekly reports generated successfully.");
        log.info("=== [Batch Job End] Weekly Report Finished ===");
    }
}