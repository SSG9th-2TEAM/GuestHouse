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
    private final RestTemplate restTemplate = new RestTemplate(); // WebClient 대신 RestTemplate 사용

    public String generateContent(String promptText) {
        try {
            // 1. URL 설정 (공백 제거)
            String finalUrl = apiUrl.trim() + "?key=" + apiKey.trim();
            log.info("Gemini API Request URL: {}", finalUrl);

            // 2. 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 3. 바디 설정
            GeminiRequest requestDto = new GeminiRequest(List.of(
                    new Content(List.of(new Part(promptText)))
            ));
            String requestBody = objectMapper.writeValueAsString(requestDto);

            // 4. 요청 보내기 (RestTemplate은 URL을 안전하게 처리함)
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

            if (textNode.isMissingNode()) return null;

            String text = textNode.asText();
            // 마크다운 코드 블록 제거
            return text.replaceAll("```json", "").replaceAll("```", "").trim();
        } catch (Exception e) {
            log.error("Parsing Error", e);
            return null;
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