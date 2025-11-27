package com.cloud1pm.backend.controller;

import com.cloud1pm.backend.dto.AlertSettingRequest;
import com.cloud1pm.backend.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/alert")
@RequiredArgsConstructor
@Profile("backend")
public class AlertController {

    private final AlertService alertService;

    @PostMapping("/settings")
    public ResponseEntity<Void> updateSettings(
            Authentication authentication,
            @RequestBody AlertSettingRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        alertService.saveOrUpdateAlertSetting(userId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/settings")
    public ResponseEntity<AlertSettingRequest> getSettings(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        AlertSettingRequest response = alertService.getAlertSetting(userId);
        return ResponseEntity.ok(response);
    }
}