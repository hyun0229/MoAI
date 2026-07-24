package com.foureyes.moai.backend.domain.ai.internal;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiApiClient implements AiApiClient {

    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";

    private final WebClient webClient;

    @Value("${GEMINI_API_KEY}")
    private String geminiApiKey;

    @Override
    public AiProvider getProvider() {
        return AiProvider.GEMINI;
    }

    @Override
    public Mono<String> generate(String modelId, String prompt) {
        String fullApiUrl = GEMINI_BASE_URL + modelId + ":generateContent";
        return generateContent(fullApiUrl, prompt);
    }

    /** Gemini 호출 (풀 URL + key 쿼리) */
    public Mono<String> generateContent(String fullApiUrl, String prompt) {
        Map<String, Object> requestBody = Map.of(
            "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
            "generationConfig", Map.of("responseMimeType", "application/json")
        );

        log.info("Gemini 호출: {}", fullApiUrl);
        return webClient.post()
            .uri(fullApiUrl + "?key=" + geminiApiKey)
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .map(this::extractTextFromGeminiResponse);
    }

    /** Gemini 응답에서 텍스트(JSON 문자열) 추출 */
    private String extractTextFromGeminiResponse(JsonNode response) {
        try {
            String raw = response.get("candidates").get(0).get("content").get("parts").get(0).get("text").asText();
            return raw.trim()
                .replace("```json", "")
                .replace("```", "");
        } catch (Exception e) {
            log.warn("Gemini 응답 파싱 실패, 빈 배열로 대체");
            return "[]";
        }
    }
}
