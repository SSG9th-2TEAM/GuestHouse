package com.ssg9th2team.geharbang.domain.dashboard.host.controller;

import com.ssg9th2team.geharbang.domain.dashboard.host.dto.HostDashboardSummaryResponse;
import com.ssg9th2team.geharbang.domain.dashboard.host.dto.TodayScheduleItemResponse;
import com.ssg9th2team.geharbang.domain.dashboard.host.service.HostDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/host")
@RequiredArgsConstructor
public class HostDashboardController {

    private final HostDashboardService hostDashboardService;

    @GetMapping("/dashboard/summary")
    // @PreAuthorize("hasRole('HOST')")
    public HostDashboardSummaryResponse summary(
            @RequestParam int year,
            @RequestParam int month
            // @AuthenticationPrincipal CustomUserDetails user
    ) {
        Long hostId = 1L; // TODO: replace with user.getUserId()
        return hostDashboardService.getSummary(hostId, year, month);
    }

    @GetMapping("/dashboard/today-schedule")
    public List<TodayScheduleItemResponse> todaySchedule(
            @RequestParam LocalDate date
    ) {
        Long hostId = 1L; // TODO: replace with user.getUserId()
        return hostDashboardService.getTodaySchedule(hostId, date);
    }
}
