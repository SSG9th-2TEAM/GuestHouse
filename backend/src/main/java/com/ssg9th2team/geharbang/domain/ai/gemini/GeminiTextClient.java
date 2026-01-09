package com.ssg9th2team.geharbang.domain.ai.gemini;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class GeminiTextClient {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String apiKey;
    private final String model;
    private final String baseUrl;

    public GeminiTextClient(
            @Value("${GEMINI_API_KEY:}") String apiKey,
            @Value("${GEMINI_MODEL:gemini-2.0-flash}") String model,
            @Value("${GEMINI_BASE_URL:https://generativelanguage.googleapis.com/v1beta}") String baseUrl,
            @Value("${ai.summary.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${ai.summary.read-timeout-ms:8000}") int readTimeoutMs
    ) {
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl.replaceAll("/$", "");
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        this.restTemplate = new RestTemplate(factory);
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public TextCompletionResult generateSuggestion(String prompt, String languageTag) {
        if (!isConfigured()) {
            throw new IllegalStateException("Gemini API key is not configured.");
        }
        try {
            Map<String, Object> generationConfig = Map.of(
                    "temperature", 0.5,
                    "maxOutputTokens", 2048
            );

            Map<String, Object> body = Map.of(
                    "contents", List.of(Map.of(
                            "role", "user",
                            "parts", List.of(Map.of("text", buildPrompt(prompt, languageTag)))
                    )),
                    "generationConfig", generationConfig
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String url = String.format("%s/models/%s:generateContent?key=%s", baseUrl, model, apiKey);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            return parseResponse(response.getBody());
        } catch (HttpStatusCodeException ex) {
            log.warn("Gemini text request failed: status={} body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new IllegalStateException("Gemini request failed", ex);
        } catch (RestClientException ex) {
            log.warn("Gemini text request failed: {}", ex.getMessage());
            throw new IllegalStateException("Gemini request failed", ex);
        } catch (JsonProcessingException ex) {
            log.warn("Gemini text parsing failed: {}", ex.getMessage());
            throw new IllegalStateException("Gemini response parsing failed", ex);
        }
    }

    private String buildPrompt(String contextPrompt, String languageTag) {
        return "당신은 전문 숙소 카피라이터입니다. 아래 힌트를 참고하여 숙소 이름과 매력적인 소개문을 작성하세요.\n\n"
                + "## 규칙\n"
                + "1. 반드시 아래 JSON 형식으로만 응답하세요. 다른 텍스트는 포함하지 마세요.\n"
                + "2. description은 반드시 10~15문장으로 풍부하고 상세하게 작성하세요.\n"
                + "3. 다음 내용을 모두 포함하세요:\n"
                + "   - 숙소의 첫인상과 전체적인 분위기\n"
                + "   - 인테리어 특징과 감성적인 포인트\n"
                + "   - 위치적 장점 (교통, 접근성)\n"
                + "   - 주변 관광지, 맛집, 명소 소개\n"
                + "   - 제공되는 편의시설과 서비스\n"
                + "   - 추천 여행객 유형 (커플, 가족, 친구, 비즈니스 등)\n"
                + "   - 계절별 매력 또는 특별한 경험\n"
                + "   - 호스트의 환대와 마무리 멘트\n"
                + "4. 감성적이고 따뜻한 톤으로 작성하세요.\n"
                + "5. confidence는 0과 1 사이 숫자입니다.\n"
                + "6. 응답 언어: " + languageTag + "\n\n"
                + "## 출력 형식 (이 형식만 출력)\n"
                + "{\"name\":\"숙소이름\",\"description\":\"소개문\",\"confidence\":0.9}\n\n"
                + "## 힌트\n"
                + contextPrompt;
    }

    private TextCompletionResult parseResponse(String body) throws JsonProcessingException {
        if (body == null || body.isBlank()) {
            throw new IllegalStateException("Gemini response is empty");
        }
        JsonNode root = objectMapper.readTree(body);
        JsonNode candidateNode = root.path("candidates").path(0);
        JsonNode contentNode = candidateNode.path("content").path("parts").path(0).path("text");
        if (contentNode.isMissingNode()) {
            throw new IllegalStateException("Gemini response missing content");
        }

        String text = contentNode.asText().trim();

        // ```json ... ``` 블록 처리
        if (text.contains("```json")) {
            int start = text.indexOf("```json") + 7;
            int end = text.indexOf("```", start);
            if (end > start) {
                text = text.substring(start, end).trim();
            }
        } else if (text.contains("```")) {
            int start = text.indexOf("```") + 3;
            int end = text.indexOf("```", start);
            if (end > start) {
                text = text.substring(start, end).trim();
            }
        }

        // JSON 객체만 추출 (앞뒤 텍스트 제거)
        int jsonStart = text.indexOf("{");
        int jsonEnd = text.lastIndexOf("}");
        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            text = text.substring(jsonStart, jsonEnd + 1);
        }

        log.debug("Parsed JSON text: {}", text);
        SuggestionPayload payload = objectMapper.readValue(text, SuggestionPayload.class);

        JsonNode usageNode = root.path("usageMetadata");
        long promptTokens = usageNode.path("promptTokenCount").asLong(0);
        long completionTokens = usageNode.path("candidatesTokenCount").asLong(0);
        long totalTokens = usageNode.path("totalTokenCount").asLong(promptTokens + completionTokens);
        return new TextCompletionResult(
                payload.getName(),
                payload.getDescription(),
                payload.getConfidence(),
                model,
                new TokenUsage(promptTokens, completionTokens, totalTokens),
                ZonedDateTime.now(KST).toString()
        );
    }

    @Getter
    public static class TextCompletionResult {
        private final String name;
        private final String description;
        private final Double confidence;
        private final String model;
        private final TokenUsage tokenUsage;
        private final String generatedAt;

        public TextCompletionResult(String name, String description, Double confidence, String model, TokenUsage tokenUsage, String generatedAt) {
            this.name = name;
            this.description = description;
            this.confidence = confidence;
            this.model = model;
            this.tokenUsage = tokenUsage;
            this.generatedAt = generatedAt;
        }
    }

    @Getter
    public static class TokenUsage {
        private final long promptTokens;
        private final long completionTokens;
        private final long totalTokens;

        public TokenUsage(long promptTokens, long completionTokens, long totalTokens) {
            this.promptTokens = promptTokens;
            this.completionTokens = completionTokens;
            this.totalTokens = totalTokens;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    private static class SuggestionPayload {
        private String name;
        private String description;
        private Double confidence;
    }
}
