package com.ssg9th2team.geharbang.domain.admin.dto;

import java.time.LocalDateTime;

public record AdminAccommodationDetail(
        Long accommodationsId,
        Long hostUserId,
        String name,
        String category,
        String city,
        String district,
        String approvalStatus,
        String rejectionReason,
        LocalDateTime createdAt
) {
}
