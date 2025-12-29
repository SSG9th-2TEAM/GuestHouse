package com.ssg9th2team.geharbang.domain.booking.host.repository.mybatis;

import com.ssg9th2team.geharbang.domain.booking.host.dto.HostBookingResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface HostBookingMapper {
    List<HostBookingResponse> selectHostBookings(@Param("hostId") Long hostId);

    List<HostBookingResponse> selectHostBookingsByRange(
            @Param("hostId") Long hostId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    HostBookingResponse selectHostBookingById(
            @Param("hostId") Long hostId,
            @Param("reservationId") Long reservationId
    );
}
