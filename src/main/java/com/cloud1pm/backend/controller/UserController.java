package com.cloud1pm.backend.controller;

import com.cloud1pm.backend.dto.InitialSetupRequest;
import com.cloud1pm.backend.dto.UserStatusResponse;
import com.cloud1pm.backend.dto.RiskSolutionResponse; // 추가
import com.cloud1pm.backend.entity.EncouragementMessage; // Added import
import com.cloud1pm.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/initial-setup")
    public ResponseEntity<Void> completeInitialSetup(
            Authentication authentication,
            @RequestBody InitialSetupRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        userService.completeInitialSetup(userId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/risk-solutions")
    // [수정] 반환 타입을 List<RiskSolution>에서 List<RiskSolutionResponse>로 변경
    public ResponseEntity<List<RiskSolutionResponse>> getRiskSolutions(
            Authentication authentication,
            @RequestParam(required = false) Integer riskLevel) {
        Long userId = (Long) authentication.getPrincipal();
        List<RiskSolutionResponse> solutions = userService.getRiskSolutions(userId, riskLevel);
        return ResponseEntity.ok(solutions);
    }

    @PostMapping("/encouragement")
    public ResponseEntity<Void> createEncouragementMessage(
            Authentication authentication,
            @RequestBody Map<String, String> request) {
        Long userId = (Long) authentication.getPrincipal();
        userService.createEncouragementMessage(userId, request.get("message"));
        return ResponseEntity.ok().build();
    }

    // 응원 문구 전체 가져오기 (추가됨)
    @GetMapping("/encouragement")
    public ResponseEntity<List<EncouragementMessage>> getAllEncouragementMessages(
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        List<EncouragementMessage> messages = userService.getEncouragementMessages(userId);
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/feed-character")
    public ResponseEntity<Void> feedCharacter(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        userService.feedCharacter(userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/status")
    public ResponseEntity<UserStatusResponse> getUserStatus(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        UserStatusResponse response = userService.getUserStatus(userId);
        return ResponseEntity.ok(response);
    }
}