package com.ssg9th2team.geharbang.domain.reservation.service;

import com.ssg9th2team.geharbang.domain.auth.entity.User;
import com.ssg9th2team.geharbang.domain.auth.repository.UserRepository;
import com.ssg9th2team.geharbang.domain.reservation.dto.ReservationRequestDto;
import com.ssg9th2team.geharbang.domain.reservation.dto.ReservationResponseDto;
import com.ssg9th2team.geharbang.domain.reservation.entity.Reservation;
import com.ssg9th2team.geharbang.domain.reservation.repository.jpa.ReservationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
        private final com.ssg9th2team.geharbang.domain.accommodation.repository.mybatis.AccommodationMapper accommodationMapper;
        private final com.ssg9th2team.geharbang.domain.room.repository.jpa.RoomJpaRepository roomRepository;
        private final UserRepository userRepository;

        @Override
        @Transactional
        public ReservationResponseDto createReservation(ReservationRequestDto requestDto) {
                // JWT 토큰에서 인증된 사용자 정보 추출
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                String email = authentication.getName();
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new IllegalStateException("인증된 사용자를 찾을 수 없습니다: " + email));
                Long userId = user.getId();

                System.out.println("DEBUG: createReservation called for user: " + email + " (ID: " + userId + ")");

                // Room 재고(maxGuests) 차감 (동시성 제어)
                if (requestDto.roomId() == null) {
                        throw new IllegalArgumentException("Room ID is required for reservation.");
                }
                int updatedRows = roomRepository.decreaseMaxGuests(requestDto.roomId(), requestDto.guestCount());
                if (updatedRows == 0) {
                        throw new IllegalStateException("객실 정원이 초과되어 예약할 수 없습니다.");
                }

                // Instant를 LocalDateTime으로 변환 (시스템 기본 시간대 사용)
                java.time.LocalDateTime checkinDateTime = java.time.LocalDateTime.ofInstant(
                                requestDto.checkin(), java.time.ZoneId.systemDefault());
                java.time.LocalDateTime checkoutDateTime = java.time.LocalDateTime.ofInstant(
                                requestDto.checkout(), java.time.ZoneId.systemDefault());

                // 숙박 박수 계산
                int stayNights = (int) ChronoUnit.DAYS.between(
                                checkinDateTime.toLocalDate(),
                                checkoutDateTime.toLocalDate());

                // 쿠폰 할인액 (요청에서 받거나 0)
                int couponDiscount = requestDto.couponDiscountAmount() != null
                                ? requestDto.couponDiscountAmount()
                                : 0;

                // 최종 결제 금액 계산
                int finalAmount = requestDto.totalAmount() - couponDiscount;

                Reservation reservation = Reservation.builder()
                                .accommodationsId(requestDto.accommodationsId())
                                .userId(userId)
                                .checkin(checkinDateTime)
                                .checkout(checkoutDateTime)
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
                                                + accommodation.getAddressDetail()
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
                                + accommodation.getAddressDetail();

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
        public List<ReservationResponseDto> getMyReservations() {
                // JWT 토큰에서 인증된 사용자 정보 추출
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                String email = authentication.getName();
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new IllegalStateException("인증된 사용자를 찾을 수 없습니다: " + email));

                // 사용자의 예약 목록 조회 (숙소 정보 + 이미지 포함)
                return reservationRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                                .stream()
                                .map(reservation -> {
                                        com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation accommodation = accommodationRepository
                                                        .findById(reservation.getAccommodationsId()).orElse(null);

                                        String accName = (accommodation != null) ? accommodation.getAccommodationsName()
                                                        : null;
                                        String accAddress = (accommodation != null)
                                                        ? accommodation.getCity() + " " + accommodation.getDistrict()
                                                                        + " " + accommodation.getAddressDetail()
                                                        : null;

                                        // 숙소 대표 이미지 조회 (sort_order = 0)
                                        String imageUrl = accommodationMapper
                                                        .selectMainImageUrl(reservation.getAccommodationsId());

                                        return ReservationResponseDto.from(reservation, accName, accAddress, imageUrl);
                                })
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
