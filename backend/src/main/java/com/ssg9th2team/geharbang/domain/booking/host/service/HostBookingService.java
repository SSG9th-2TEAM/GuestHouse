package com.ssg9th2team.geharbang.domain.booking.host.service;

import com.ssg9th2team.geharbang.domain.booking.host.dto.HostBookingResponse;

import java.time.YearMonth;
import java.util.List;

public interface HostBookingService {
    List<HostBookingResponse> getBookings(Long hostId);

    List<HostBookingResponse> getBookingsForMonth(Long hostId, YearMonth month);

    HostBookingResponse getBooking(Long hostId, Long reservationId);
}
