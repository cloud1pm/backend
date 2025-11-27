package com.cloud1pm.backend.controller;
import com.cloud1pm.backend.repository.UserRepository; //추가 - Username 중복 체크를 위해


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
    private final UserRepository userRepository;  //추가 - Username 중복 체크를 위해

    // 회원 가입
    @PostMapping("/signup")
    public ResponseEntity<UserProfileResponse> signUp(@RequestBody SignUpRequest request) {
        UserProfileResponse response = userService.signUp(request);
        return ResponseEntity.ok(response);
    }

    // 로그인: username을 받음
    @PostMapping("/login")
    public ResponseEntity<SignInResponse> signIn(@RequestBody SignInRequest request) {
        String jwtToken = userService.signIn(request);
        // 임시로 userId를 1L로 설정 (실제로는 토큰에서 파싱해야 함)
        // 주의: 실제 환경에서는 토큰에서 userId를 안전하게 추출해야 합니다.
        return ResponseEntity.ok(SignInResponse.builder().token(jwtToken).userId(1L).build());
    }

    // 회원 정보 조회 (GET /api/user)
    @GetMapping
    public ResponseEntity<UserProfileResponse> getUserProfile(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        UserProfileResponse response = userService.getUserProfile(userId);
        return ResponseEntity.ok(response);
    }
    @GetMapping("/check-username") //추가 - 중복 체크를 위해
        public ResponseEntity<Boolean> checkUsername(@RequestParam String username) {
        boolean exists = userRepository.existsByUsername(username);
        return ResponseEntity.ok(exists);   // 존재하면 true
    }


    // 회원 정보 수정 (닉네임, 사진)
    @PatchMapping
    public ResponseEntity<UserProfileResponse> updateUserProfile(
            Authentication authentication,
            @RequestBody UserUpdateRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        UserProfileResponse response = userService.updateUserProfile(userId, request);
        return ResponseEntity.ok(response);
    }

    // 비밀번호 수정
    @PatchMapping("/password")
    public ResponseEntity<Void> updatePassword(
            Authentication authentication,
            @RequestBody PasswordUpdateRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        userService.updatePassword(userId, request);
        return ResponseEntity.ok().build();
    }

    // 회원 탈퇴
    @DeleteMapping
    public ResponseEntity<Void> deleteUser(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    // /api/user/initial-setup의 요소 수정 (단계별 해결 방법 수정)
    @PutMapping("/initial-setup")
    public ResponseEntity<Void> updateInitialSetup(
            Authentication authentication,
            @RequestBody InitialSetupRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        userService.completeInitialSetup(userId, request);
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
            // Map 대신 DTO를 직접 사용하도록 수정
            @RequestBody CreateEncouragementMessageRequest request) {
        Long userId = (Long) authentication.getPrincipal();

        userService.createEncouragementMessage(userId, request);
        return ResponseEntity.ok().build();
    }

    // 응원 문구 전체 가져오기
    @GetMapping("/encouragement")
    public ResponseEntity<List<EncouragementMessageResponse>> getAllEncouragementMessages(
                                                                                           Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        List<EncouragementMessageResponse> messages = userService.getEncouragementMessages(userId);
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/feed-character")
    public ResponseEntity<FeedCharacterResponse> feedCharacter(Authentication authentication) {
        // SecurityContext에 저장된 principal(userId)을 Long으로 캐스팅하여 사용
        Long userId = (Long) authentication.getPrincipal();

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