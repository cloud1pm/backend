package com.cloud1pm.backend.service;

import com.cloud1pm.backend.dto.AlertSettingRequest;
import com.cloud1pm.backend.entity.AlertSetting;
import com.cloud1pm.backend.entity.User;
import com.cloud1pm.backend.repository.AlertSettingRepository;
import com.cloud1pm.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertSettingRepository alertSettingRepository;
    private final UserRepository userRepository;

    @Transactional
    public void saveOrUpdateAlertSetting(Long userId, AlertSettingRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        AlertSetting setting = alertSettingRepository.findByUserId(userId)
                .orElse(AlertSetting.builder().user(user).build());

        setting.setAlertTime(request.getAlertTime());
        setting.setIsEnabled(request.getIsEnabled());

        alertSettingRepository.save(setting);
    }

    @Transactional(readOnly = true)
    public AlertSettingRequest getAlertSetting(Long userId) {
        AlertSetting setting = alertSettingRepository.findByUserId(userId)
                .orElse(new AlertSetting());

        AlertSettingRequest dto = new AlertSettingRequest();
        dto.setAlertTime(setting.getAlertTime());
        dto.setIsEnabled(setting.getIsEnabled());
        return dto;
    }
}