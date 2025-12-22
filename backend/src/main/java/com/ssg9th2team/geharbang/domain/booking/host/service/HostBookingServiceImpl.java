package com.ssg9th2team.geharbang.domain.booking.host.service;

import com.ssg9th2team.geharbang.domain.booking.host.dto.HostBookingResponse;
import com.ssg9th2team.geharbang.domain.booking.host.repository.mybatis.HostBookingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HostBookingServiceImpl implements HostBookingService {

    private final HostBookingMapper hostBookingMapper;

    @Override
    public List<HostBookingResponse> getBookings(Long hostId) {
        return hostBookingMapper.selectHostBookings(hostId);
    }

    @Override
    public List<HostBookingResponse> getBookingsForMonth(Long hostId, YearMonth month) {
        LocalDateTime start = month.atDay(1).atStartOfDay();
        LocalDateTime end = month.plusMonths(1).atDay(1).atStartOfDay();
        return hostBookingMapper.selectHostBookingsByRange(hostId, start, end);
    }

    @Override
    public HostBookingResponse getBooking(Long hostId, Long reservationId) {
        return hostBookingMapper.selectHostBookingById(hostId, reservationId);
    }
}
