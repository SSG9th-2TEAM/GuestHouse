package com.ssg9th2team.geharbang.domain.booking.host.controller;

import com.ssg9th2team.geharbang.domain.booking.host.dto.HostBookingResponse;
import com.ssg9th2team.geharbang.domain.booking.host.service.HostBookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/host/booking")
@RequiredArgsConstructor
public class HostBookingController {

    private final HostBookingService hostBookingService;

    @GetMapping
    public List<HostBookingResponse> listBookings(
            @RequestParam(required = false) String month,
            @RequestParam(required = false, name = "class") String viewClass
    ) {
        Long hostId = 1L; // TODO: replace with authenticated user ID.
        if (month != null && !month.isBlank()) {
            YearMonth yearMonth = YearMonth.parse(month);
            return hostBookingService.getBookingsForMonth(hostId, yearMonth);
        }
        return hostBookingService.getBookings(hostId);
    }

    @GetMapping("/{reservationId}")
    public ResponseEntity<HostBookingResponse> getBooking(
            @PathVariable Long reservationId
    ) {
        Long hostId = 1L; // TODO: replace with authenticated user ID.
        HostBookingResponse response = hostBookingService.getBooking(hostId, reservationId);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }
}
