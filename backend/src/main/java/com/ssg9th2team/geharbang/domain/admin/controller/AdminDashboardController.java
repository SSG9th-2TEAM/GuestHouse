package com.ssg9th2team.geharbang.domain.admin.controller;

import com.ssg9th2team.geharbang.domain.admin.dto.AdminDashboardSummaryResponse;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminIssueCenterResponse;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminTimeseriesResponse;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminWeeklyReportResponse;
import com.ssg9th2team.geharbang.domain.admin.service.AdminDashboardService;
import com.ssg9th2team.geharbang.domain.admin.support.AdminIdentityResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;
    private final AdminIdentityResolver adminIdentityResolver;

    @GetMapping("/summary")
    public AdminDashboardSummaryResponse getDashboardSummary(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        adminIdentityResolver.resolveAdminUserId(authentication);
        return dashboardService.getDashboardSummary(from, to);
    }

    @GetMapping("/timeseries")
    public AdminTimeseriesResponse getDashboardTimeseries(
            Authentication authentication,
            @RequestParam String metric,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        adminIdentityResolver.resolveAdminUserId(authentication);
        return dashboardService.getTimeseries(metric, from, to);
    }

    @GetMapping("/issues")
    public AdminIssueCenterResponse getIssueCenter(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        adminIdentityResolver.resolveAdminUserId(authentication);
        return dashboardService.getIssueCenter(from, to);
    }

    @GetMapping("/weekly")
    public AdminWeeklyReportResponse getWeeklyReport(
            Authentication authentication,
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        adminIdentityResolver.resolveAdminUserId(authentication);
        return dashboardService.getWeeklyReport(days, from, to);
    }
}
