package com.ssg9th2team.geharbang.domain.admin.service;

import com.ssg9th2team.geharbang.domain.admin.dto.AdminPageResponse;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminPaymentDetail;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminPaymentSummary;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminPaymentMetricsPoint;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminPaymentMetricsRow;
import com.ssg9th2team.geharbang.domain.admin.log.AdminLogConstants;
import com.ssg9th2team.geharbang.domain.admin.repository.mybatis.AdminPaymentMetricsMapper;
import com.ssg9th2team.geharbang.domain.payment.dto.PaymentResponseDto;
import com.ssg9th2team.geharbang.domain.payment.entity.Payment;
import com.ssg9th2team.geharbang.domain.payment.entity.PaymentRefund;
import com.ssg9th2team.geharbang.domain.payment.repository.jpa.PaymentJpaRepository;
import com.ssg9th2team.geharbang.domain.payment.repository.jpa.PaymentRefundJpaRepository;
import com.ssg9th2team.geharbang.domain.reservation.entity.Reservation;
import com.ssg9th2team.geharbang.domain.reservation.repository.jpa.ReservationJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminPaymentService {

    private final PaymentJpaRepository paymentRepository;
    private final AdminPaymentMetricsMapper paymentMetricsMapper;
    private final PaymentRefundJpaRepository paymentRefundRepository;
    private final ReservationJpaRepository reservationRepository;
    private final AdminLogService adminLogService;
    @PersistenceContext
    private EntityManager entityManager;

    public AdminPageResponse<AdminPaymentSummary> getPayments(
            String status,
            String keyword,
            int page,
            int size,
            String sort
    ) {
        Sort sorting = "oldest".equalsIgnoreCase(sort)
                ? Sort.by(Sort.Direction.ASC, "createdAt")
                : Sort.by(Sort.Direction.DESC, "createdAt");
        List<Payment> filtered = paymentRepository.findAll(sorting).stream()
                .filter(payment -> matchesStatus(payment, status))
                .filter(payment -> matchesKeyword(payment, keyword))
                .toList();

        int totalElements = filtered.size();
        int totalPages = size > 0 ? (int) Math.ceil(totalElements / (double) size) : 1;
        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<AdminPaymentSummary> items = filtered.subList(fromIndex, toIndex).stream()
                .map(this::toSummary)
                .toList();
        return AdminPageResponse.of(items, page, size, totalElements, totalPages);
    }

    public AdminPaymentDetail getPaymentDetail(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
        return toDetail(payment);
    }

    @Transactional
    public PaymentResponseDto refundPayment(Long paymentId, String reason, Integer amount) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
        boolean hasRefund = paymentRefundRepository.findByPaymentId(paymentId).stream()
                .anyMatch(refund -> refund.getRefundStatus() != null && refund.getRefundStatus() == 1);
        if (hasRefund) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment already refunded");
        }
        if (payment.getPaymentStatus() != null && payment.getPaymentStatus() == 3) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment already refunded");
        }
        if (payment.getPaymentStatus() == null || payment.getPaymentStatus() != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only successful payments can be refunded");
        }
        Integer baseAmount = payment.getApprovedAmount() != null
                ? payment.getApprovedAmount()
                : payment.getRequestAmount();
        if (baseAmount == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Refund amount is missing");
        }
        Integer refundAmount = amount != null ? amount : baseAmount;
        if (refundAmount > baseAmount) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Refund amount exceeds approved amount");
        }

        PaymentRefund refund = PaymentRefund.builder()
                .paymentId(payment.getId())
                .refundAmount(refundAmount)
                .refundStatus(1)
                .reasonMessage(reason)
                .requestedBy("ADMIN")
                .approvedAt(LocalDateTime.now())
                .build();
        paymentRefundRepository.save(refund);

        entityManager.createQuery(
                        "UPDATE Payment p SET p.paymentStatus = :status, p.updatedAt = :updatedAt WHERE p.id = :id")
                .setParameter("status", 3)
                .setParameter("updatedAt", LocalDateTime.now())
                .setParameter("id", paymentId)
                .executeUpdate();

        Reservation reservation = reservationRepository.findById(payment.getReservationId()).orElse(null);
        if (reservation != null) {
            entityManager.createQuery(
                            "UPDATE Reservation r SET r.reservationStatus = :status, r.paymentStatus = :paymentStatus, r.updatedAt = :updatedAt WHERE r.id = :id")
                    .setParameter("status", 9)
                    .setParameter("paymentStatus", 3)
                    .setParameter("updatedAt", LocalDateTime.now())
                    .setParameter("id", reservation.getId())
                    .executeUpdate();
        }

        entityManager.clear();
        Payment updated = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
        adminLogService.writeLog(AdminLogConstants.TARGET_PAYMENT, paymentId, AdminLogConstants.ACTION_REFUND, reason);
        return PaymentResponseDto.from(updated);
    }

    public List<AdminPaymentMetricsPoint> getMetrics(
            String mode,
            Integer year,
            String status,
            String type,
            String keyword
    ) {
        Integer statusCode = parseStatus(status);
        String typeMode = parseType(type);
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;

        if ("monthly".equalsIgnoreCase(mode)) {
            int targetYear = year != null ? year : LocalDate.now().getYear();
            LocalDateTime from = LocalDate.of(targetYear, 1, 1).atStartOfDay();
            LocalDateTime to = LocalDate.of(targetYear, 12, 31).atTime(23, 59, 59);
            List<AdminPaymentMetricsRow> raw = paymentMetricsMapper.selectMonthlyMetrics(from, to, statusCode, typeMode, normalizedKeyword);
            Map<Integer, AdminPaymentMetricsRow> byMonth = raw.stream()
                    .filter(row -> row.period() != null)
                    .collect(Collectors.toMap(AdminPaymentMetricsRow::period, row -> row));
            return buildMonthlyPoints(targetYear, byMonth);
        }

        int currentYear = LocalDate.now().getYear();
        int startYear = currentYear - 4;
        LocalDateTime from = LocalDate.of(startYear, 1, 1).atStartOfDay();
        LocalDateTime to = LocalDate.of(currentYear, 12, 31).atTime(23, 59, 59);
        List<AdminPaymentMetricsRow> raw = paymentMetricsMapper.selectYearlyMetrics(from, to, statusCode, typeMode, normalizedKeyword);
        Map<Integer, AdminPaymentMetricsRow> byYear = raw.stream()
                .filter(row -> row.period() != null)
                .collect(Collectors.toMap(AdminPaymentMetricsRow::period, row -> row));
        return buildYearlyPoints(startYear, currentYear, byYear);
    }

    private Integer parseStatus(String status) {
        if (!StringUtils.hasText(status) || "all".equalsIgnoreCase(status)) {
            return null;
        }
        String normalized = status.trim().toLowerCase();
        return switch (normalized) {
            case "success", "completed" -> 1;
            case "failed", "fail" -> 2;
            case "refunded", "refund" -> 3;
            default -> null;
        };
    }

    private String parseType(String type) {
        if (!StringUtils.hasText(type) || "all".equalsIgnoreCase(type)) {
            return null;
        }
        String normalized = type.trim().toLowerCase();
        if ("refund".equals(normalized) || "refunded".equals(normalized) || "refund".equalsIgnoreCase(type)) {
            return "REFUND";
        }
        if ("reservation".equals(normalized) || "reserve".equals(normalized)) {
            return "RESERVATION";
        }
        return null;
    }

    private boolean matchesStatus(Payment payment, String status) {
        Integer statusCode = parseStatus(status);
        return statusCode == null || statusCode.equals(payment.getPaymentStatus());
    }

    private boolean matchesKeyword(Payment payment, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String normalized = keyword.toLowerCase();
        String orderId = payment.getOrderId() != null ? payment.getOrderId().toLowerCase() : "";
        String pgPaymentKey = payment.getPgPaymentKey() != null ? payment.getPgPaymentKey().toLowerCase() : "";
        return orderId.contains(normalized) || pgPaymentKey.contains(normalized);
    }

    private AdminPaymentSummary toSummary(Payment payment) {
        return new AdminPaymentSummary(
                payment.getId(),
                payment.getReservationId(),
                payment.getOrderId(),
                payment.getPgPaymentKey(),
                payment.getApprovedAmount(),
                payment.getPaymentStatus(),
                payment.getCreatedAt()
        );
    }

    private AdminPaymentDetail toDetail(Payment payment) {
        Reservation reservation = reservationRepository.findById(payment.getReservationId()).orElse(null);
        PaymentRefund refund = paymentRefundRepository.findByPaymentId(payment.getId()).stream()
                .max(Comparator.comparing(PaymentRefund::getRequestedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
        return new AdminPaymentDetail(
                payment.getId(),
                payment.getReservationId(),
                reservation != null ? reservation.getAccommodationsId() : null,
                reservation != null ? reservation.getUserId() : null,
                reservation != null ? reservation.getReservationStatus() : null,
                payment.getOrderId(),
                payment.getPgPaymentKey(),
                payment.getRequestAmount(),
                payment.getApprovedAmount(),
                payment.getPaymentStatus(),
                payment.getApprovedAt(),
                refund != null ? refund.getRefundStatus() : null,
                refund != null ? refund.getRefundAmount() : null,
                refund != null ? refund.getApprovedAt() : null,
                refund != null ? refund.getReasonMessage() : null,
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }

    private List<AdminPaymentMetricsPoint> buildMonthlyPoints(int year, Map<Integer, AdminPaymentMetricsRow> byMonth) {
        return java.util.stream.IntStream.rangeClosed(1, 12)
                .mapToObj(month -> {
                    AdminPaymentMetricsRow row = byMonth.get(month);
                    long totalAmount = Optional.ofNullable(row).map(AdminPaymentMetricsRow::totalAmount).orElse(0L);
                    long totalCount = Optional.ofNullable(row).map(AdminPaymentMetricsRow::totalCount).orElse(0L);
                    return new AdminPaymentMetricsPoint(String.format("%d월", month), totalAmount, totalCount);
                })
                .toList();
    }

    private List<AdminPaymentMetricsPoint> buildYearlyPoints(int startYear, int endYear, Map<Integer, AdminPaymentMetricsRow> byYear) {
        return java.util.stream.IntStream.rangeClosed(startYear, endYear)
                .mapToObj(year -> {
                    AdminPaymentMetricsRow row = byYear.get(year);
                    long totalAmount = Optional.ofNullable(row).map(AdminPaymentMetricsRow::totalAmount).orElse(0L);
                    long totalCount = Optional.ofNullable(row).map(AdminPaymentMetricsRow::totalCount).orElse(0L);
                    return new AdminPaymentMetricsPoint(String.valueOf(year), totalAmount, totalCount);
                })
                .toList();
    }
}
