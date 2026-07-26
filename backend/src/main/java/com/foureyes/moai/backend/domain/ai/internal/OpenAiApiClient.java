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
public class OpenAiApiClient implements AiApiClient {

    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
    public static final String PROVIDER_NAME = "OPENAI";

    private final WebClient webClient;

    @Value("${OPENAI_API_KEY}")
    private String openAiApiKey;

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public Mono<String> generate(String modelId, String prompt) {
        return chatCompletions(OPENAI_URL, modelId, prompt);
    }

    /** OpenAI chat.completions (풀 URL, Bearer 헤더) */
    public Mono<String> chatCompletions(String fullApiUrl, String modelId, String prompt) {
        Map<String, Object> body = Map.of(
            "model", modelId,
            "messages", List.of(
                Map.of("role", "system",
                    "content", "Return ONLY a valid JSON array (UTF-8). No markdown code fences, no extra text."),
                Map.of("role", "user", "content", prompt)
            ),
            "max_tokens", 4000,
            "temperature", 0.3
        );

        log.info("OpenAI 호출: {}", fullApiUrl);


        return webClient.post()
            .uri(fullApiUrl)
            .headers(h -> h.setBearerAuth(openAiApiKey))
            .bodyValue(body)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .map(this::extractTextFromOpenAiResponse);
    }

    /** OpenAI 응답 텍스트 추출 → JSON Array 문자열 */
    private String extractTextFromOpenAiResponse(JsonNode response) {
        try {
            String raw = response.get("choices").get(0).get("message").get("content").asText();
            return raw.trim()
                .replace("```json", "")
                .replace("```", "");
        } catch (Exception e) {
            log.warn("OpenAI 응답 파싱 실패, 빈 배열로 대체");
            return "[]";
        }
    }
}
