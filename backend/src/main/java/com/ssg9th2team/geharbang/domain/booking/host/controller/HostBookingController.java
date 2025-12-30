package com.ssg9th2team.geharbang.domain.booking.host.controller;

import com.ssg9th2team.geharbang.domain.booking.host.dto.HostBookingResponse;
import com.ssg9th2team.geharbang.domain.booking.host.service.HostBookingService;
import com.ssg9th2team.geharbang.domain.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/host/booking")
@RequiredArgsConstructor
public class HostBookingController {

    private final HostBookingService hostBookingService;
    private final UserRepository userRepository;

    @GetMapping
    public List<HostBookingResponse> listBookings(
            @RequestParam(required = false) String month,
            @RequestParam(required = false, name = "class") String viewClass,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "false") boolean upcomingOnly,
            Authentication authentication
    ) {
        String email = authentication.getName();
        Long hostId = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"))
                .getId();
        if (month != null && !month.isBlank()) {
            YearMonth yearMonth = YearMonth.parse(month);
            return hostBookingService.getBookingsForMonth(hostId, yearMonth);
        }
        if (upcomingOnly) {
            LocalDate baseDate = startDate != null ? startDate : LocalDate.now();
            return hostBookingService.getUpcomingBookings(hostId, baseDate.atStartOfDay());
        }
        if (startDate != null || endDate != null) {
            LocalDate rangeStart = startDate != null ? startDate : LocalDate.now();
            LocalDate rangeEnd = endDate != null ? endDate : rangeStart;
            LocalDateTime start = rangeStart.atStartOfDay();
            LocalDateTime end = rangeEnd.plusDays(1).atStartOfDay();
            return hostBookingService.getBookingsByRange(hostId, start, end);
        }
        return hostBookingService.getBookings(hostId);
    }

    @GetMapping("/{reservationId}")
    public ResponseEntity<HostBookingResponse> getBooking(
            @PathVariable Long reservationId,
            Authentication authentication
    ) {
        String email = authentication.getName();
        Long hostId = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"))
                .getId();
        HostBookingResponse response = hostBookingService.getBooking(hostId, reservationId);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }
}
