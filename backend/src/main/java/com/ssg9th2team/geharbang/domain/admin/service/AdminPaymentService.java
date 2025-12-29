package com.ssg9th2team.geharbang.domain.admin.service;

import com.ssg9th2team.geharbang.domain.admin.dto.AdminPageResponse;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminPaymentDetail;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminPaymentSummary;
import com.ssg9th2team.geharbang.domain.payment.dto.PaymentResponseDto;
import com.ssg9th2team.geharbang.domain.payment.entity.Payment;
import com.ssg9th2team.geharbang.domain.payment.repository.jpa.PaymentJpaRepository;
import com.ssg9th2team.geharbang.domain.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminPaymentService {

    private final PaymentJpaRepository paymentRepository;
    private final PaymentService paymentService;

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

    public PaymentResponseDto refundPayment(Long paymentId, String reason, Integer amount) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
        return paymentService.cancelPayment(payment.getReservationId(), reason, amount);
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
        return new AdminPaymentDetail(
                payment.getId(),
                payment.getReservationId(),
                payment.getOrderId(),
                payment.getPgPaymentKey(),
                payment.getRequestAmount(),
                payment.getApprovedAmount(),
                payment.getPaymentStatus(),
                payment.getApprovedAt(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
