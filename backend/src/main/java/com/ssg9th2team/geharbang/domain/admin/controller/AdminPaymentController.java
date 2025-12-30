package com.ssg9th2team.geharbang.domain.admin.controller;

import com.ssg9th2team.geharbang.domain.admin.dto.AdminPageResponse;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminPaymentDetail;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminPaymentSummary;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminRefundRequest;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminPaymentMetricsPoint;
import com.ssg9th2team.geharbang.domain.admin.service.AdminPaymentService;
import com.ssg9th2team.geharbang.domain.payment.dto.PaymentResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/payments")
@RequiredArgsConstructor
public class AdminPaymentController {

    private final AdminPaymentService paymentService;

    @GetMapping
    public AdminPageResponse<AdminPaymentSummary> getPayments(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "latest") String sort
    ) {
        return paymentService.getPayments(
                normalizeFilter(status),
                normalizeFilter(keyword),
                page,
                size,
                sort
        );
    }

    @GetMapping("/metrics")
    public List<AdminPaymentMetricsPoint> getPaymentMetrics(
            @RequestParam(defaultValue = "yearly") String mode,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword
    ) {
        return paymentService.getMetrics(
                mode,
                year,
                normalizeFilter(status),
                normalizeFilter(type),
                normalizeFilter(keyword)
        );
    }

    @GetMapping("/{paymentId}")
    public AdminPaymentDetail getPaymentDetail(@PathVariable Long paymentId) {
        return paymentService.getPaymentDetail(paymentId);
    }

    @PostMapping("/{paymentId}/refund")
    public PaymentResponseDto refundPayment(
            @PathVariable Long paymentId,
            @RequestBody AdminRefundRequest request
    ) {
        String reason = request != null ? request.reason() : null;
        Integer amount = request != null ? request.amount() : null;
        Boolean override = request != null ? request.override() : null;
        return paymentService.refundPayment(paymentId, reason, amount, override);
    }

    private String normalizeFilter(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        if ("all".equalsIgnoreCase(normalized)
                || "undefined".equalsIgnoreCase(normalized)
                || "null".equalsIgnoreCase(normalized)) {
            return null;
        }
        return normalized;
    }
}
