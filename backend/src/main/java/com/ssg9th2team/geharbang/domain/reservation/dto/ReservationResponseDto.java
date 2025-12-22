package com.ssg9th2team.geharbang.domain.reservation.dto;

import com.ssg9th2team.geharbang.domain.reservation.entity.Reservation;

import java.time.LocalDateTime;

public record ReservationResponseDto(
        Long reservationId,
        Long accommodationsId,
        String accommodationName,
        String accommodationAddress,
        LocalDateTime checkin,
        LocalDateTime checkout,
        Integer stayNights,
        Integer guestCount,
        Integer reservationStatus,
        Integer totalAmountBeforeDc,
        Integer couponDiscountAmount,
        Integer finalPaymentAmount,
        Integer paymentStatus,
        String reserverName,
        String reserverPhone,
        LocalDateTime createdAt) {
    public static ReservationResponseDto from(Reservation reservation) {
        return new ReservationResponseDto(
                reservation.getId(),
                reservation.getAccommodationsId(),
                null, // accommodationName
                null, // accommodationAddress
                reservation.getCheckin(),
                reservation.getCheckout(),
                reservation.getStayNights(),
                reservation.getGuestCount(),
                reservation.getReservationStatus(),
                reservation.getTotalAmountBeforeDc(),
                reservation.getCouponDiscountAmount(),
                reservation.getFinalPaymentAmount(),
                reservation.getPaymentStatus(),
                reservation.getReserverName(),
                reservation.getReserverPhone(),
                reservation.getCreatedAt());
    }

    public static ReservationResponseDto from(Reservation reservation, String accommodationName,
            String accommodationAddress) {
        return new ReservationResponseDto(
                reservation.getId(),
                reservation.getAccommodationsId(),
                accommodationName,
                accommodationAddress,
                reservation.getCheckin(),
                reservation.getCheckout(),
                reservation.getStayNights(),
                reservation.getGuestCount(),
                reservation.getReservationStatus(),
                reservation.getTotalAmountBeforeDc(),
                reservation.getCouponDiscountAmount(),
                reservation.getFinalPaymentAmount(),
                reservation.getPaymentStatus(),
                reservation.getReserverName(),
                reservation.getReserverPhone(),
                reservation.getCreatedAt());
    }
}
