package com.ssg9th2team.geharbang.domain.revenue.host.controller;

import com.ssg9th2team.geharbang.domain.revenue.host.dto.HostRevenueDetailResponse;
import com.ssg9th2team.geharbang.domain.revenue.host.dto.HostRevenueSummaryResponse;
import com.ssg9th2team.geharbang.domain.revenue.host.dto.HostRevenueTrendResponse;
import com.ssg9th2team.geharbang.domain.revenue.host.service.HostRevenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/host/revenue")
@RequiredArgsConstructor
public class HostRevenueController {

    private final HostRevenueService hostRevenueService;

    @GetMapping("/summary")
    // 월 요약: 체크아웃 기준 확정 예약 매출 + 다음달 예상 매출
    public HostRevenueSummaryResponse summary(
            @RequestParam int year,
            @RequestParam int month
    ) {
        // TODO: replace with authenticated user ID once security is wired.
        Long hostId = 1L;
        return hostRevenueService.getSummary(hostId, year, month);
    }

    @GetMapping("/trend")
    // 연도별 월 매출 추이 (1~12월 고정)
    public List<HostRevenueTrendResponse> trend(
            @RequestParam int year
    ) {
        // TODO: replace with authenticated user ID once security is wired.
        Long hostId = 1L;
        return hostRevenueService.getTrend(hostId, year);
    }

    @GetMapping("/details")
    // 기간 내 월별 매출/점유율 상세
    public List<HostRevenueDetailResponse> details(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        // TODO: replace with authenticated user ID once security is wired.
        Long hostId = 1L;
        return hostRevenueService.getDetails(hostId, from, to);
    }
}
