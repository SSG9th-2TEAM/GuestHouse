package com.ssg9th2team.geharbang.domain.report.host.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostAiInsightSection;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostForecastResponse;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewReportSummaryResponse;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostThemeReportResponse;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostThemeReportRow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class OpenAiInsightClient {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String model;
    private final String baseUrl;

    public OpenAiInsightClient(
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

    public OpenAiInsightResult generateReview(HostReviewReportSummaryResponse summary) {
        String prompt = buildReviewPrompt(summary);
        return request(prompt);
    }

    public OpenAiInsightResult generateTheme(HostThemeReportResponse report) {
        String prompt = buildThemePrompt(report);
        return request(prompt);
    }

    public OpenAiInsightResult generateDemand(HostForecastResponse forecast) {
        String prompt = buildDemandPrompt(forecast);
        return request(prompt);
    }

    private OpenAiInsightResult request(String prompt) {
        if (!isConfigured()) {
            throw new HostReportAiException("OpenAI is not configured");
        }
        try {
            Map<String, Object> system = Map.of(
                    "role", "system",
                    "content", "너는 숙소 운영 리포트를 작성하는 분석가다."
            );
            Map<String, Object> user = Map.of("role", "user", "content", prompt);
            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("messages", List.of(system, user));
            body.put("temperature", 0.3);
            body.put("max_tokens", 900);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    baseUrl + "/chat/completions",
                    entity,
                    Map.class
            );

            String content = stripCodeFence(extractContent(response.getBody()));
            if (content == null || content.isBlank()) {
                throw new HostReportAiException("OpenAI response missing content");
            }
            List<HostAiInsightSection> sections = objectMapper.readValue(content, new TypeReference<List<HostAiInsightSection>>() {});
            OpenAiInsightResult result = new OpenAiInsightResult();
            result.setGeneratedAt(ZonedDateTime.now(KST).toString());
            result.setSections(sections);
            return result;
        } catch (RestClientException ex) {
            log.warn("OpenAI insight request failed: {}", ex.getMessage());
            throw new HostReportAiException("OpenAI request failed", ex);
        } catch (Exception ex) {
            log.warn("OpenAI insight parsing failed: {}", ex.getMessage());
            throw new HostReportAiException("OpenAI response parsing failed", ex);
        }
    }

    private String buildReviewPrompt(HostReviewReportSummaryResponse summary) {
        String tagLine = summary.getTopTags() == null ? "없음" : summary.getTopTags().stream()
                .limit(10)
                .map(tag -> tag.getTagName() + ":" + tag.getCount())
                .collect(Collectors.joining(", "));
        return String.format(Locale.KOREA,
                "다음 리뷰 요약 데이터를 바탕으로 호스트 리포트 인사이트를 생성한다.\n" +
                        "출력은 JSON 배열이며 각 요소는 {\"title\":string,\"items\":string[]} 형식이다.\n" +
                        "섹션 순서: 총평, 좋았던 점, 개선 포인트, 다음 액션, 주의·리스크.\n" +
                        "각 아이템은 근거를 포함하고 과장하지 않는다.\n\n" +
                        "[입력]\n" +
                        "- 리뷰 수: %d\n" +
                        "- 평균 평점: %.2f\n" +
                        "- 평점 분포: %s\n" +
                        "- 상위 태그: %s\n",
                summary.getReviewCount() != null ? summary.getReviewCount() : 0,
                summary.getAvgRating() != null ? summary.getAvgRating() : 0.0,
                summary.getRatingDistribution(),
                tagLine
        );
    }

    private String buildThemePrompt(HostThemeReportResponse report) {
        String rows = report.getRows() == null ? "없음" : report.getRows().stream()
                .limit(10)
                .map(row -> String.format(Locale.KOREA, "%s|예약:%d|매출:%d|숙소:%d",
                        row.getThemeName(),
                        row.getReservationCount() != null ? row.getReservationCount() : 0,
                        row.getRevenueSum() != null ? row.getRevenueSum() : 0,
                        row.getAccommodationCount() != null ? row.getAccommodationCount() : 0))
                .collect(Collectors.joining(", "));
        return String.format(Locale.KOREA,
                "테마 인기 리포트 데이터를 바탕으로 인사이트를 생성한다.\n" +
                        "출력은 JSON 배열이며 각 요소는 {\"title\":string,\"items\":string[]} 형식이다.\n" +
                        "섹션 순서: 핵심 요약, 강점, 개선 포인트, 다음 액션, 모니터링.\n" +
                        "각 아이템은 근거를 포함하고 과장하지 않는다.\n\n" +
                        "[입력]\n" +
                        "- 기간: %s ~ %s\n" +
                        "- 지표: %s\n" +
                        "- 테마 데이터: %s\n",
                report.getFrom(),
                report.getTo(),
                report.getMetric(),
                rows
        );
    }

    private String buildDemandPrompt(HostForecastResponse forecast) {
        return String.format(Locale.KOREA,
                "수요 예측 데이터를 바탕으로 인사이트를 생성한다.\n" +
                        "출력은 JSON 배열이며 각 요소는 {\"title\":string,\"items\":string[]} 형식이다.\n" +
                        "섹션 순서: 핵심 요약, 해석 포인트, 개선 포인트, 운영 액션, 모니터링.\n" +
                        "각 아이템은 근거를 포함하고 과장하지 않는다.\n\n" +
                        "[입력]\n" +
                        "- 타깃: %s\n" +
                        "- 예측 요약: %s\n" +
                        "- MAPE: %s\n" +
                        "- 설명: %s\n",
                forecast.getTarget(),
                forecast.getForecastSummary(),
                forecast.getDiagnostics() != null ? forecast.getDiagnostics().getMape() : null,
                forecast.getExplain()
        );
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

    private String stripCodeFence(String content) {
        if (content == null) return null;
        String trimmed = content.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }
        int firstBrace = trimmed.indexOf('[');
        int lastBrace = trimmed.lastIndexOf(']');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return trimmed.substring(firstBrace, lastBrace + 1);
        }
        return trimmed.replaceAll("```", "");
    }

    public static class OpenAiInsightResult {
        private String generatedAt;
        private List<HostAiInsightSection> sections;

        public String getGeneratedAt() {
            return generatedAt;
        }

        public void setGeneratedAt(String generatedAt) {
            this.generatedAt = generatedAt;
        }

        public List<HostAiInsightSection> getSections() {
            return sections;
        }

        public void setSections(List<HostAiInsightSection> sections) {
            this.sections = sections;
        }
    }
}
