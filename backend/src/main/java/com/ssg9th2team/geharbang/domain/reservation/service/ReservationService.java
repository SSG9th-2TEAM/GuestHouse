package com.ssg9th2team.geharbang.domain.reservation.service;

import com.ssg9th2team.geharbang.domain.reservation.dto.ReservationRequestDto;
import com.ssg9th2team.geharbang.domain.reservation.dto.ReservationResponseDto;

import java.util.List;

public interface ReservationService {

    /**
     * 예약 생성
     */
    ReservationResponseDto createReservation(ReservationRequestDto requestDto);

    /**
     * 예약 단건 조회
     */
    ReservationResponseDto getReservationById(Long reservationId);

    /**
     * 사용자별 예약 목록 조회
     */
    List<ReservationResponseDto> getReservationsByUserId(Long userId);

    /**
     * 숙소별 예약 목록 조회
     */
    List<ReservationResponseDto> getReservationsByAccommodationId(Long accommodationsId);
}
