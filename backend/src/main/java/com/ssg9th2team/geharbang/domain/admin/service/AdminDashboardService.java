package com.ssg9th2team.geharbang.domain.admin.service;

import com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation;
import com.ssg9th2team.geharbang.domain.accommodation.entity.ApprovalStatus;
import com.ssg9th2team.geharbang.domain.accommodation.repository.jpa.AccommodationJpaRepository;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminAccommodationSummary;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminDashboardSummaryResponse;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminReportSummary;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminTimeseriesPoint;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminTimeseriesResponse;
import com.ssg9th2team.geharbang.domain.admin.entity.PlatformDailyStats;
import com.ssg9th2team.geharbang.domain.admin.repository.PlatformDailyStatsRepository;
import com.ssg9th2team.geharbang.domain.payment.repository.jpa.PaymentRefundJpaRepository;
import com.ssg9th2team.geharbang.domain.payment.repository.jpa.PaymentJpaRepository;
import com.ssg9th2team.geharbang.domain.report.entity.ReviewReport;
import com.ssg9th2team.geharbang.domain.report.repository.jpa.ReviewReportJpaRepository;
import com.ssg9th2team.geharbang.domain.reservation.entity.Reservation;
import com.ssg9th2team.geharbang.domain.reservation.repository.jpa.ReservationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    @Value("${host.platform.fee-rate:0.04}")
    private double platformFeeRate;

    private final AccommodationJpaRepository accommodationRepository;
    private final ReservationJpaRepository reservationRepository;
    private final PaymentJpaRepository paymentRepository;
    private final PaymentRefundJpaRepository refundRepository;
    private final ReviewReportJpaRepository reportRepository;
    private final PlatformDailyStatsRepository statsRepository;

    public AdminDashboardSummaryResponse getDashboardSummary(LocalDate from, LocalDate to) {
        LocalDate startDate = from != null ? from : LocalDate.now();
        LocalDate endDate = to != null ? to : LocalDate.now();
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();

        long pendingAccommodations = accommodationRepository.count(approvalEquals(ApprovalStatus.PENDING));
        long reservationCount = reservationRepository.count(createdBetween(start, end));
        Long successAmount = paymentRepository.sumApprovedAmount(start, end);
        long paymentSuccessAmount = successAmount != null ? successAmount : 0L;
        long platformFeeAmount = Math.round(paymentSuccessAmount * platformFeeRate);
        long paymentFailureCount = paymentRepository.countByPaymentStatusAndCreatedAtBetween(2, start, end)
                + paymentRepository.countByPaymentStatusAndCreatedAtBetween(3, start, end);
        long refundRequestCount = refundRepository.countByRefundStatusAndCreatedAtBetween(0, start, end);
        long openReports = reportRepository.count(reportStateEquals("WAIT"));

        List<AdminAccommodationSummary> pendingList = accommodationRepository
                .findAll(approvalEquals(ApprovalStatus.PENDING),
                        PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt")))
                .stream()
                .map(this::toAccommodationSummary)
                .toList();

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

    private Specification<Accommodation> approvalEquals(ApprovalStatus status) {
        return (root, query, cb) -> cb.equal(root.get("approvalStatus"), status);
    }

    private Specification<Reservation> createdBetween(LocalDateTime start, LocalDateTime end) {
        return (root, query, cb) -> cb.and(
                cb.greaterThanOrEqualTo(root.get("createdAt"), start),
                cb.lessThan(root.get("createdAt"), end)
        );
    }

    private Specification<ReviewReport> reportStateEquals(String state) {
        return (root, query, cb) -> cb.equal(root.get("state"), state);
    }

    private AdminAccommodationSummary toAccommodationSummary(Accommodation accommodation) {
        return new AdminAccommodationSummary(
                accommodation.getAccommodationsId(),
                accommodation.getUserId(),
                accommodation.getAccommodationsName(),
                accommodation.getAccommodationsCategory() != null ? accommodation.getAccommodationsCategory().name() : null,
                accommodation.getCity(),
                accommodation.getDistrict(),
                accommodation.getApprovalStatus() != null ? accommodation.getApprovalStatus().name() : null,
                accommodation.getRejectionReason(),
                accommodation.getCreatedAt()
        );
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
}
