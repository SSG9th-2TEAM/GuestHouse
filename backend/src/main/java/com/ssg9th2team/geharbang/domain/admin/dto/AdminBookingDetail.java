package com.ssg9th2team.geharbang.domain.admin.dto;

import java.time.LocalDateTime;

public record AdminBookingDetail(
        Long reservationId,
        Long accommodationsId,
        Long userId,
        LocalDateTime checkin,
        LocalDateTime checkout,
        Integer reservationStatus,
        Integer paymentStatus,
        Integer finalPaymentAmount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
