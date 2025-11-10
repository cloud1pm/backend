package com.cloud1pm.backend.service;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.Tool;
import com.google.genai.types.FunctionDeclaration;
import com.google.genai.types.FunctionResponse;
import com.google.genai.types.FunctionCall;
import com.google.common.collect.ImmutableList;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.*;

@Service
@Slf4j
public class GeminiService {

    private final Client geminiClient;
    private static final String MODEL_NAME = "gemini-2.5-flash";

    public GeminiService(@Value("${gemini.api-key}") String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("Gemini API Key cannot be empty.");
        }
        this.geminiClient = Client.builder().apiKey(apiKey).build();
    }

    /**
     * 감정 분석을 위한 더미 메서드
     */
    public static Map<String, Object> analyzeSentiment(String sentiment, double score) {
        Map<String, Object> result = new HashMap<>();
        result.put("sentiment", sentiment);
        result.put("score", score);
        return result;
    }

    /**
     * Gemini 모델을 사용하여 챗봇 응답과 감정 분석을 동시에 수행합니다.
     */
    public Map<String, Object> generateChatResponseAndAnalyzeSentiment(String userMessage) {
        String finalSentiment = "neutral";
        double finalScore = 0.0;
        String finalBotResponse = "";

        try {
            // 1. 시스템 명령어 및 대화 기록 설정
            String systemInstruction = "당신은 사용자에게 정서적 지지와 공감을 제공하는 따뜻하고 안전한 챗봇입니다. " +
                    "사용자의 기분을 분석하고, 그들의 말에 진심으로 공감하며 부드럽고 긍정적인 방향으로 대화를 이끌어주세요. " +
                    "절대 전문가처럼 진단하거나 조언하지 말고, 항상 듣고 지지하는 자세를 유지하세요. " +
                    "그리고 사용자 메시지에 대해 한국어로 응답을 생성해주세요. " +
                    "반드시 analyzeSentiment 함수를 호출하여 사용자의 감정을 분석한 결과를 제공한 다음, 그 결과와 공감 내용을 포함하는 최종 텍스트 응답을 생성하세요.";

            List<Content> history = new ArrayList<>();
            Content userContent = Content.builder()
                    .parts(ImmutableList.of(Part.builder().text(userMessage).build()))
                    .role("user")
                    .build();
            history.add(userContent);

            // 2. Tool 생성
            Method method = GeminiService.class.getMethod("analyzeSentiment", String.class, double.class);
            FunctionDeclaration functionDeclaration = FunctionDeclaration.fromMethod(method);
            Tool tool = Tool.builder()
                    .functionDeclarations(ImmutableList.of(functionDeclaration))
                    .build();

            // 3. GenerateContentConfig 생성
            // systemInstruction을 Content 객체로 변환
            Content systemInstructionContent = Content.builder()
                    .parts(ImmutableList.of(Part.builder().text(systemInstruction).build()))
                    .role("user")
                    .build();

            GenerateContentConfig config = GenerateContentConfig.builder()
                    .systemInstruction(systemInstructionContent)  // Content 객체 전달
                    .temperature(0.7f)  // float 타입으로 변경
                    .tools(ImmutableList.of(tool))
                    .build();

            // 4. Step 1: Gemini API 호출 - 함수 호출을 요청
            GenerateContentResponse response1 = geminiClient.models.generateContent(MODEL_NAME, history, config);

            // Optional 처리: candidates()가 Optional<List<...>>를 반환
            List<com.google.genai.types.Candidate> candidates1 = response1.candidates().orElse(Collections.emptyList());

            if (candidates1.isEmpty()) {
                log.warn("First API call failed or returned empty candidate.");
                return getFallbackResponse();
            }

            // content()도 Optional<Content>를 반환하므로 처리 필요
            Content content1 = candidates1.get(0).content().orElse(null);
            if (content1 == null) {
                log.warn("First API call returned null content.");
                return getFallbackResponse();
            }

            // parts()도 Optional<List<Part>>를 반환
            List<Part> parts1 = content1.parts().orElse(Collections.emptyList());

            if (parts1.isEmpty()) {
                log.warn("First API call returned empty parts.");
                return getFallbackResponse();
            }

            Part firstPart = parts1.get(0);

            // 5. Function Call 처리 (Step 2 준비)
            Optional<FunctionCall> funcCallOpt = firstPart.functionCall();

            // Optional을 사용하여 안전하게 함수 이름 확인
            boolean isFunctionCall = funcCallOpt.map(FunctionCall::name)
                    .filter("analyzeSentiment"::equals)
                    .isPresent();

            if (isFunctionCall) {
                log.info("Model requested analyzeSentiment function call.");
                FunctionCall funcCall = funcCallOpt.get();

                // A. Function Call Content를 history에 추가 (모델 응답)
                Content functionCallContent = Content.builder()
                        .parts(ImmutableList.of(firstPart))
                        .role("model")
                        .build();
                history.add(functionCallContent);

                // B. Function Arguments 추출 및 로컬 함수 실행
                Map<String, Object> args = funcCall.args().orElse(Collections.emptyMap());

                if (args.containsKey("sentiment") && args.get("sentiment") instanceof String) {
                    finalSentiment = (String) args.get("sentiment");
                }
                if (args.containsKey("score")) {
                    Object scoreObj = args.get("score");
                    if (scoreObj instanceof Number) {
                        finalScore = ((Number) scoreObj).doubleValue();
                    }
                }

                // C. Local Function Execution (Dummy)
                Map<String, Object> functionResult = analyzeSentiment(finalSentiment, finalScore);

                // D. Function Response Content를 history에 추가 (앱 응답)
                Content functionResponseContent = Content.builder()
                        .parts(ImmutableList.of(
                                Part.builder()
                                        .functionResponse(
                                                FunctionResponse.builder()
                                                        .name("analyzeSentiment")
                                                        .response(functionResult)
                                                        .build()
                                        )
                                        .build()
                        ))
                        .role("function")
                        .build();
                history.add(functionResponseContent);

                // 6. Step 2: Gemini API 호출 - 최종 텍스트 응답 요청
                GenerateContentResponse response2 = geminiClient.models.generateContent(MODEL_NAME, history, config);

                List<com.google.genai.types.Candidate> candidates2 = response2.candidates().orElse(Collections.emptyList());

                if (candidates2.isEmpty()) {
                    log.warn("Second API call returned empty candidate.");
                    return getFallbackResponse();
                }

                // 7. 최종 텍스트 응답 추출
                finalBotResponse = extractTextFromResponse(response2);

            } else {
                // 모델이 함수 호출을 건너뛰고 텍스트를 바로 반환한 경우
                Optional<String> textOpt = firstPart.text();
                finalBotResponse = textOpt.orElse("").trim();

                log.warn("Model did not request function call, returning text only: {}", finalBotResponse);
            }

            // 최종 결과 반환
            Map<String, Object> finalResult = new HashMap<>();
            finalResult.put("botResponse", finalBotResponse.isEmpty() ? "응답을 생성하는 중 문제가 발생했습니다." : finalBotResponse);
            finalResult.put("sentiment", finalSentiment);
            finalResult.put("score", finalScore);

            return finalResult;

        } catch (NoSuchMethodException e) {
            log.error("Failed to create tool from method", e);
            return getFallbackResponse();
        } catch (Exception e) {
            log.error("Gemini API call workflow failed", e);
            return getFallbackResponse();
        }
    }

    /**
     * Gemini 응답에서 텍스트 부분을 추출합니다.
     */
    private String extractTextFromResponse(GenerateContentResponse response) {
        StringBuilder builder = new StringBuilder();
        try {
            List<com.google.genai.types.Candidate> candidates = response.candidates().orElse(Collections.emptyList());

            if (candidates.isEmpty()) {
                return "";
            }

            // content()도 Optional<Content>를 반환
            Content content = candidates.get(0).content().orElse(null);
            if (content == null) {
                return "";
            }

            List<Part> parts = content.parts().orElse(Collections.emptyList());

            for (Part p : parts) {
                Optional<String> textOpt = p.text();
                if (textOpt.isPresent() && !textOpt.get().isEmpty()) {
                    builder.append(textOpt.get()).append(" ");
                }
            }
        } catch (Exception e) {
            log.error("Error extracting text from response", e);
        }

        return builder.toString().trim();
    }

    /**
     * 오류 발생 시 기본 응답 반환
     */
    private Map<String, Object> getFallbackResponse() {
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("botResponse", "죄송합니다. 현재 챗봇 서비스에 문제가 발생했습니다. 잠시 후 다시 시도해 주세요.");
        fallback.put("sentiment", "neutral");
        fallback.put("score", 0.0);
        return fallback;
    }
}