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
        private final com.ssg9th2team.geharbang.domain.accommodation.repository.jpa.AccommodationJpaRepository accommodationRepository;
        private final com.ssg9th2team.geharbang.domain.room.repository.jpa.RoomJpaRepository roomRepository;

        @Override
        @Transactional
        public ReservationResponseDto createReservation(ReservationRequestDto requestDto) {
                System.out.println("DEBUG: createReservation called with: " + requestDto);
                // Room 재고(maxGuests) 차감 (동시성 제어)
                // roomId가 없으면 에러 혹은 로직에 따라 처리해야 하나, 요구사항에 따라 필수라고 가정
                if (requestDto.roomId() == null) {
                        throw new IllegalArgumentException("Room ID is required for reservation.");
                }
                int updatedRows = roomRepository.decreaseMaxGuests(requestDto.roomId(), requestDto.guestCount());
                if (updatedRows == 0) {
                        throw new IllegalStateException("객실 정원이 초과되어 예약할 수 없습니다.");
                }

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
                                .reservationStatus(0) // 0: 결제 대기
                                .totalAmountBeforeDc(requestDto.totalAmount())
                                .couponDiscountAmount(couponDiscount)
                                .finalPaymentAmount(finalAmount)
                                .paymentStatus(0) // 0: 미결제
                                .reserverName(requestDto.reserverName())
                                .reserverPhone(requestDto.reserverPhone())
                                .build();

                Reservation saved = reservationRepository.save(reservation);

                // Accommodation 정보 조회 (이름/주소 반환을 위해)
                com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation accommodation = accommodationRepository
                                .findById(saved.getAccommodationsId())
                                .orElse(null);

                String accName = (accommodation != null) ? accommodation.getAccommodationsName() : null;
                String accAddress = (accommodation != null)
                                ? accommodation.getCity() + " " + accommodation.getDistrict() + " "
                                                + accommodation.getTownship() + " " + accommodation.getAddressDetail()
                                : null;

                return ReservationResponseDto.from(saved, accName, accAddress);
        }

        @Override
        public ReservationResponseDto getReservationById(Long reservationId) {
                Reservation reservation = reservationRepository.findById(reservationId)
                                .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다: " + reservationId));

                com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation accommodation = accommodationRepository
                                .findById(reservation.getAccommodationsId())
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "숙소를 찾을 수 없습니다: " + reservation.getAccommodationsId()));

                String address = accommodation.getCity() + " " + accommodation.getDistrict() + " "
                                + accommodation.getTownship() + " " + accommodation.getAddressDetail();

                return ReservationResponseDto.from(reservation, accommodation.getAccommodationsName(), address);
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

        @Override
        @Transactional
        public void deletePendingReservation(Long reservationId) {
                int deleted = reservationRepository.deletePendingReservation(reservationId);
                if (deleted == 0) {
                        throw new IllegalArgumentException("대기 상태의 예약을 찾을 수 없습니다: " + reservationId);
                }
        }

        @Override
        @Transactional
        public int cleanupOldPendingReservations() {
                // 30분 전 시간 계산
                java.time.LocalDateTime cutoffTime = java.time.LocalDateTime.now().minusMinutes(30);
                return reservationRepository.deleteOldPendingReservations(cutoffTime);
        }
}
