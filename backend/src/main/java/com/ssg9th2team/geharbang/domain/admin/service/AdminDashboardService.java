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
import com.ssg9th2team.geharbang.domain.payment.entity.Payment;
import com.ssg9th2team.geharbang.domain.payment.entity.PaymentRefund;
import com.ssg9th2team.geharbang.domain.payment.repository.jpa.PaymentJpaRepository;
import com.ssg9th2team.geharbang.domain.payment.repository.jpa.PaymentRefundJpaRepository;
import com.ssg9th2team.geharbang.domain.report.entity.ReviewReport;
import com.ssg9th2team.geharbang.domain.report.repository.jpa.ReviewReportJpaRepository;
import com.ssg9th2team.geharbang.domain.reservation.repository.jpa.ReservationJpaRepository;
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
import java.util.stream.Collectors;

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
    private final ReservationJpaRepository reservationRepository;
    private final PaymentJpaRepository paymentRepository;
    private final PaymentRefundJpaRepository refundRepository;

    public AdminDashboardSummaryResponse getDashboardSummary(LocalDate from, LocalDate to) {
        LocalDate startDate = from != null ? from : LocalDate.now();
        LocalDate endDate = to != null ? to : LocalDate.now();
        SummaryMetrics metrics = buildSummaryMetrics(startDate, endDate);
        long reservationCount = metrics.reservationCount;
        long paymentSuccessAmount = metrics.paymentSuccessAmount;
        long paymentFailureCount = metrics.paymentFailureCount;
        long refundRequestCount = metrics.refundRequestCount;
        long refundCompletedCount = metrics.refundCompletedCount;
        long refundCompletedAmount = metrics.refundCompletedAmount;
        long netRevenue = metrics.netRevenue;

        long pendingAccommodations = accommodationRepository.count(approvalEquals(ApprovalStatus.PENDING));
        long openReports = reportRepository.count(reportStateEquals("WAIT"));
        long platformFeeAmount = buildPlatformFeeSeries(startDate, endDate).stream()
                .mapToLong(AdminTimeseriesPoint::value)
                .sum();

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
                refundCompletedCount,
                refundCompletedAmount,
                netRevenue,
                pendingList,
                openReportList
        );
    }

    public AdminTimeseriesResponse getTimeseries(String metric, LocalDate from, LocalDate to) {
        LocalDate startDate = from != null ? from : LocalDate.now().minusDays(6);
        LocalDate endDate = to != null ? to : LocalDate.now();
        List<PlatformDailyStats> stats = statsRepository.findByStatDateBetweenOrderByStatDateAsc(startDate, endDate);

        List<AdminTimeseriesPoint> points;
        if ("platform_fee".equalsIgnoreCase(metric)) {
            points = buildPlatformFeeSeries(startDate, endDate);
        } else if ("revenue".equalsIgnoreCase(metric)) {
            points = buildNetRevenueSeries(startDate, endDate);
        } else {
            points = stats.stream()
                    .map(stat -> new AdminTimeseriesPoint(stat.getStatDate(), resolveMetricValue(metric, stat)))
                    .toList();
        }

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

    /*
     * KPI definitions (Admin dashboard / payments management)
     * - Gross: Payment.paymentStatus=1 AND createdAt in [from, to) SUM(approvedAmount)
     * - Refund completed: PaymentRefund.refundStatus=1 AND requestedAt in [from, to) COUNT/SUM(refundAmount)
     * - Net: Gross - Refund completed amount
     * - Platform fee: floor(Net * platformFeeRate)
     * - Payment failure: Payment.paymentStatus=2 COUNT
     * - Refund request: PaymentRefund.refundStatus=0 COUNT
     */
    private SummaryMetrics buildSummaryMetrics(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();

        long reservationCount = reservationRepository.countByCreatedAtBetween(start, end);

        List<Payment> payments = paymentRepository.findByCreatedAtBetween(start, end);
        long paymentSuccessAmount = payments.stream()
                .filter(payment -> payment.getPaymentStatus() != null && payment.getPaymentStatus() == 1)
                .mapToLong(payment -> payment.getApprovedAmount() != null ? payment.getApprovedAmount() : 0L)
                .sum();
        long paymentFailureCount = payments.stream()
                .filter(payment -> payment.getPaymentStatus() != null && payment.getPaymentStatus() == 2)
                .count();

        List<PaymentRefund> refunds = refundRepository.findByRequestedAtBetween(start, end);
        long refundRequestCount = refunds.stream()
                .filter(refund -> refund.getRefundStatus() != null && refund.getRefundStatus() == 0)
                .count();
        long refundCompletedCount = refunds.stream()
                .filter(refund -> refund.getRefundStatus() != null && refund.getRefundStatus() == 1)
                .count();
        long refundCompletedAmount = refunds.stream()
                .filter(refund -> refund.getRefundStatus() != null && refund.getRefundStatus() == 1)
                .mapToLong(refund -> refund.getRefundAmount() != null ? refund.getRefundAmount() : 0L)
                .sum();
        long netRevenue = paymentSuccessAmount - refundCompletedAmount;

        return new SummaryMetrics(
                reservationCount,
                paymentSuccessAmount,
                paymentFailureCount,
                refundRequestCount,
                refundCompletedCount,
                refundCompletedAmount,
                netRevenue
        );
    }

    private List<AdminTimeseriesPoint> buildNetRevenueSeries(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();

        Map<LocalDate, Long> grossByDate = paymentRepository.findByCreatedAtBetween(start, end).stream()
                .filter(payment -> payment.getPaymentStatus() != null && payment.getPaymentStatus() == 1)
                .collect(Collectors.groupingBy(
                        payment -> payment.getCreatedAt().toLocalDate(),
                        Collectors.summingLong(payment -> payment.getApprovedAmount() != null ? payment.getApprovedAmount() : 0L)
                ));

        Map<LocalDate, Long> refundByDate = refundRepository.findByRequestedAtBetween(start, end).stream()
                .filter(refund -> refund.getRefundStatus() != null && refund.getRefundStatus() == 1)
                .collect(Collectors.groupingBy(
                        refund -> refund.getRequestedAt().toLocalDate(),
                        Collectors.summingLong(refund -> refund.getRefundAmount() != null ? refund.getRefundAmount() : 0L)
                ));

        return startDate.datesUntil(endDate.plusDays(1))
                .map(date -> {
                    long gross = grossByDate.getOrDefault(date, 0L);
                    long refund = refundByDate.getOrDefault(date, 0L);
                    return new AdminTimeseriesPoint(date, gross - refund);
                })
                .toList();
    }

    private List<AdminTimeseriesPoint> buildPlatformFeeSeries(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();

        Map<LocalDate, Long> grossByDate = paymentRepository.findByCreatedAtBetween(start, end).stream()
                .filter(payment -> payment.getPaymentStatus() != null && payment.getPaymentStatus() == 1)
                .collect(Collectors.groupingBy(
                        payment -> payment.getCreatedAt().toLocalDate(),
                        Collectors.summingLong(payment -> payment.getApprovedAmount() != null ? payment.getApprovedAmount() : 0L)
                ));

        Map<LocalDate, Long> refundByDate = refundRepository.findByRequestedAtBetween(start, end).stream()
                .filter(refund -> refund.getRefundStatus() != null && refund.getRefundStatus() == 1)
                .collect(Collectors.groupingBy(
                        refund -> refund.getRequestedAt().toLocalDate(),
                        Collectors.summingLong(refund -> refund.getRefundAmount() != null ? refund.getRefundAmount() : 0L)
                ));

        return startDate.datesUntil(endDate.plusDays(1))
                .map(date -> {
                    long gross = grossByDate.getOrDefault(date, 0L);
                    long refund = refundByDate.getOrDefault(date, 0L);
                    long net = gross - refund;
                    return new AdminTimeseriesPoint(date, calcPlatformFee(net));
                })
                .toList();
    }

    private long calcPlatformFee(long netRevenue) {
        return (long) Math.floor(netRevenue * platformFeeRate);
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

    private record SummaryMetrics(long reservationCount,
                                  long paymentSuccessAmount,
                                  long paymentFailureCount,
                                  long refundRequestCount,
                                  long refundCompletedCount,
                                  long refundCompletedAmount,
                                  long netRevenue) {
    }
}
