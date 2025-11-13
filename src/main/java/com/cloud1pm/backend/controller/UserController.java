package com.cloud1pm.backend.controller;

import com.cloud1pm.backend.dto.*;
import com.cloud1pm.backend.entity.EncouragementMessage; // Added import
import com.cloud1pm.backend.service.UserService;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Getter
@Builder
class SignInResponse {
    private String token;
    private Long userId; // 실제로는 JWT 파싱을 통해 얻어야 함
}

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // [추가] 회원 가입
    @PostMapping("/signup")
    public ResponseEntity<UserProfileResponse> signUp(@RequestBody SignUpRequest request) {
        UserProfileResponse response = userService.signUp(request);
        return ResponseEntity.ok(response);
    }

    // [추가] 로그인
    @PostMapping("/login")
    public ResponseEntity<SignInResponse> signIn(@RequestBody SignInRequest request) {
        String jwtToken = userService.signIn(request);
        // 임시로 userId를 1L로 설정 (실제로는 토큰에서 파싱해야 함)
        return ResponseEntity.ok(SignInResponse.builder().token(jwtToken).userId(1L).build());
    }

    // [추가] 회원 정보 조회 (GET /api/user)
    @GetMapping
    public ResponseEntity<UserProfileResponse> getUserProfile(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        UserProfileResponse response = userService.getUserProfile(userId);
        return ResponseEntity.ok(response);
    }

    // [추가] 회원 정보 수정 (닉네임, 사진)
    @PatchMapping
    public ResponseEntity<UserProfileResponse> updateUserProfile(
            Authentication authentication,
            @RequestBody UserUpdateRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        UserProfileResponse response = userService.updateUserProfile(userId, request);
        return ResponseEntity.ok(response);
    }

    // [추가] 비밀번호 수정
    @PatchMapping("/password")
    public ResponseEntity<Void> updatePassword(
            Authentication authentication,
            @RequestBody PasswordUpdateRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        userService.updatePassword(userId, request);
        return ResponseEntity.ok().build();
    }

    // [추가] 회원 탈퇴
    @DeleteMapping
    public ResponseEntity<Void> deleteUser(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    // [수정] /api/user/initial-setup의 요소 수정 (단계별 해결 방법 수정) - PUT으로 재사용
    // 기존 POST는 유지하고, PUT을 수정용으로 추가합니다.
    @PutMapping("/initial-setup")
    public ResponseEntity<Void> updateInitialSetup(
            Authentication authentication,
            @RequestBody InitialSetupRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        userService.completeInitialSetup(userId, request); // 기존 메서드를 재사용하여 수정
        return ResponseEntity.ok().build();
    }

    @PostMapping("/initial-setup")
    public ResponseEntity<Void> completeInitialSetup(
            Authentication authentication,
            @RequestBody InitialSetupRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        userService.completeInitialSetup(userId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/risk-solutions")
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

    // 응원 문구 전체 가져오기
    @GetMapping("/encouragement")
    public ResponseEntity<List<EncouragementMessage>> getAllEncouragementMessages(
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        List<EncouragementMessage> messages = userService.getEncouragementMessages(userId);
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/feed-character")
    public ResponseEntity<FeedCharacterResponse> feedCharacter(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        FeedCharacterResponse response = userService.feedCharacter(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity<UserStatusResponse> getUserStatus(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        UserStatusResponse response = userService.getUserStatus(userId);
        return ResponseEntity.ok(response);
    }
}