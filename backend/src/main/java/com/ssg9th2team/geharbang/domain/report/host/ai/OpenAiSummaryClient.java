package com.ssg9th2team.geharbang.domain.report.host.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewAiSummaryRequest;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewAiSummaryResponse;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewReportRecentRow;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewReportSummaryResponse;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewReportTagRow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class OpenAiSummaryClient implements AiSummaryClient {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String model;
    private final String baseUrl;

    public OpenAiSummaryClient(
            @Value("${OPENAI_API_KEY:}") String apiKey,
            @Value("${OPENAI_MODEL:gpt-4o-mini}") String model,
            @Value("${OPENAI_BASE_URL:https://api.openai.com/v1}") String baseUrl,
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

    @Override
    public HostReviewAiSummaryResponse generate(HostReviewReportSummaryResponse summary, HostReviewAiSummaryRequest request) {
        HostReviewAiSummaryResponse base = new HostReviewAiSummaryResponse();
        base.setAccommodationId(request.getAccommodationId());
        base.setFrom(summary.getFrom());
        base.setTo(summary.getTo());
        base.setGeneratedAt(ZonedDateTime.now(KST).toString());
        base.setOverview(List.of());
        base.setPositives(List.of());
        base.setNegatives(List.of());
        base.setActions(List.of());
        base.setRisks(List.of());

        if (!isConfigured()) {
            throw new HostReportAiException("OpenAI is not configured");
        }

        try {
            Map<String, Object> requestBody = buildRequest(summary, request);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    baseUrl + "/chat/completions",
                    entity,
                    Map.class
            );

            String content = extractContent(response.getBody());
            if (content == null || content.isBlank()) {
                throw new HostReportAiException("OpenAI response missing content");
            }

            return parseResponse(content, base);
        } catch (RestClientException ex) {
            log.warn("OpenAI summary request failed: {}", ex.getMessage());
            throw new HostReportAiException("OpenAI request failed", ex);
        } catch (Exception ex) {
            log.warn("OpenAI summary parsing failed: {}", ex.getMessage());
            throw new HostReportAiException("OpenAI response parsing failed", ex);
        }
    }

    private Map<String, Object> buildRequest(HostReviewReportSummaryResponse summary, HostReviewAiSummaryRequest request) {
        String accommodationName = extractAccommodationName(summary);
        String tagLine = summary.getTopTags() == null ? "" : summary.getTopTags().stream()
                .limit(10)
                .map(tag -> tag.getTagName() + ":" + tag.getCount())
                .collect(Collectors.joining(", "));
        String recentReviews = buildRecentReviews(summary.getRecentReviews());

        String prompt = String.format(Locale.KOREA,
                "숙소명: %s\n기간: %s~%s\n평균평점: %.2f\n리뷰수: %d\n별점분포: %s\nTOP태그: %s\n최근리뷰:\n%s\n\n" +
                        "위 데이터를 기반으로 다음 JSON 형태로 요약해줘.\n" +
                        "{\n" +
                        "  \"overview\": [\"총평 1\", \"총평 2\", \"총평 3\"],\n" +
                        "  \"positives\": [\"좋았던 점 1\", \"좋았던 점 2\", \"좋았던 점 3\"],\n" +
                        "  \"negatives\": [\"개선 포인트 1\", \"개선 포인트 2\", \"개선 포인트 3\"],\n" +
                        "  \"actions\": [\"다음 액션 1\", \"다음 액션 2\", \"다음 액션 3\"],\n" +
                        "  \"risks\": [\"주의 1\", \"주의 2\"]\n" +
                        "}",
                accommodationName,
                summary.getFrom(),
                summary.getTo(),
                summary.getAvgRating() != null ? summary.getAvgRating() : 0.0,
                summary.getReviewCount() != null ? summary.getReviewCount() : 0,
                summary.getRatingDistribution(),
                tagLine,
                recentReviews
        );

        Map<String, Object> system = Map.of("role", "system", "content", "너는 호스트 숙소 리뷰 리포트 분석가다.");
        Map<String, Object> user = Map.of("role", "user", "content", prompt);
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", List.of(system, user));
        body.put("temperature", 0.4);
        body.put("max_tokens", 800);
        return body;
    }

    private String extractAccommodationName(HostReviewReportSummaryResponse summary) {
        if (summary.getRecentReviews() == null || summary.getRecentReviews().isEmpty()) {
            return "해당 숙소";
        }
        String name = summary.getRecentReviews().get(0).getAccommodationName();
        return name == null || name.isBlank() ? "해당 숙소" : name;
    }

    private String buildRecentReviews(List<HostReviewReportRecentRow> reviews) {
        if (reviews == null || reviews.isEmpty()) return "리뷰 데이터 없음";
        return reviews.stream()
                .limit(5)
                .map(review -> "- " + trimContent(review.getContent()))
                .collect(Collectors.joining("\n"));
    }

    private String trimContent(String content) {
        if (content == null) return "";
        String trimmed = content.replaceAll("\\s+", " ").trim();
        if (trimmed.length() > 200) {
            return trimmed.substring(0, 200) + "...";
        }
        return trimmed;
    }

    private String extractContent(Map body) {
        if (body == null) return null;
        Object choices = body.get("choices");
        if (!(choices instanceof List<?> list) || list.isEmpty()) return null;
        Object first = list.get(0);
        if (!(first instanceof Map<?, ?> choice)) return null;
        Object message = choice.get("message");
        if (!(message instanceof Map<?, ?> messageMap)) return null;
        Object content = messageMap.get("content");
        return content == null ? null : content.toString();
    }

    private HostReviewAiSummaryResponse parseResponse(String content, HostReviewAiSummaryResponse base) throws Exception {
        String trimmed = stripCodeFence(content).trim();
        Map<String, List<String>> parsed = objectMapper.readValue(trimmed, new TypeReference<Map<String, List<String>>>() {});

        HostReviewAiSummaryResponse response = new HostReviewAiSummaryResponse();
        response.setAccommodationId(base.getAccommodationId());
        response.setFrom(base.getFrom());
        response.setTo(base.getTo());
        response.setGeneratedAt(base.getGeneratedAt());
        response.setOverview(parsed.getOrDefault("overview", List.of()));
        response.setPositives(parsed.getOrDefault("positives", List.of()));
        response.setNegatives(parsed.getOrDefault("negatives", List.of()));
        response.setActions(parsed.getOrDefault("actions", List.of()));
        response.setRisks(parsed.getOrDefault("risks", List.of()));
        return response;
    }

    private String stripCodeFence(String content) {
        if (content == null) return "";
        String trimmed = content.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }
        int firstBrace = trimmed.indexOf('{');
        int lastBrace = trimmed.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return trimmed.substring(firstBrace, lastBrace + 1);
        }
        return trimmed.replaceAll("```", "");
    }
}
