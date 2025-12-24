package com.ssg9th2team.geharbang.domain.admin.controller;

import com.ssg9th2team.geharbang.domain.admin.dto.AdminPageResponse;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminPaymentDetail;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminPaymentSummary;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminRefundRequest;
import com.ssg9th2team.geharbang.domain.admin.service.AdminPaymentService;
import com.ssg9th2team.geharbang.domain.payment.dto.PaymentResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
        return paymentService.getPayments(status, keyword, page, size, sort);
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
        return paymentService.refundPayment(paymentId, reason, amount);
    }
}
