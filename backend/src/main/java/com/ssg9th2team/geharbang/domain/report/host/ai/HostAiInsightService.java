package com.ssg9th2team.geharbang.domain.report.host.ai;

import com.ssg9th2team.geharbang.domain.report.host.dto.HostAiInsightRequest;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostAiInsightResponse;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostAiInsightSection;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostForecastResponse;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewAiSummaryRequest;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewAiSummaryResponse;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewReportSummaryResponse;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostThemeReportResponse;
import com.ssg9th2team.geharbang.domain.report.host.service.HostReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class HostAiInsightService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final String CACHE_NAME = "hostAiInsight";

    private final HostReportService hostReportService;
    private final RuleBasedSummaryClient ruleBasedSummaryClient;
    private final MockAiSummaryClient mockAiSummaryClient;
    private final OpenAiSummaryClient openAiSummaryClient;
    private final OpenAiInsightClient openAiInsightClient;
    private final CacheManager cacheManager;

    @Value("${ai.summary.provider:RULE}")
    private String provider;

    public HostAiInsightResponse generate(Long hostId, HostAiInsightRequest request) {
        HostAiInsightTab tab = parseTab(request.getTab());
        String cacheKey = buildCacheKey(hostId, request, tab);
        if (!request.isForceRefresh()) {
            HostAiInsightResponse cached = getCached(cacheKey, request);
            if (cached != null) return cached;
        }

        Provider selected = parseProvider(provider);
        HostAiInsightResponse response;
        if (selected == Provider.MOCK) {
            response = buildMock(tab, request, hostId);
            response.setEngine("MOCK");
            response.setFallbackUsed(false);
        } else if (selected == Provider.OPENAI) {
            response = generateWithOpenAi(tab, request, hostId);
        } else {
            response = buildRule(tab, request, hostId);
            response.setEngine("RULE");
            response.setFallbackUsed(false);
        }

        putCache(cacheKey, response, request);
        return response;
    }

    private HostAiInsightResponse generateWithOpenAi(HostAiInsightTab tab, HostAiInsightRequest request, Long hostId) {
        if (!openAiSummaryClient.isConfigured()) {
            HostAiInsightResponse fallback = buildRule(tab, request, hostId);
            fallback.setEngine("RULE");
            fallback.setFallbackUsed(true);
            return fallback;
        }

        try {
            HostAiInsightResponse response = buildOpenAi(tab, request, hostId);
            if (response.getSections() == null || response.getSections().isEmpty()) {
                throw new HostReportAiException("OpenAI insight empty");
            }
            response.setEngine("OPENAI");
            response.setFallbackUsed(false);
            return response;
        } catch (Exception ex) {
            log.info("OpenAI insight failed; falling back to RULE. reason={}", ex.getMessage());
            HostAiInsightResponse fallback = buildRule(tab, request, hostId);
            fallback.setEngine("RULE");
            fallback.setFallbackUsed(true);
            return fallback;
        }
    }

    private HostAiInsightResponse buildRule(HostAiInsightTab tab, HostAiInsightRequest request, Long hostId) {
        RuleBasedInsightComposer composer = new RuleBasedInsightComposer(ruleBasedSummaryClient);
        List<HostAiInsightSection> sections;
        if (tab == HostAiInsightTab.REVIEW) {
            HostReviewReportSummaryResponse summary = hostReportService.getReviewSummary(
                    hostId,
                    request.getAccommodationId(),
                    request.getFrom(),
                    request.getTo()
            );
            sections = composer.buildReviewSections(summary);
        } else if (tab == HostAiInsightTab.THEME) {
            HostThemeReportResponse report = hostReportService.getThemePopularity(
                    hostId,
                    request.getAccommodationId(),
                    request.getFrom(),
                    request.getTo(),
                    request.getMetric()
            );
            sections = composer.buildThemeSections(report);
        } else {
            HostForecastResponse forecast = hostReportService.getDemandForecast(
                    hostId,
                    request.getAccommodationId(),
                    safeTarget(request.getTarget()),
                    safeInt(request.getHorizonDays(), 30),
                    safeInt(request.getHistoryDays(), 180)
            );
            sections = composer.buildDemandSections(forecast);
        }
        return buildResponse(sections);
    }

    private HostAiInsightResponse buildOpenAi(HostAiInsightTab tab, HostAiInsightRequest request, Long hostId) {
        if (tab == HostAiInsightTab.REVIEW) {
            HostReviewReportSummaryResponse summary = hostReportService.getReviewSummary(
                    hostId,
                    request.getAccommodationId(),
                    request.getFrom(),
                    request.getTo()
            );
            HostReviewAiSummaryRequest aiRequest = new HostReviewAiSummaryRequest();
            aiRequest.setAccommodationId(request.getAccommodationId());
            aiRequest.setFrom(summary.getFrom());
            aiRequest.setTo(summary.getTo());
            HostReviewAiSummaryResponse aiResponse = openAiSummaryClient.generate(summary, aiRequest);
            List<HostAiInsightSection> sections = mapReviewSections(aiResponse);
            return buildResponse(sections);
        }

        if (tab == HostAiInsightTab.THEME) {
            HostThemeReportResponse report = hostReportService.getThemePopularity(
                    hostId,
                    request.getAccommodationId(),
                    request.getFrom(),
                    request.getTo(),
                    request.getMetric()
            );
            OpenAiInsightClient.OpenAiInsightResult result = openAiInsightClient.generateTheme(report);
            return buildResponse(result.getSections(), result.getGeneratedAt());
        }

        HostForecastResponse forecast = hostReportService.getDemandForecast(
                hostId,
                request.getAccommodationId(),
                safeTarget(request.getTarget()),
                safeInt(request.getHorizonDays(), 30),
                safeInt(request.getHistoryDays(), 180)
        );
        OpenAiInsightClient.OpenAiInsightResult result = openAiInsightClient.generateDemand(forecast);
        return buildResponse(result.getSections(), result.getGeneratedAt());
    }

    private HostAiInsightResponse buildMock(HostAiInsightTab tab, HostAiInsightRequest request, Long hostId) {
        RuleBasedInsightComposer composer = new RuleBasedInsightComposer(ruleBasedSummaryClient);
        if (tab == HostAiInsightTab.REVIEW) {
            HostReviewReportSummaryResponse summary = hostReportService.getReviewSummary(
                    hostId,
                    request.getAccommodationId(),
                    request.getFrom(),
                    request.getTo()
            );
            HostReviewAiSummaryRequest aiRequest = new HostReviewAiSummaryRequest();
            aiRequest.setAccommodationId(request.getAccommodationId());
            aiRequest.setFrom(summary.getFrom());
            aiRequest.setTo(summary.getTo());
            HostReviewAiSummaryResponse mock = mockAiSummaryClient.generate(summary, aiRequest);
            return buildResponse(mapReviewSections(mock));
        }
        if (tab == HostAiInsightTab.THEME) {
            HostThemeReportResponse report = hostReportService.getThemePopularity(
                    hostId,
                    request.getAccommodationId(),
                    request.getFrom(),
                    request.getTo(),
                    request.getMetric()
            );
            return buildResponse(composer.buildThemeSections(report));
        }
        HostForecastResponse forecast = hostReportService.getDemandForecast(
                hostId,
                request.getAccommodationId(),
                safeTarget(request.getTarget()),
                safeInt(request.getHorizonDays(), 30),
                safeInt(request.getHistoryDays(), 180)
        );
        return buildResponse(composer.buildDemandSections(forecast));
    }

    private List<HostAiInsightSection> mapReviewSections(HostReviewAiSummaryResponse response) {
        List<HostAiInsightSection> sections = new ArrayList<>();
        sections.add(section("총평", response.getOverview()));
        sections.add(section("좋았던 점", response.getPositives()));
        sections.add(section("개선 포인트", response.getNegatives()));
        sections.add(section("다음 액션", response.getActions()));
        sections.add(section("주의·리스크", response.getRisks()));
        return sections;
    }

    private HostAiInsightSection section(String title, List<String> items) {
        HostAiInsightSection section = new HostAiInsightSection();
        section.setTitle(title);
        section.setItems(items);
        return section;
    }

    private HostAiInsightResponse buildResponse(List<HostAiInsightSection> sections) {
        return buildResponse(sections, ZonedDateTime.now(KST).toString());
    }

    private HostAiInsightResponse buildResponse(List<HostAiInsightSection> sections, String generatedAt) {
        HostAiInsightResponse response = new HostAiInsightResponse();
        response.setGeneratedAt(generatedAt);
        response.setSections(normalizeSections(sections));
        return response;
    }

    private HostAiInsightResponse copyResponse(HostAiInsightResponse response) {
        HostAiInsightResponse copy = new HostAiInsightResponse();
        copy.setEngine(response.getEngine());
        copy.setFallbackUsed(response.isFallbackUsed());
        copy.setGeneratedAt(response.getGeneratedAt());
        if (response.getSections() != null) {
            List<HostAiInsightSection> sections = new ArrayList<>();
            for (HostAiInsightSection section : response.getSections()) {
                HostAiInsightSection copySection = new HostAiInsightSection();
                copySection.setTitle(section.getTitle());
                copySection.setItems(section.getItems() != null ? new ArrayList<>(section.getItems()) : List.of());
                sections.add(copySection);
            }
            copy.setSections(sections);
        }
        return copy;
    }

    private HostAiInsightResponse getCached(String cacheKey, HostAiInsightRequest request) {
        if (!isCacheEligible(request)) return null;
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache == null) return null;
        HostAiInsightResponse cached = cache.get(cacheKey, HostAiInsightResponse.class);
        return cached == null ? null : copyResponse(cached);
    }

    private void putCache(String cacheKey, HostAiInsightResponse response, HostAiInsightRequest request) {
        if (!isCacheEligible(request)) return;
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache == null) return;
        cache.put(cacheKey, copyResponse(response));
    }

    private boolean isCacheEligible(HostAiInsightRequest request) {
        if (request == null) return false;
        LocalDate from = request.getFrom();
        LocalDate to = request.getTo();
        if (from == null || to == null) return true;
        if (to.isBefore(from)) return false;
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        if (days == 7 || days == 30) return true;
        if (from.getDayOfMonth() == 1) {
            LocalDate lastDay = from.with(TemporalAdjusters.lastDayOfMonth());
            return lastDay.equals(to);
        }
        return false;
    }

    private List<HostAiInsightSection> normalizeSections(List<HostAiInsightSection> sections) {
        if (sections == null) return List.of();
        List<HostAiInsightSection> normalized = new ArrayList<>();
        for (HostAiInsightSection section : sections) {
            HostAiInsightSection copy = new HostAiInsightSection();
            copy.setTitle(section.getTitle());
            List<String> items = section.getItems() == null ? List.of() : section.getItems();
            List<String> normalizedItems = new ArrayList<>();
            for (String item : items) {
                normalizedItems.add(normalizeLine(item));
            }
            copy.setItems(normalizedItems);
            normalized.add(copy);
        }
        return normalized;
    }

    private String normalizeLine(String line) {
        if (line == null || line.isBlank()) {
            return "데이터 부족||데이터 부족";
        }
        if (line.contains("||")) {
            return line;
        }
        String marker = "— 근거:";
        if (line.contains(marker)) {
            String[] parts = line.split(marker, 2);
            String main = parts[0].trim();
            String evidence = parts.length > 1 ? parts[1].trim() : "데이터 부족";
            return main + "||" + evidence;
        }
        return line.trim() + "||데이터 부족";
    }

    private String buildCacheKey(Long hostId, HostAiInsightRequest request, HostAiInsightTab tab) {
        return String.join("|",
                String.valueOf(hostId),
                String.valueOf(request.getAccommodationId()),
                String.valueOf(request.getFrom()),
                String.valueOf(request.getTo()),
                tab.name(),
                safeString(request.getMetric()),
                safeString(request.getTarget()),
                String.valueOf(request.getHorizonDays()),
                String.valueOf(request.getHistoryDays())
        );
    }

    private String safeString(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private int safeInt(Integer value, int fallback) {
        if (value == null || value <= 0) return fallback;
        return value;
    }

    private String safeTarget(String target) {
        return Objects.equals(target, "revenue") ? "revenue" : "reservations";
    }

    private HostAiInsightTab parseTab(String raw) {
        if (raw == null || raw.isBlank()) return HostAiInsightTab.REVIEW;
        try {
            return HostAiInsightTab.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return HostAiInsightTab.REVIEW;
        }
    }

    private Provider parseProvider(String raw) {
        if (raw == null || raw.isBlank()) return Provider.RULE;
        try {
            return Provider.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return Provider.RULE;
        }
    }

    private enum Provider {
        RULE,
        OPENAI,
        MOCK
    }
}
