package com.ssg9th2team.geharbang.domain.recommendation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation;
import com.ssg9th2team.geharbang.domain.accommodation.repository.jpa.AccommodationJpaRepository;
import com.ssg9th2team.geharbang.domain.recommendation.dto.AiRecommendationResponse;
import com.ssg9th2team.geharbang.domain.review.repository.jpa.ReviewJpaRepository;
import com.ssg9th2team.geharbang.domain.theme.entity.ThemeCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiRecommendationService {

    private final AccommodationJpaRepository accommodationRepository;
    private final ReviewJpaRepository reviewRepository;
    private final AiSearchLogService searchLogService;
    private final RedissonClient redissonClient;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 병렬 쿼리용 스레드 풀
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    private static final String CACHE_PREFIX = "ai:recommend:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    @Value("${GEMINI_API_KEY:}")
    private String geminiApiKey;

    @Value("${GEMINI_MODEL:gemini-1.5-flash-latest}")
    private String geminiModel;

    @Value("${GEMINI_BASE_URL:https://generativelanguage.googleapis.com/v1beta}")
    private String geminiBaseUrl;

    private static final int MAX_RECOMMENDATIONS = 10;
    private static final int QUERY_LIMIT = 20;

    /**
     * 사용자 자연어 입력을 분석하여 숙소 추천
     * 캐싱 + 병렬 쿼리로 최적화
     */
    public AiRecommendationResponse recommend(String userQuery) {
        long startTime = System.currentTimeMillis();

        // 1. 캐시 확인
        String cacheKey = CACHE_PREFIX + hashQuery(userQuery);
        RBucket<AiRecommendationResponse> cache = redissonClient.getBucket(cacheKey);
        AiRecommendationResponse cached = cache.get();
        if (cached != null) {
            log.info("캐시 히트! 쿼리: {}, 소요시간: {}ms", userQuery, System.currentTimeMillis() - startTime);
            return cached;
        }

        // 2. Gemini API로 테마 + 키워드 분석
        AnalysisResult analysisResult = analyzeUserIntent(userQuery);
        log.info("Gemini 분석 완료: {}ms", System.currentTimeMillis() - startTime);

        // 3. 병렬 검색: 테마 + 키워드(설명/리뷰) 동시 실행
        Set<Long> matchedIds = executeParallelSearch(analysisResult);
        log.info("병렬 검색 완료: {}ms, 결과 수: {}", System.currentTimeMillis() - startTime, matchedIds.size());

        // 4. 결과 조합 및 중복 제거
        List<Accommodation> finalResults = matchedIds.stream()
                .limit(MAX_RECOMMENDATIONS * 2)
                .map(accommodationRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(acc -> acc.getApprovalStatus().name().equals("APPROVED"))
                .sorted((a, b) -> {
                    Double ratingA = a.getRating() != null ? a.getRating() : 0.0;
                    Double ratingB = b.getRating() != null ? b.getRating() : 0.0;
                    return ratingB.compareTo(ratingA);
                })
                .limit(MAX_RECOMMENDATIONS)
                .collect(Collectors.toList());

        // 5. Redis에 검색 로그 저장
        searchLogService.logSearch(userQuery, analysisResult.themes(), analysisResult.confidence());

        // 6. 응답 생성
        List<AiRecommendationResponse.AccommodationSummary> summaries = finalResults.stream()
                .map(this::toAccommodationSummary)
                .collect(Collectors.toList());

        AiRecommendationResponse response = AiRecommendationResponse.builder()
                .query(userQuery)
                .matchedThemes(analysisResult.themes())
                .reasoning(analysisResult.reasoning())
                .confidence(analysisResult.confidence())
                .accommodations(summaries)
                .build();

        // 7. 결과 캐싱
        if (!summaries.isEmpty()) {
            cache.set(response, CACHE_TTL);
        }

        log.info("AI 추천 완료! 쿼리: {}, 총 소요시간: {}ms", userQuery, System.currentTimeMillis() - startTime);
        return response;
    }

    /**
     * 병렬 검색 실행
     */
    private Set<Long> executeParallelSearch(AnalysisResult analysisResult) {
        Set<Long> matchedIds = Collections.synchronizedSet(new LinkedHashSet<>());
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // 1. 테마 기반 검색 (비동기)
        if (!analysisResult.themes().isEmpty()) {
            futures.add(CompletableFuture.runAsync(() -> {
                List<Accommodation> themeMatched = accommodationRepository
                        .findByThemeCategories(analysisResult.themes());
                themeMatched.stream()
                        .limit(QUERY_LIMIT)
                        .forEach(acc -> matchedIds.add(acc.getAccommodationsId()));
            }, executor));
        }

        // 2. 키워드 기반 검색 - 설명 (비동기, 각 키워드 병렬)
        for (String keyword : analysisResult.keywords()) {
            futures.add(CompletableFuture.runAsync(() -> {
                List<Accommodation> keywordMatched = accommodationRepository.findByKeywordInDescription(keyword);
                keywordMatched.stream()
                        .limit(QUERY_LIMIT / 2)
                        .forEach(acc -> matchedIds.add(acc.getAccommodationsId()));
            }, executor));
        }

        // 3. 키워드 기반 검색 - 리뷰 (비동기, 각 키워드 병렬)
        for (String keyword : analysisResult.keywords()) {
            futures.add(CompletableFuture.runAsync(() -> {
                List<Long> reviewMatchedIds = reviewRepository.findAccommodationIdsByKeywordInContent(keyword);
                reviewMatchedIds.stream()
                        .limit(QUERY_LIMIT / 2)
                        .forEach(matchedIds::add);
            }, executor));
        }

        // 모든 병렬 작업 완료 대기 (최대 5초)
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("병렬 검색 일부 타임아웃: {}", e.getMessage());
        }

        return matchedIds;
    }

    /**
     * 쿼리 해시 생성 (캐시 키용)
     */
    private String hashQuery(String query) {
        return Integer.toHexString(query.toLowerCase().trim().hashCode());
    }

    /**
     * Gemini API를 호출하여 사용자 의도 분석 (테마 + 키워드)
     */
    private AnalysisResult analyzeUserIntent(String userQuery) {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            log.warn("Gemini API key not configured, using fallback keyword matching");
            return fallbackKeywordMatching(userQuery);
        }

        try {
            String prompt = buildAnalysisPrompt(userQuery);
            String response = callGeminiApi(prompt);
            return parseGeminiResponse(response);
        } catch (Exception e) {
            log.warn("Gemini API 호출 실패, 키워드 매칭으로 대체: {}", e.getMessage());
            return fallbackKeywordMatching(userQuery);
        }
    }

    private String buildAnalysisPrompt(String userQuery) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("당신은 숙소 추천 전문가입니다. 사용자의 여행 선호도를 분석하여 적합한 테마와 검색 키워드를 추출하세요.\n\n");
        prompt.append("테마 목록:\n");
        for (ThemeCategory category : ThemeCategory.values()) {
            prompt.append("- ").append(category.name()).append(" (").append(category.getKoreanName()).append(")\n");
        }
        prompt.append("\n사용자 입력: \"").append(userQuery).append("\"\n\n");
        prompt.append("반드시 아래 JSON 형식으로만 응답하세요:\n");
        prompt.append(
                "{\"themes\": [\"THEME1\"], \"keywords\": [\"키워드1\", \"키워드2\"], \"confidence\": 0.85, \"reasoning\": \"분석 이유\"}\n");
        prompt.append("themes: 1~2개, keywords: 1~3개 (짧게)");
        return prompt.toString();
    }

    private String callGeminiApi(String prompt) throws JsonProcessingException {
        String url = String.format("%s/models/%s:generateContent?key=%s", geminiBaseUrl, geminiModel, geminiApiKey);

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of("temperature", 0.3));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        return response.getBody();
    }

    private AnalysisResult parseGeminiResponse(String responseBody) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");

        if (textNode.isMissingNode()) {
            throw new IllegalStateException("Gemini 응답에서 텍스트를 찾을 수 없습니다");
        }

        String text = textNode.asText().trim();
        if (text.startsWith("```")) {
            int start = text.indexOf('{');
            int end = text.lastIndexOf('}');
            if (start >= 0 && end > start) {
                text = text.substring(start, end + 1);
            }
        }

        JsonNode analysisNode = objectMapper.readTree(text);

        List<String> themes = new ArrayList<>();
        analysisNode.path("themes").forEach(node -> themes.add(node.asText()));

        List<String> keywords = new ArrayList<>();
        analysisNode.path("keywords").forEach(node -> keywords.add(node.asText()));

        double confidence = analysisNode.path("confidence").asDouble(0.5);
        String reasoning = analysisNode.path("reasoning").asText("AI 분석 결과");

        log.info("Gemini 분석: themes={}, keywords={}", themes, keywords);
        return new AnalysisResult(themes, keywords.stream().limit(3).collect(Collectors.toList()), confidence,
                reasoning);
    }

    private AnalysisResult fallbackKeywordMatching(String userQuery) {
        List<String> matchedThemes = new ArrayList<>();
        List<String> extractedKeywords = new ArrayList<>();
        String query = userQuery.toLowerCase();

        Map<String, List<String>> keywordMap = Map.of(
                "NATURE", List.of("자연", "산", "숲", "풍경", "경치", "바다", "해변"),
                "VIBE", List.of("조용", "힐링", "분위기", "로맨틱", "감성"),
                "ACTIVITY", List.of("스포츠", "레저", "서핑", "등산"),
                "PARTY", List.of("파티", "모임", "축제"),
                "MEETING", List.of("커플", "친구", "가족"),
                "PERSONA", List.of("반려동물", "펫", "강아지"),
                "FACILITY", List.of("수영장", "바베큐", "온천"),
                "FOOD", List.of("맛집", "조식", "음식"),
                "CULTURE", List.of("문화", "역사", "전통"),
                "PLAY", List.of("게임", "오락", "놀이"));

        for (Map.Entry<String, List<String>> entry : keywordMap.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (query.contains(keyword)) {
                    if (!matchedThemes.contains(entry.getKey()) && matchedThemes.size() < 2) {
                        matchedThemes.add(entry.getKey());
                    }
                    if (!extractedKeywords.contains(keyword) && extractedKeywords.size() < 3) {
                        extractedKeywords.add(keyword);
                    }
                }
            }
        }

        if (matchedThemes.isEmpty())
            matchedThemes.add("VIBE");
        if (extractedKeywords.isEmpty()) {
            String[] words = userQuery.split("\\s+");
            for (String word : words) {
                if (word.length() >= 2 && extractedKeywords.size() < 3) {
                    extractedKeywords.add(word);
                }
            }
        }

        return new AnalysisResult(matchedThemes, extractedKeywords, 0.6, "키워드 매칭 결과");
    }

    private AiRecommendationResponse.AccommodationSummary toAccommodationSummary(Accommodation acc) {
        return AiRecommendationResponse.AccommodationSummary.builder()
                .accommodationsId(acc.getAccommodationsId())
                .accommodationsName(acc.getAccommodationsName())
                .city(acc.getCity())
                .district(acc.getDistrict())
                .rating(acc.getRating())
                .reviewCount(acc.getReviewCount())
                .thumbnailUrl(null)
                .themes(List.of())
                .build();
    }

    private record AnalysisResult(List<String> themes, List<String> keywords, Double confidence, String reasoning) {
    }
}
