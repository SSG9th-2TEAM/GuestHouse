package com.ssg9th2team.geharbang.domain.admin.service;

import com.ssg9th2team.geharbang.domain.admin.dto.AdminBookingDetail;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminBookingSummary;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminPageResponse;
import com.ssg9th2team.geharbang.domain.reservation.entity.Reservation;
import com.ssg9th2team.geharbang.domain.reservation.repository.jpa.ReservationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
        Specification<Reservation> spec = buildSpecification(status, from, to);
        Sort sorting = resolveSort(sort);
        Page<Reservation> result = reservationRepository.findAll(spec, PageRequest.of(page, size, sorting));
        List<AdminBookingSummary> items = result.stream()
                .map(this::toSummary)
                .toList();
        return AdminPageResponse.of(items, page, size, result.getTotalElements(), result.getTotalPages());
    }

    public AdminBookingDetail getBookingDetail(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found"));
        return toDetail(reservation);
    }

    private Specification<Reservation> buildSpecification(String status, LocalDate from, LocalDate to) {
        return (root, q, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            Integer statusCode = parseStatus(status);
            if (statusCode != null) {
                predicates.add(cb.equal(root.get("reservationStatus"), statusCode));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("checkin"), from.atStartOfDay()));
            }
            if (to != null) {
                predicates.add(cb.lessThan(root.get("checkin"), to.plusDays(1).atStartOfDay()));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
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
