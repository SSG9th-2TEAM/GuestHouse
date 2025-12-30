package com.ssg9th2team.geharbang.domain.admin.dto;

import java.util.List;

public record AdminDashboardSummaryResponse(
        long pendingAccommodations,
        long openReports,
        long reservationCount,
        long paymentSuccessAmount,
        double platformFeeRate,
        long platformFeeAmount,
        long paymentFailureCount,
        long refundRequestCount,
        List<AdminAccommodationSummary> pendingAccommodationsList,
        List<AdminReportSummary> openReportsList
) {
}
