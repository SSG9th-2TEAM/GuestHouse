package com.ssg9th2team.geharbang.domain.admin.service;

import com.ssg9th2team.geharbang.domain.admin.dto.AdminPageResponse;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminPaymentDetail;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminPaymentSummary;
import com.ssg9th2team.geharbang.domain.payment.dto.PaymentResponseDto;
import com.ssg9th2team.geharbang.domain.payment.entity.Payment;
import com.ssg9th2team.geharbang.domain.payment.repository.jpa.PaymentJpaRepository;
import com.ssg9th2team.geharbang.domain.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
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
        Specification<Payment> spec = buildSpecification(status, keyword);
        Sort sorting = "oldest".equalsIgnoreCase(sort)
                ? Sort.by(Sort.Direction.ASC, "createdAt")
                : Sort.by(Sort.Direction.DESC, "createdAt");
        Page<Payment> result = paymentRepository.findAll(spec, PageRequest.of(page, size, sorting));
        List<AdminPaymentSummary> items = result.stream()
                .map(this::toSummary)
                .toList();
        return AdminPageResponse.of(items, page, size, result.getTotalElements(), result.getTotalPages());
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

    private Specification<Payment> buildSpecification(String status, String keyword) {
        return (root, q, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            Integer statusCode = parseStatus(status);
            if (statusCode != null) {
                predicates.add(cb.equal(root.get("paymentStatus"), statusCode));
            }
            if (StringUtils.hasText(keyword)) {
                String likeQuery = "%" + keyword.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("orderId")), likeQuery),
                        cb.like(cb.lower(root.get("pgPaymentKey")), likeQuery)
                ));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
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
