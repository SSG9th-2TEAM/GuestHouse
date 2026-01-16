package com.ssg9th2team.geharbang.domain.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiApiClient {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    public String generateContent(String promptText) {
        try {
            String finalUrl = apiUrl.trim() + "?key=" + apiKey.trim();
            log.info("Gemini API Request URL: {}", finalUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            GeminiRequest requestDto = new GeminiRequest(List.of(
                    new Content(List.of(new Part(promptText)))
            ));
            String requestBody = objectMapper.writeValueAsString(requestDto);

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(finalUrl, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseAndSanitize(response.getBody());
            } else {
                throw new RuntimeException("Gemini API Error: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("Gemini Client Error", e);
            throw new RuntimeException("Gemini Call Failed", e);
        }
    }

    private String parseAndSanitize(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");

            if (textNode.isMissingNode()) return "{}"; // NPE 방지: 빈 JSON 반환

            String text = textNode.asText();
            // 마크다운 코드 블록 제거 로직 통합
            return text.replaceAll("```json", "").replaceAll("```", "").trim();
        } catch (Exception e) {
            log.error("Parsing Error", e);
            return "{}"; // 에러 발생 시에도 빈 JSON 반환
        }
    }

    // --- DTO ---
    @Data
    static class GeminiRequest {
        private final List<Content> contents;
    }
    @Data
    static class Content {
        private final List<Part> parts;
    }
    @Data
    static class Part {
        private final String text;
    }
}
