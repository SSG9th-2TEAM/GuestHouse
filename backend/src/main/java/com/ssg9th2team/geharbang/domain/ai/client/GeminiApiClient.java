package com.ssg9th2team.geharbang.domain.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiApiClient {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    public String generateContent(String prompt) {
        try {
            GeminiRequest request = new GeminiRequest(prompt);
            String requestUrl = apiUrl + "?key=" + apiKey;

            GeminiResponse response = webClientBuilder.build()
                    .post()
                    .uri(requestUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(GeminiResponse.class)
                    .block(); // 동기식 처리를 위해 block() 사용

            if (response != null && !response.getCandidates().isEmpty()) {
                return response.getCandidates().get(0).getContent().getParts().get(0).getText();
            }
        } catch (Exception e) {
            log.error("Gemini API call failed", e);
            throw new RuntimeException("Gemini API call failed", e);
        }
        return null;
    }

    // --- Request DTOs ---
    @Getter
    private static class GeminiRequest {
        private final List<Content> contents;

        public GeminiRequest(String text) {
            this.contents = Collections.singletonList(new Content(new Part(text)));
        }

        @Getter
        @RequiredArgsConstructor
        private static class Content {
            private final List<Part> parts;

            public Content(Part part) {
                this.parts = Collections.singletonList(part);
            }
        }

        @Getter
        @RequiredArgsConstructor
        private static class Part {
            private final String text;
        }
    }

    // --- Response DTOs ---
    @Getter
    private static class GeminiResponse {
        private List<Candidate> candidates;

        @Getter
        private static class Candidate {
            private Content content;
        }

        @Getter
        private static class Content {
            private List<Part> parts;
        }

        @Getter
        private static class Part {
            private String text;
        }
    }
}
