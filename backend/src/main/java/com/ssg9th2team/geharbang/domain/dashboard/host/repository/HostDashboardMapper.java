package com.ssg9th2team.geharbang.domain.dashboard.host.repository;

import com.ssg9th2team.geharbang.domain.dashboard.host.dto.TodayScheduleItemResponse;
import com.ssg9th2team.geharbang.domain.dashboard.host.entity.HostSummaryRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface HostDashboardMapper {

    HostSummaryRow selectHostSummary(
            @Param("hostId") Long hostId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    List<TodayScheduleItemResponse> selectTodaySchedule(
            @Param("hostId") Long hostId,
            @Param("date") LocalDate date
    );
}
