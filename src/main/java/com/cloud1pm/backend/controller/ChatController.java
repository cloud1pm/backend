package com.cloud1pm.backend.controller;

import com.cloud1pm.backend.dto.*;
import com.cloud1pm.backend.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Profile("backend")
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/message")
    public ResponseEntity<ChatResponse> sendMessage(
            Authentication authentication,
            @RequestBody ChatRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        ChatResponse response = chatService.sendMessage(userId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/message/{sessionId}")
    // [수정]: @AuthenticationPrincipal UserDetails 대신 Authentication 사용
    public ResponseEntity<ChatResponse> processMessage(
            Authentication authentication,
            @PathVariable Long sessionId,
            @RequestBody ChatRequest chatRequest) {
        Long userId = (Long) authentication.getPrincipal();
        ChatResponse response = chatService.processMessage(userId, sessionId, chatRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/emotion-trend")
    public ResponseEntity<EmotionTrendResponse> getEmotionTrend(
            Authentication authentication,
            @RequestParam(defaultValue = "7") int days) {
        Long userId = (Long) authentication.getPrincipal();
        EmotionTrendResponse response = chatService.getEmotionTrend(userId, days);
        return ResponseEntity.ok(response);
    }

    // 1. 새로운 채팅 세션 생성 (채팅 명 설정)
    @PostMapping("/sessions")
    // [수정]: @AuthenticationPrincipal UserDetails 대신 Authentication 사용
    public ResponseEntity<ChatSessionResponse> createNewSession(
            Authentication authentication,
            @RequestBody(required = false) ChatSessionRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        String title = (request != null && request.getTitle() != null) ? request.getTitle() : null;
        ChatSessionResponse session = chatService.createNewSession(userId, title);
        return ResponseEntity.ok(session);
    }

    // 2. 사용자의 채팅 세션 목록 조회 (채팅 목록 보여주기)
    @GetMapping("/sessions")
    // [수정]: @AuthenticationPrincipal UserDetails 대신 Authentication 사용
    public ResponseEntity<List<ChatSessionResponse>> getChatSessions(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        List<ChatSessionResponse> sessions = chatService.getChatSessions(userId);
        return ResponseEntity.ok(sessions);
    }

    // 3. 특정 채팅 세션의 명칭 수정 (채팅 명 설정/수정)
    @PutMapping("/sessions/{sessionId}/title")
    // [수정]: @AuthenticationPrincipal UserDetails 대신 Authentication 사용
    public ResponseEntity<ChatSessionResponse> updateSessionTitle(
            Authentication authentication,
            @PathVariable Long sessionId,
            @RequestBody ChatSessionRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        ChatSessionResponse updatedSession = chatService.updateSessionTitle(sessionId, request.getTitle(), userId);
        return ResponseEntity.ok(updatedSession);
    }

    // 4. 특정 채팅 세션 삭제 (채팅 삭제)
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> deleteSession(
            Authentication authentication,
            @PathVariable Long sessionId) {
        Long userId = (Long) authentication.getPrincipal();
        chatService.deleteSession(sessionId, userId);
        return ResponseEntity.noContent().build();
    }

    // 5. 특정 채팅 세션의 메시지 기록 조회 (채팅 들어가기)
    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<List<ChatMessageResponse>> getChatHistory(
            Authentication authentication,
            @PathVariable Long sessionId) {
        Long userId = (Long) authentication.getPrincipal();
        List<ChatMessageResponse> history = chatService.getChatHistory(sessionId, userId);
        return ResponseEntity.ok(history);
    }
}