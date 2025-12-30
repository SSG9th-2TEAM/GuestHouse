package com.ssg9th2team.geharbang.domain.booking.host.service;

import com.ssg9th2team.geharbang.domain.booking.host.dto.HostBookingResponse;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

public interface HostBookingService {
    List<HostBookingResponse> getBookings(Long hostId);

    List<HostBookingResponse> getBookingsForMonth(Long hostId, YearMonth month);

    List<HostBookingResponse> getBookingsByRange(Long hostId, LocalDateTime start, LocalDateTime end);

    List<HostBookingResponse> getUpcomingBookings(Long hostId, LocalDateTime start);

    HostBookingResponse getBooking(Long hostId, Long reservationId);
}
