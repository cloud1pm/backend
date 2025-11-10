package com.cloud1pm.backend.controller;

import com.cloud1pm.backend.dto.ChatRequest;
import com.cloud1pm.backend.dto.ChatResponse;
import com.cloud1pm.backend.dto.EmotionTrendResponse;
import com.cloud1pm.backend.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
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

    @GetMapping("/emotion-trend")
    public ResponseEntity<EmotionTrendResponse> getEmotionTrend(
            Authentication authentication,
            @RequestParam(defaultValue = "7") int days) {
        Long userId = (Long) authentication.getPrincipal();
        EmotionTrendResponse response = chatService.getEmotionTrend(userId, days);
        return ResponseEntity.ok(response);
    }
}