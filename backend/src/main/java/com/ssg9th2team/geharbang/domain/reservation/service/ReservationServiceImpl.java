package com.ssg9th2team.geharbang.domain.reservation.service;

import com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation;
import com.ssg9th2team.geharbang.domain.accommodation.repository.jpa.AccommodationJpaRepository;
import com.ssg9th2team.geharbang.domain.accommodation.repository.mybatis.AccommodationMapper;
import com.ssg9th2team.geharbang.domain.auth.entity.User;
import com.ssg9th2team.geharbang.domain.auth.repository.UserRepository;
import com.ssg9th2team.geharbang.domain.payment.entity.Payment;
import com.ssg9th2team.geharbang.domain.payment.repository.jpa.PaymentJpaRepository;
import com.ssg9th2team.geharbang.domain.payment.service.PaymentService;
import com.ssg9th2team.geharbang.domain.reservation.dto.ReservationRequestDto;
import com.ssg9th2team.geharbang.domain.reservation.dto.ReservationResponseDto;
import com.ssg9th2team.geharbang.domain.reservation.entity.Reservation;
import com.ssg9th2team.geharbang.domain.reservation.repository.jpa.ReservationJpaRepository;
import com.ssg9th2team.geharbang.domain.review.repository.jpa.ReviewJpaRepository;
import com.ssg9th2team.geharbang.domain.room.repository.jpa.RoomJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationServiceImpl implements ReservationService {

        private final ReservationJpaRepository reservationRepository;
        private final AccommodationJpaRepository accommodationRepository;
        private final AccommodationMapper accommodationMapper;
        private final UserRepository userRepository;
        private final ReviewJpaRepository reviewJpaRepository;
        private final PaymentService paymentService;
        private final RoomJpaRepository roomJpaRepository;
        private final PaymentJpaRepository paymentRepository;

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

                // Room ID 필수 확인
                if (requestDto.roomId() == null) {
                        throw new IllegalArgumentException("Room ID is required for reservation.");
                }

                // [동시성 제어] 비관적 락(Pessimistic Lock)으로 Room 선점
                // 먼저 조회하는 트랜잭션이 락을 획득하고, 후속 요청은 대기함 (직렬화)
                com.ssg9th2team.geharbang.domain.room.entity.Room room = roomJpaRepository
                                .findByIdWithLock(requestDto.roomId())
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "객실을 찾을 수 없습니다: " + requestDto.roomId()));

                // Instant를 LocalDate로 변환 (시스템 기본 시간대 사용)
                java.time.LocalDate checkinDate = java.time.LocalDateTime.ofInstant(
                                requestDto.checkin(), java.time.ZoneId.systemDefault()).toLocalDate();
                java.time.LocalDate checkoutDate = java.time.LocalDateTime.ofInstant(
                                requestDto.checkout(), java.time.ZoneId.systemDefault()).toLocalDate();

                // 시간 강제 설정: 체크인 15:00, 체크아웃 11:00
                java.time.LocalDateTime checkinDateTime = checkinDate.atTime(15, 0);
                java.time.LocalDateTime checkoutDateTime = checkoutDate.atTime(11, 0);

                // [정원 기반 재고 관리] 날짜가 겹치는 기존 예약의 총 인원 조회
                Integer reservedGuestCount = reservationRepository.sumGuestCountByRoomIdAndDateRange(
                                requestDto.roomId(), checkinDateTime, checkoutDateTime);

                // 잔여 정원 계산 및 검증
                int maxGuests = room.getMaxGuests() != null ? room.getMaxGuests() : 0;
                int remainingCapacity = maxGuests - reservedGuestCount;

                if (requestDto.guestCount() > remainingCapacity) {
                        throw new IllegalStateException(
                                        "정원 초과: 해당 날짜의 남은 정원은 " + remainingCapacity + "명입니다. (최대 정원: " + maxGuests
                                                        + "명)");
                }

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
                                .roomId(requestDto.roomId())
                                .userId(userId)
                                .checkin(checkinDateTime)
                                .checkout(checkoutDateTime)
                                .stayNights(stayNights)
                                .guestCount(requestDto.guestCount())
                                .reservationStatus(0) // 0: 결제 대기
                                .totalAmountBeforeDc(requestDto.totalAmount())
                                .userCouponId(requestDto.userCouponId())
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

                Accommodation accommodation = accommodationRepository
                                .findById(reservation.getAccommodationsId())
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "숙소를 찾을 수 없습니다: " + reservation.getAccommodationsId()));

                String address = accommodation.getCity() + " " + accommodation.getDistrict() + " "
                                + accommodation.getAddressDetail();

                // Payment에서 paymentMethod 조회
                String paymentMethod = paymentRepository.findByReservationId(reservationId)
                                .map(Payment::getPaymentMethod)
                                .orElse(null);

                return ReservationResponseDto.from(reservation, accommodation.getAccommodationsName(), address, null,
                                false, paymentMethod);
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

                Long userId = user.getId();

                // 사용자의 예약 목록 조회 (숙소 정보 + 이미지 + 리뷰 작성 여부 포함)
                // DB 레벨에서 결제 완료된 예약만 조회 (reservationStatus >= 2: 확정 이상)
                return reservationRepository.findCompletedReservationsByUserIdOrderByCreatedAtDesc(userId)
                                .stream()
                                .map(reservation -> {
                                        Accommodation accommodation = accommodationRepository
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

                                        // 해당 숙소에 리뷰 작성 여부 확인
                                        Boolean hasReview = reviewJpaRepository
                                                        .existsByUserIdAndAccommodationsIdAndIsDeletedFalse(
                                                                        userId, reservation.getAccommodationsId());

                                        return ReservationResponseDto.from(reservation, accName, accAddress, imageUrl,
                                                        hasReview);
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
        public void deleteCompletedReservation(Long reservationId) {
                // 디버깅을 위해 예약 정보 조회
                Reservation r = reservationRepository.findById(reservationId)
                                .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다: " + reservationId));

                java.time.LocalDateTime now = java.time.LocalDateTime.now();

                System.out.println("DEBUG: Deleting reservation " + reservationId);
                System.out.println(" - Status: " + r.getReservationStatus());
                System.out.println(" - Checkout: " + r.getCheckout());
                System.out.println(" - Current Time: " + now);

                // 상태 체크
                boolean statusOk = (r.getReservationStatus() == 2 || r.getReservationStatus() == 0);
                // 시간 체크
                boolean timeOk = r.getCheckout().isBefore(now);

                if (!statusOk) {
                        throw new IllegalArgumentException(
                                        String.format("삭제 실패: 예약 상태가 '확정(2)' 또는 '대기(0)'가 아닙니다. (현재 상태: %d)",
                                                        r.getReservationStatus()));
                }

                if (!timeOk) {
                        throw new IllegalArgumentException(String.format(
                                        "삭제 실패: 체크아웃 시간이 아직 지나지 않았습니다. (체크아웃: %s, 현재: %s)", r.getCheckout(), now));
                }

                // 결제 데이터(Payment, PaymentRefund) 먼저 삭제 (FK 제약조건 해결) -> Soft Delete로 변경되면서 제거
                // (데이터 보존)
                // paymentService.deleteAllPaymentDataByReservationId(reservationId);

                // 현재 시간 기준으로 체크인이 지난 확정 예약만 삭제 가능
                int deleted = reservationRepository.deleteCompletedReservation(reservationId, now);
                if (deleted == 0) {
                        // 위 검증을 통과했는데 여기서 0이면 뭔가 이상함 (동시성 문제 등)
                        throw new IllegalArgumentException("이용 완료된 예약만 삭제할 수 있습니다. (DB 삭제 0건)");
                }
        }

        @Override
        @Transactional
        public int cleanupOldPendingReservations() {
                // 30분 전 시간 계산
                java.time.LocalDateTime cutoffTime = java.time.LocalDateTime.now().minusMinutes(30);
                return reservationRepository.deleteOldPendingReservations(cutoffTime);
        }

        // 객실별 예약 조회
        @Override
        public List<ReservationResponseDto> getReservationByUserId(Long roomId) {
                List<Reservation> reservations = reservationRepository.findByRoomId(roomId);
                return reservations.stream()
                                .map(ReservationResponseDto::from)
                                .collect(Collectors.toList());
        }

        // 전체 예약 목록 조회 (관리자용)
        @Override
        public List<ReservationResponseDto> getAllReservations() {
                return reservationRepository.findAll().stream()
                                .map(ReservationResponseDto::from)
                                .collect(Collectors.toList());
        }
}
