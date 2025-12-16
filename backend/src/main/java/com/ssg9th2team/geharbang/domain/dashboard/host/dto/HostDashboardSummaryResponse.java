package com.ssg9th2team.geharbang.domain.dashboard.host.dto;

import com.ssg9th2team.geharbang.domain.dashboard.host.entity.HostSummaryRow;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class HostDashboardSummaryResponse {
    private final long expectedRevenue;          // 이번 달 예상 수익
    private final int confirmedReservations;     // 이번 달 확정 예약 수
    private final double avgRating;              // 평균 평점
    private final int operatingAccommodations;   // 운영 중 숙소 수

    public static HostDashboardSummaryResponse from(HostSummaryRow row) {
        if (row == null) {
            return HostDashboardSummaryResponse.builder()
                    .expectedRevenue(0L)
                    .confirmedReservations(0)
                    .avgRating(0.0)
                    .operatingAccommodations(0)
                    .build();
        }
        return HostDashboardSummaryResponse.builder()
                .expectedRevenue(row.getExpectedRevenue())
                .confirmedReservations(row.getConfirmedReservations())
                .avgRating(row.getAvgRating())
                .operatingAccommodations(row.getOperatingAccommodations())
                .build();
    }
}
