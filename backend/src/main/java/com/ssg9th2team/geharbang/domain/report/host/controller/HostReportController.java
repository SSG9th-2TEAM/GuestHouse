package com.ssg9th2team.geharbang.domain.report.host.controller;

import com.ssg9th2team.geharbang.domain.auth.repository.UserRepository;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostForecastResponse;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewAiSummaryRequest;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewAiSummaryResponse;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewReportSummaryResponse;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewReportTrendRow;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostThemeReportResponse;
import com.ssg9th2team.geharbang.domain.report.host.ai.HostReportAiException;
import com.ssg9th2team.geharbang.domain.report.host.service.HostReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/host/reports")
@RequiredArgsConstructor
public class HostReportController {

    private final HostReportService hostReportService;
    private final UserRepository userRepository;

    @GetMapping("/reviews/summary")
    public HostReviewReportSummaryResponse reviewSummary(
            @RequestParam(required = false) Long accommodationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication authentication
    ) {
        requireHostRole(authentication);
        Long hostId = resolveHostId(authentication);
        return hostReportService.getReviewSummary(hostId, accommodationId, from, to);
    }

    @GetMapping("/reviews/trend")
    public List<HostReviewReportTrendRow> reviewTrend(
            @RequestParam(required = false) Long accommodationId,
            @RequestParam(defaultValue = "6") int months,
            Authentication authentication
    ) {
        requireHostRole(authentication);
        Long hostId = resolveHostId(authentication);
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
        requireHostRole(authentication);
        Long hostId = resolveHostId(authentication);
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
        requireHostRole(authentication);
        Long hostId = resolveHostId(authentication);
        return hostReportService.getDemandForecast(hostId, accommodationId, target, horizonDays, historyDays);
    }

    @PostMapping("/reviews/ai-summary")
    public HostReviewAiSummaryResponse reviewAiSummary(
            @RequestBody HostReviewAiSummaryRequest request,
            Authentication authentication
    ) {
        requireHostRole(authentication);
        Long hostId = resolveHostId(authentication);
        if (request == null) {
            request = new HostReviewAiSummaryRequest();
        }
        try {
            return hostReportService.getReviewAiSummary(
                    hostId,
                    request.getAccommodationId(),
                    request.getFrom(),
                    request.getTo()
            );
        } catch (HostReportAiException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI summary failed", ex);
        }
    }

    private Long resolveHostId(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"))
                .getId();
    }

    private void requireHostRole(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            throw new AccessDeniedException("HOST role required");
        }
        boolean allowed = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> "ROLE_HOST".equals(role) || "HOST".equals(role));
        if (!allowed) {
            throw new AccessDeniedException("HOST role required");
        }
    }
}
