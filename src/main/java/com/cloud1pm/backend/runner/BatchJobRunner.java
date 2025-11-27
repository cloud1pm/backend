package com.cloud1pm.backend.runner;

import com.cloud1pm.backend.service.BatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("batch") // 'batch' 프로필일 때만 이 Runner가 활성화됨
@RequiredArgsConstructor
@Slf4j
public class BatchJobRunner implements CommandLineRunner {

    private final BatchService batchService;

    @Value("${job.name:NONE}") // 환경변수 JOB_NAME을 읽음 (기본값 NONE)
    private String jobName;

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting Batch Job Runner... Job Name: {}", jobName);

        try {
            switch (jobName.toUpperCase()) {
                case "ALARM":
                    batchService.runAlarmJob();
                    break;
                case "REPORT":
                    batchService.runWeeklyReportJob();
                    break;
                default:
                    log.warn("No valid JOB_NAME provided. Skipping execution.");
            }
        } catch (Exception e) {
            log.error("Batch Job Failed", e);
            throw e; // 에러를 던져서 Pod가 실패 상태(Error)로 종료되게 함 -> K8s가 재시도 가능
        }

        log.info("Batch Job Completed. Application will exit.");
    }
}