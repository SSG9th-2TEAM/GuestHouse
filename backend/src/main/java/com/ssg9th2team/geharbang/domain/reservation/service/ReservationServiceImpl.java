package com.ssg9th2team.geharbang.domain.reservation.service;

import com.ssg9th2team.geharbang.domain.reservation.dto.ReservationRequestDto;
import com.ssg9th2team.geharbang.domain.reservation.dto.ReservationResponseDto;
import com.ssg9th2team.geharbang.domain.reservation.entity.Reservation;
import com.ssg9th2team.geharbang.domain.reservation.repository.jpa.ReservationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationServiceImpl implements ReservationService {

    private final ReservationJpaRepository reservationRepository;

    @Override
    @Transactional
    public ReservationResponseDto createReservation(ReservationRequestDto requestDto) {
        // userId가 null이면 기본값 1L 사용
        Long userId = requestDto.getUserIdOrDefault();

        // 숙박 박수 계산
        int stayNights = (int) ChronoUnit.DAYS.between(
                requestDto.checkin().toLocalDate(),
                requestDto.checkout().toLocalDate());

        // 쿠폰 할인액 (요청에서 받거나 0)
        int couponDiscount = requestDto.couponDiscountAmount() != null
                ? requestDto.couponDiscountAmount()
                : 0;

        // 최종 결제 금액 계산
        int finalAmount = requestDto.totalAmount() - couponDiscount;

        Reservation reservation = Reservation.builder()
                .accommodationsId(requestDto.accommodationsId())
                .userId(userId)
                .checkin(requestDto.checkin())
                .checkout(requestDto.checkout())
                .stayNights(stayNights)
                .guestCount(requestDto.guestCount())
                .reservationStatus(1) // 1: 요청
                .totalAmountBeforeDc(requestDto.totalAmount())
                .couponDiscountAmount(couponDiscount)
                .finalPaymentAmount(finalAmount)
                .paymentStatus(0) // 0: 미결제
                .reserverName(requestDto.reserverName())
                .reserverPhone(requestDto.reserverPhone())
                .build();

        Reservation saved = reservationRepository.save(reservation);
        return ReservationResponseDto.from(saved);
    }

    @Override
    public ReservationResponseDto getReservationById(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다: " + reservationId));
        return ReservationResponseDto.from(reservation);
    }

    @Override
    public List<ReservationResponseDto> getReservationsByUserId(Long userId) {
        return reservationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(ReservationResponseDto::from)
                .toList();
    }

    @Override
    public List<ReservationResponseDto> getReservationsByAccommodationId(Long accommodationsId) {
        return reservationRepository.findByAccommodationsId(accommodationsId)
                .stream()
                .map(ReservationResponseDto::from)
                .toList();
    }
}
