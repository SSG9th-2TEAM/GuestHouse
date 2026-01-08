package com.ssg9th2team.geharbang.domain.report.host.controller;

import com.ssg9th2team.geharbang.domain.booking.host.support.HostIdentityResolver;
import com.ssg9th2team.geharbang.domain.report.host.ai.HostAiInsightService;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostAiInsightRequest;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostAiInsightResponse;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostForecastResponse;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewAiSummaryRequest;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewAiSummaryResponse;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewReportSummaryResponse;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewReportTrendRow;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostThemeReportResponse;
import com.ssg9th2team.geharbang.domain.report.host.service.HostReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/host/reports")
@RequiredArgsConstructor
public class HostReportController {

    private final HostReportService hostReportService;
    private final HostIdentityResolver hostIdentityResolver;
    private final HostAiInsightService hostAiInsightService;

    @GetMapping("/reviews/summary")
    public HostReviewReportSummaryResponse reviewSummary(
            @RequestParam(required = false) Long accommodationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication authentication
    ) {
        Long hostId = hostIdentityResolver.resolveHostUserId(authentication);
        return hostReportService.getReviewSummary(hostId, accommodationId, from, to);
    }

    @GetMapping("/reviews/trend")
    public List<HostReviewReportTrendRow> reviewTrend(
            @RequestParam(required = false) Long accommodationId,
            @RequestParam(defaultValue = "6") int months,
            Authentication authentication
    ) {
        Long hostId = hostIdentityResolver.resolveHostUserId(authentication);
        return hostReportService.getReviewTrend(hostId, accommodationId, months);
    }

    @GetMapping("/themes/popular")
    public HostThemeReportResponse themePopularity(
            @RequestParam(required = false) Long accommodationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "reservations") String metric,
            Authentication authentication
    ) {
        Long hostId = hostIdentityResolver.resolveHostUserId(authentication);
        return hostReportService.getThemePopularity(hostId, accommodationId, from, to, metric);
    }

    @GetMapping("/forecast/demand")
    public HostForecastResponse forecastDemand(
            @RequestParam(required = false) Long accommodationId,
            @RequestParam(defaultValue = "reservations") String target,
            @RequestParam(defaultValue = "30") int horizonDays,
            @RequestParam(defaultValue = "180") int historyDays,
            Authentication authentication
    ) {
        Long hostId = hostIdentityResolver.resolveHostUserId(authentication);
        return hostReportService.getDemandForecast(hostId, accommodationId, target, horizonDays, historyDays);
    }

    @PostMapping("/reviews/ai-summary")
    public HostReviewAiSummaryResponse reviewAiSummary(
            @RequestBody HostReviewAiSummaryRequest request,
            Authentication authentication
    ) {
        Long hostId = hostIdentityResolver.resolveHostUserId(authentication);
        if (request == null) {
            request = new HostReviewAiSummaryRequest();
        }
        return hostReportService.getReviewAiSummary(
                hostId,
                request.getAccommodationId(),
                request.getFrom(),
                request.getTo()
        );
    }

    /**
     * Host AI insight endpoint for REVIEW/THEME/DEMAND tabs.
     * Chosen to keep existing /reviews/ai-summary stable while allowing tab expansion.
     */
    @PostMapping("/ai-insight")
    public HostAiInsightResponse aiInsight(
            @RequestBody HostAiInsightRequest request,
            Authentication authentication
    ) {
        Long hostId = hostIdentityResolver.resolveHostUserId(authentication);
        if (request == null) {
            request = new HostAiInsightRequest();
        }
        return hostAiInsightService.generate(hostId, request);
    }

}
