package com.cloud1pm.backend.repository;

import com.cloud1pm.backend.entity.AlertSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface AlertSettingRepository extends JpaRepository<AlertSetting, Long> {
    Optional<AlertSetting> findByUserId(Long userId);

    // 배치 작업을 위해 특정 시간대의 활성화된 설정 조회
    List<AlertSetting> findAllByAlertTimeAndIsEnabledTrue(LocalTime alertTime);
}