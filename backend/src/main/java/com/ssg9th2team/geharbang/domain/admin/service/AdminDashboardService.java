package com.ssg9th2team.geharbang.domain.admin.service;

import com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation;
import com.ssg9th2team.geharbang.domain.accommodation.entity.ApprovalStatus;
import com.ssg9th2team.geharbang.domain.accommodation.repository.jpa.AccommodationJpaRepository;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminAccommodationSummary;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminDashboardSummaryResponse;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminIssueCenterResponse;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminReportSummary;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminTimeseriesPoint;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminTimeseriesResponse;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminWeeklyReportResponse;
import com.ssg9th2team.geharbang.domain.admin.entity.PlatformDailyStats;
import com.ssg9th2team.geharbang.domain.admin.repository.mybatis.AdminDashboardMapper;
import com.ssg9th2team.geharbang.domain.admin.repository.PlatformDailyStatsRepository;
import com.ssg9th2team.geharbang.domain.auth.entity.User;
import com.ssg9th2team.geharbang.domain.auth.repository.UserRepository;
import com.ssg9th2team.geharbang.domain.report.entity.ReviewReport;
import com.ssg9th2team.geharbang.domain.report.repository.jpa.ReviewReportJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    @Value("${host.platform.fee-rate:0.04}")
    private double platformFeeRate;

    private final AccommodationJpaRepository accommodationRepository;
    private final ReviewReportJpaRepository reportRepository;
    private final PlatformDailyStatsRepository statsRepository;
    private final AdminDashboardMapper dashboardMapper;
    private final UserRepository userRepository;

    public AdminDashboardSummaryResponse getDashboardSummary(LocalDate from, LocalDate to) {
        LocalDate startDate = from != null ? from : LocalDate.now();
        LocalDate endDate = to != null ? to : LocalDate.now();
        List<PlatformDailyStats> stats = statsRepository.findByStatDateBetweenOrderByStatDateAsc(startDate, endDate);

        long reservationCount = stats.stream()
                .mapToLong(PlatformDailyStats::getTotalReservations)
                .sum();
        long paymentSuccessAmount = stats.stream()
                .mapToLong(PlatformDailyStats::getTotalRevenue)
                .sum();
        long paymentFailureCount = stats.stream()
                .mapToLong(PlatformDailyStats::getReservationsFailed)
                .sum();
        long refundRequestCount = stats.stream()
                .mapToLong(PlatformDailyStats::getRefundCount)
                .sum();

        long pendingAccommodations = accommodationRepository.count(approvalEquals(ApprovalStatus.PENDING));
        long openReports = reportRepository.count(reportStateEquals("WAIT"));
        long platformFeeAmount = Math.round(paymentSuccessAmount * platformFeeRate);

        List<AdminAccommodationSummary> pendingList = dashboardMapper.selectPendingAccommodations(5);

        List<AdminReportSummary> openReportList = reportRepository
                .findAll(reportStateEquals("WAIT"),
                        PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt")))
                .stream()
                .map(this::toReportSummary)
                .toList();

        return new AdminDashboardSummaryResponse(
                pendingAccommodations,
                openReports,
                reservationCount,
                paymentSuccessAmount,
                platformFeeRate,
                platformFeeAmount,
                paymentFailureCount,
                refundRequestCount,
                pendingList,
                openReportList
        );
    }

    public AdminTimeseriesResponse getTimeseries(String metric, LocalDate from, LocalDate to) {
        LocalDate startDate = from != null ? from : LocalDate.now().minusDays(6);
        LocalDate endDate = to != null ? to : LocalDate.now();
        List<PlatformDailyStats> stats = statsRepository.findByStatDateBetweenOrderByStatDateAsc(startDate, endDate);

        List<AdminTimeseriesPoint> points = stats.stream()
                .map(stat -> new AdminTimeseriesPoint(stat.getStatDate(), resolveMetricValue(metric, stat)))
                .toList();

        return new AdminTimeseriesResponse(metric, points);
    }

    public AdminIssueCenterResponse getIssueCenter(LocalDate from, LocalDate to) {
        LocalDate startDate = from != null ? from : LocalDate.now();
        LocalDate endDate = to != null ? to : LocalDate.now();
        List<PlatformDailyStats> stats = statsRepository.findByStatDateBetweenOrderByStatDateAsc(startDate, endDate);

        long paymentFailureCount = stats.stream()
                .mapToLong(PlatformDailyStats::getReservationsFailed)
                .sum();
        long refundCount = stats.stream()
                .mapToLong(PlatformDailyStats::getRefundCount)
                .sum();

        long pendingAccommodations = accommodationRepository.count(approvalEquals(ApprovalStatus.PENDING));
        long openReports = reportRepository.count(reportStateEquals("WAIT"));

        List<AdminAccommodationSummary> pendingList = dashboardMapper.selectPendingAccommodations(10);
        List<AdminReportSummary> openReportList = reportRepository
                .findAll(reportStateEquals("WAIT"),
                        PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")))
                .stream()
                .map(this::toReportSummary)
                .toList();

        return new AdminIssueCenterResponse(
                pendingAccommodations,
                openReports,
                refundCount,
                paymentFailureCount,
                pendingList,
                openReportList
        );
    }

    public AdminWeeklyReportResponse getWeeklyReport(int days, LocalDate from, LocalDate to) {
        int rangeDays = days > 0 ? days : 7;
        LocalDate endDate = to != null ? to : LocalDate.now();
        LocalDate startDate = from != null ? from : endDate.minusDays(rangeDays - 1);
        List<PlatformDailyStats> stats = statsRepository.findByStatDateBetweenOrderByStatDateAsc(startDate, endDate);
        boolean statsReady = !stats.isEmpty();

        long reservationCount = stats.stream()
                .mapToLong(PlatformDailyStats::getTotalReservations)
                .sum();
        long paymentSuccessCount = stats.stream()
                .mapToLong(PlatformDailyStats::getReservationsSuccess)
                .sum();
        long cancelCount = stats.stream()
                .mapToLong(PlatformDailyStats::getCancelCount)
                .sum();
        long refundCount = stats.stream()
                .mapToLong(PlatformDailyStats::getRefundCount)
                .sum();
        long refundAmount = stats.stream()
                .mapToLong(PlatformDailyStats::getRefundAmount)
                .sum();
        long newHosts = stats.stream()
                .mapToLong(PlatformDailyStats::getNewHosts)
                .sum();
        long newAccommodations = stats.stream()
                .mapToLong(PlatformDailyStats::getNewAccommodations)
                .sum();

        long pendingAccommodations = accommodationRepository.count(approvalEquals(ApprovalStatus.PENDING));

        LocalDateTime startAt = startDate.atStartOfDay();
        LocalDateTime endAt = endDate.plusDays(1).atStartOfDay().minusNanos(1);
        long newUsers = userRepository.count(createdBetween(startAt, endAt));

        Map<LocalDate, PlatformDailyStats> statMap = new HashMap<>();
        for (PlatformDailyStats stat : stats) {
            statMap.put(stat.getStatDate(), stat);
        }
        List<AdminTimeseriesPoint> revenueSeries = startDate.datesUntil(endDate.plusDays(1))
                .map(date -> {
                    PlatformDailyStats stat = statMap.get(date);
                    long value = stat != null ? stat.getTotalRevenue() : 0L;
                    return new AdminTimeseriesPoint(date, value);
                })
                .toList();

        return new AdminWeeklyReportResponse(
                startDate,
                endDate,
                rangeDays,
                statsReady,
                reservationCount,
                paymentSuccessCount,
                cancelCount,
                refundCount,
                refundAmount,
                newUsers,
                newHosts,
                newAccommodations,
                pendingAccommodations,
                revenueSeries
        );
    }

    private Specification<Accommodation> approvalEquals(ApprovalStatus status) {
        return (root, query, cb) -> cb.equal(root.get("approvalStatus"), status);
    }

    private Specification<ReviewReport> reportStateEquals(String state) {
        return (root, query, cb) -> cb.equal(root.get("state"), state);
    }

    private AdminReportSummary toReportSummary(ReviewReport report) {
        return new AdminReportSummary(
                report.getReportId(),
                "REVIEW",
                report.getState(),
                report.getReason(),
                report.getCreatedAt()
        );
    }

    private Long resolveMetricValue(String metric, PlatformDailyStats stat) {
        if (metric == null) {
            return stat.getTotalRevenue();
        }
        return switch (metric.toLowerCase()) {
            case "revenue" -> stat.getTotalRevenue();
            case "reservations" -> stat.getTotalReservations();
            case "occupancy" -> stat.getOccupancyRate() != null
                    ? stat.getOccupancyRate().longValue()
                    : 0L;
            case "active_hosts" -> stat.getActiveHosts();
            case "active_guests" -> stat.getActiveGuests();
            default -> stat.getTotalRevenue();
        };
    }

    private Specification<User> createdBetween(LocalDateTime from, LocalDateTime to) {
        return (root, query, cb) -> cb.between(root.get("createdAt"), from, to);
    }
}
