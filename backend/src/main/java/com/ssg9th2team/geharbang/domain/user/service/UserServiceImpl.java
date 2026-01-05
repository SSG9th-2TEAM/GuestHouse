package com.ssg9th2team.geharbang.domain.user.service;

import com.ssg9th2team.geharbang.domain.auth.entity.User;
import com.ssg9th2team.geharbang.domain.auth.entity.UserSocial;
import com.ssg9th2team.geharbang.domain.auth.repository.UserRepository;
import com.ssg9th2team.geharbang.domain.auth.repository.UserSocialRepository;
import com.ssg9th2team.geharbang.domain.payment.entity.Payment;
import com.ssg9th2team.geharbang.domain.payment.entity.PaymentRefund;
import com.ssg9th2team.geharbang.domain.payment.repository.jpa.PaymentJpaRepository;
import com.ssg9th2team.geharbang.domain.payment.repository.jpa.PaymentRefundJpaRepository;
import com.ssg9th2team.geharbang.domain.report.repository.jpa.ReviewReportJpaRepository;
import com.ssg9th2team.geharbang.domain.reservation.entity.Reservation;
import com.ssg9th2team.geharbang.domain.reservation.repository.jpa.ReservationJpaRepository;
import com.ssg9th2team.geharbang.domain.review.repository.jpa.ReviewJpaRepository;
import com.ssg9th2team.geharbang.domain.user.dto.DeleteAccountRequest;
import com.ssg9th2team.geharbang.domain.user.dto.UpdateProfileRequest;
import com.ssg9th2team.geharbang.domain.wishlist.repository.jpa.WishlistJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ReservationJpaRepository reservationRepository;
    private final UserSocialRepository userSocialRepository;
    private final PaymentJpaRepository paymentJpaRepository;
    private final PaymentRefundJpaRepository paymentRefundJpaRepository;
    private final ReviewReportJpaRepository reviewReportJpaRepository;
    private final ReviewJpaRepository reviewJpaRepository;
    private final WishlistJpaRepository wishlistJpaRepository;

    @Override
    @Transactional
    public void deleteUser(String email, DeleteAccountRequest deleteAccountRequest) {
        log.info("회원 탈퇴 시도: email={}", email);

        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
            Long userId = user.getId();
            log.info("사용자 조회 성공: userId={}, email={}", userId, email);

            // 1. 활성 예약 확인 (진행중인 예약이 있으면 탈퇴 불가)
            List<Reservation> activeReservations = reservationRepository.findActiveReservationsByUserId(userId);
            if (!activeReservations.isEmpty()) {
                log.warn("사용자 {} 탈퇴 실패 - 활성 예약 {}건 존재", email, activeReservations.size());
                throw new IllegalStateException("진행 중인 예약이 있어 탈퇴할 수 없습니다.");
            }

            // --- 연관 데이터 삭제 시작 ---

            // 2. 사용자의 모든 예약 조회
            List<Reservation> allReservations = reservationRepository.findAllByUserId(userId);
            if (!allReservations.isEmpty()) {
                List<Long> reservationIds = allReservations.stream()
                        .map(Reservation::getId)
                        .collect(Collectors.toList());

                // 3. 예약에 연결된 결제 기록 조회
                List<Payment> payments = paymentJpaRepository.findByReservationIdIn(reservationIds);
                if (!payments.isEmpty()) {
                    List<Long> paymentIds = payments.stream()
                            .map(Payment::getId)
                            .collect(Collectors.toList());

                    // 4. 결제에 연결된 환불 기록 조회 및 삭제
                    List<PaymentRefund> refunds = paymentIds.stream()
                            .flatMap(paymentId -> paymentRefundJpaRepository.findByPaymentId(paymentId).stream())
                            .collect(Collectors.toList());

                    if (!refunds.isEmpty()) {
                        paymentRefundJpaRepository.deleteAllInBatch(refunds);
                        log.info("사용자 {}의 환불 기록 {}건 삭제 완료", email, refunds.size());
                    }

                    // 5. 결제 기록 삭제
                    paymentJpaRepository.deleteAllInBatch(payments);
                    log.info("사용자 {}의 결제 기록 {}건 삭제 완료", email, payments.size());
                }

                // 6. 예약 기록 삭제
                reservationRepository.deleteAllInBatch(allReservations);
                log.info("사용자 {}의 예약 기록 삭제 완료", email);
            }

            // 7. 리뷰 신고 기록 삭제
            reviewReportJpaRepository.deleteAllByUserId(userId);
            log.info("사용자 {}의 리뷰 신고 기록 삭제 완료", email);

            // 8. 리뷰 기록 삭제
            reviewJpaRepository.deleteAllByUserId(userId);
            log.info("사용자 {}의 리뷰 기록 삭제 완료", email);

            // 9. 위시리스트 삭제
            wishlistJpaRepository.deleteAllByUserId(userId);
            log.info("사용자 {}의 위시리스트 삭제 완료", email);

            // 10. 소셜 로그인 정보 삭제
            List<UserSocial> userSocials = userSocialRepository.findByUser(user);
            if (!userSocials.isEmpty()) {
                userSocialRepository.deleteAll(userSocials);
                log.info("사용자 {}의 연결된 소셜 로그인 정보 {}개 삭제", email, userSocials.size());
            }

            // 11. 최종 사용자 삭제
            log.info("사용자 {} 탈퇴 처리 시작. 사유: {}, 기타: {}", email, deleteAccountRequest.getReasons(),
                    deleteAccountRequest.getOtherReason());
            userRepository.delete(user);
            log.info("사용자 {} 탈퇴 성공", email);

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("회원 탈퇴 실패 (검증 오류): email={}, error={}", email, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("회원 탈퇴 중 예상치 못한 오류 발생: email={}", email, e);
            throw new RuntimeException("회원 탈퇴 처리 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    @Override
    @Transactional
    public void updateUserProfile(String email, UpdateProfileRequest request) {
        User user = getUserByEmail(email);

        // 닉네임 중복 확인 (자기 자신은 제외)
        if (userRepository.existsByNicknameAndIdNot(request.getNickname(), user.getId())) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        user.updateProfile(request.getNickname(), request.getPhone(), request.getGender());
    }
}
