package com.ssg9th2team.geharbang.domain.admin.service;

import com.ssg9th2team.geharbang.domain.admin.dto.AdminBookingDetail;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminBookingSummary;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminPageResponse;
import com.ssg9th2team.geharbang.domain.reservation.entity.Reservation;
import com.ssg9th2team.geharbang.domain.reservation.repository.jpa.ReservationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminBookingService {

    private final ReservationJpaRepository reservationRepository;

    public AdminPageResponse<AdminBookingSummary> getBookings(
            String status,
            LocalDate from,
            LocalDate to,
            String sort,
            int page,
            int size
    ) {
        Sort sorting = resolveSort(sort);
        List<Reservation> filtered = reservationRepository.findAll(sorting).stream()
                .filter(reservation -> matchesStatus(reservation, status))
                .filter(reservation -> matchesDateRange(reservation, from, to))
                .toList();

        int totalElements = filtered.size();
        int totalPages = size > 0 ? (int) Math.ceil(totalElements / (double) size) : 1;
        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<AdminBookingSummary> items = filtered.subList(fromIndex, toIndex).stream()
                .map(this::toSummary)
                .toList();
        return AdminPageResponse.of(items, page, size, totalElements, totalPages);
    }

    public AdminBookingDetail getBookingDetail(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found"));
        return toDetail(reservation);
    }

    private Sort resolveSort(String sort) {
        if (StringUtils.hasText(sort) && sort.equalsIgnoreCase("checkin")) {
            return Sort.by(Sort.Direction.ASC, "checkin");
        }
        return Sort.by(Sort.Direction.DESC, "createdAt");
    }

    private Integer parseStatus(String status) {
        if (!StringUtils.hasText(status) || "all".equalsIgnoreCase(status)) {
            return null;
        }
        String normalized = status.trim().toLowerCase();
        return switch (normalized) {
            case "pending", "requested" -> 1;
            case "confirmed", "approve" -> 2;
            case "checkedin", "checkin" -> 3;
            case "canceled", "cancelled", "refund" -> 9;
            default -> null;
        };
    }

    private boolean matchesStatus(Reservation reservation, String status) {
        Integer statusCode = parseStatus(status);
        return statusCode == null || statusCode.equals(reservation.getReservationStatus());
    }

    private boolean matchesDateRange(Reservation reservation, LocalDate from, LocalDate to) {
        LocalDateTime checkin = reservation.getCheckin();
        if (checkin == null) {
            return false;
        }
        if (from != null && checkin.isBefore(from.atStartOfDay())) {
            return false;
        }
        if (to != null && !checkin.isBefore(to.plusDays(1).atStartOfDay())) {
            return false;
        }
        return true;
    }

    private AdminBookingSummary toSummary(Reservation reservation) {
        return new AdminBookingSummary(
                reservation.getId(),
                reservation.getAccommodationsId(),
                reservation.getUserId(),
                reservation.getCheckin(),
                reservation.getCheckout(),
                reservation.getReservationStatus(),
                reservation.getPaymentStatus(),
                reservation.getFinalPaymentAmount(),
                reservation.getCreatedAt()
        );
    }

    private AdminBookingDetail toDetail(Reservation reservation) {
        return new AdminBookingDetail(
                reservation.getId(),
                reservation.getAccommodationsId(),
                reservation.getUserId(),
                reservation.getCheckin(),
                reservation.getCheckout(),
                reservation.getReservationStatus(),
                reservation.getPaymentStatus(),
                reservation.getFinalPaymentAmount(),
                reservation.getCreatedAt(),
                reservation.getUpdatedAt()
        );
    }
}
