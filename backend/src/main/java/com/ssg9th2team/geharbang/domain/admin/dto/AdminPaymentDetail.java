package com.ssg9th2team.geharbang.domain.admin.dto;

import java.time.LocalDateTime;

public record AdminPaymentDetail(
        Long paymentId,
        Long reservationId,
        String orderId,
        String pgPaymentKey,
        Integer requestAmount,
        Integer approvedAmount,
        Integer paymentStatus,
        LocalDateTime approvedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
