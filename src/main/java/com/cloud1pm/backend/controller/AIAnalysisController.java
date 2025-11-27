package com.cloud1pm.backend.controller;

import com.cloud1pm.backend.dto.AIAnalysisRequest;
import com.cloud1pm.backend.dto.AIAnalysisResponse;
import com.cloud1pm.backend.service.GeminiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/internal/ai")
@RequiredArgsConstructor
@Slf4j
@Profile("ai")
public class AIAnalysisController {

    private final GeminiService geminiService;

    @PostMapping("/analyze")
    public ResponseEntity<AIAnalysisResponse> analyzeMessage(@RequestBody AIAnalysisRequest request) {
        log.info("AI Analysis Requested for message: {}", request.getMessage());

        Map<String, Object> result = geminiService.generateChatResponseAndAnalyzeSentiment(request.getMessage());

        AIAnalysisResponse response = AIAnalysisResponse.builder()
                .botResponse((String) result.get("botResponse"))
                .sentiment((String) result.get("sentiment"))
                .score((Double) result.get("score"))
                .build();

        return ResponseEntity.ok(response);
    }
}