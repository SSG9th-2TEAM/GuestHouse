package com.ssg9th2team.geharbang.domain.coupon.service;

import com.ssg9th2team.geharbang.domain.coupon.dto.UserCouponResponseDto;
import com.ssg9th2team.geharbang.domain.coupon.entity.Coupon;
import com.ssg9th2team.geharbang.domain.coupon.entity.CouponIssueResult;
import com.ssg9th2team.geharbang.domain.coupon.entity.CouponTriggerType;
import com.ssg9th2team.geharbang.domain.coupon.entity.UserCoupon;
import com.ssg9th2team.geharbang.domain.coupon.entity.UserCouponStatus;
import com.ssg9th2team.geharbang.domain.coupon.repository.jpa.CouponJpaRepository;
import com.ssg9th2team.geharbang.domain.coupon.repository.jpa.UserCouponJpaRepository;
import com.ssg9th2team.geharbang.domain.coupon.repository.mybatis.UserCouponMapper;
import com.ssg9th2team.geharbang.domain.review.repository.jpa.ReviewJpaRepository;
import com.ssg9th2team.geharbang.domain.reservation.repository.jpa.ReservationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserCouponServiceImpl implements UserCouponService {

    private final UserCouponJpaRepository userCouponJpaRepository;
    private final CouponJpaRepository couponJpaRepository;
    private final UserCouponMapper userCouponMapper;
    private final ReviewJpaRepository reviewJpaRepository;
    private final ReservationJpaRepository reservationJpaRepository;
    private final CouponInventoryService couponInventoryService;

    // 쿠폰 발급 (수동 - 숙소 상세페이지에서 쿠폰 받기 등)
    @Override
    @Transactional
    public void issueCoupon(Long userId, Long couponId) {
        Coupon coupon = couponJpaRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다."));

        // 이 쿠폰이 '다운로드용'인지 확인!
        if (coupon.getTriggerType() != CouponTriggerType.DOWNLOAD && coupon.getTriggerType() != CouponTriggerType.EVENT) {
             throw new IllegalArgumentException("직접 다운로드할 수 없는 쿠폰입니다.");
        }

        // 활성화 체크
        if (!Boolean.TRUE.equals(coupon.getIsActive())) {
            throw new IllegalArgumentException("현재 발급 불가능한 쿠폰입니다.");
        }

        // 공통 발급 메서드 호출
        CouponIssueResult result = issueToUser(userId, coupon);
        if (result == CouponIssueResult.DUPLICATED) {
            throw new IllegalArgumentException("이미 발급 받은 쿠폰입니다.");
        } else if (result == CouponIssueResult.SOLD_OUT) {
            throw new IllegalStateException("오늘 선착순 수량이 모두 소진되었습니다.");
        }
    }


    // 첫 예약 완료 시 쿠폰 자동 발급 (PaymentService에서 호출)
    @Override
    @Transactional
    public boolean issueFirstReservationCoupon(Long userId) {
        // 내 계정에서 완료된 예약 수 조회 ( reservationStatus가 2인게 몇개인지 조회해라 )
        Long completedCount = reservationJpaRepository.countByUserIdAndReservationStatus(userId, 2);

        // 첫 예약인지 체크
        if(completedCount != 1) {   // 여기서 1은 예약 확정 된게 1개이면
            return false;
        }

        return issueByTrigger(userId, CouponTriggerType.FIRST_RESERVATION);
    }



    // 리뷰 달성 시 쿠폰 자동 발급 (ReviewService에서 호출)
    // 3, 6, 9회 → REVIEW_3 쿠폰 / 10회 → REVIEW_10 쿠폰 / 11회 이후 → 없음
    @Override
    @Transactional
    public boolean issueReviewRewardCoupon(Long userId) {
        long reviewCount = reviewJpaRepository.countByUserIdAndIsDeletedFalse(userId);

        // 10회 달성 시 REVIEW_10 쿠폰 발급
        if (reviewCount == 10) {
            return issueByTrigger(userId, CouponTriggerType.REVIEW_10);
        }

        // 3, 6, 9회일 때만 REVIEW_3 쿠폰 발급 (10회 미만)
        if (reviewCount < 10 && reviewCount % 3 == 0 && reviewCount > 0) {
            return issueByTrigger(userId, CouponTriggerType.REVIEW_3);
        }

        return false;
    }



    // 자동 발급 (트리거 타입 기반) -> 회원가입, 리뷰3번 등등
    @Override
    @Transactional
    public boolean issueByTrigger(Long userId, CouponTriggerType trigger) {
        Coupon coupon = couponJpaRepository.findByTriggerTypeAndIsActiveTrue(trigger)
                .orElse(null);

        if (coupon == null) {
            return false;  // 해당 트리거 타입의 활성화된 쿠폰이 없음
        }

        return issueToUser(userId, coupon) == CouponIssueResult.SUCCESS;
    }


    // 공통 발급 로직 (만료일 자동 계산)
    @Override
    @Transactional
    public CouponIssueResult issueToUser(Long userId, Coupon coupon) {
        // 1. 중복 체크
        if (userCouponJpaRepository.existsByUserIdAndCouponId(userId, coupon.getCouponId())) {
            return CouponIssueResult.DUPLICATED;
        }

        // 2. 선착순 제한 확인
        boolean slotAvailable = couponInventoryService.consumeSlotIfLimited(coupon.getCouponId());
        if (!slotAvailable) {
            return CouponIssueResult.SOLD_OUT;
        }

        // 2. 만료일 계산 (Coupon 엔티티의 calculateExpiryDate 메서드 사용)
        LocalDateTime expiresAt = coupon.calculateExpiryDate();

        // 3. UserCoupon 생성 및 저장
        UserCoupon userCoupon = UserCoupon.issue(userId, coupon.getCouponId(), expiresAt);
        userCouponJpaRepository.save(userCoupon);

        return CouponIssueResult.SUCCESS;
    }



    // 사용 가능 쿠폰, 만료 쿠폰, 사용 완료한 쿠폰 조회
    @Override
    @Transactional(readOnly = true)
    public List<UserCouponResponseDto> getMyCouponsByStatus(Long userId, String status) {
        //  DTO 리스트 반환
        return userCouponMapper.selectMyCouponsByStatus(userId, status);
    }


    // 쿠폰 사용 처리
    @Override
    @Transactional
    public void useCoupon(Long userId, Long userCouponId) {
        // 쿠폰 조회
        UserCoupon userCoupon = userCouponJpaRepository.findById(userCouponId)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다."));

        // 본인 쿠폰인지 확인
        if (!userCoupon.getUserId().equals(userId)) {
            throw new IllegalArgumentException("본인의 쿠폰만 사용할 수 있습니다.");
        }

        // 사용 가능 상태인지 확인
        if (userCoupon.getStatus() != UserCouponStatus.ISSUED) {
            throw new IllegalArgumentException("사용할 수 없는 쿠폰입니다.");
        }

        // 만료 여부 확인
        if (userCoupon.getExpiredAt() != null && userCoupon.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("만료된 쿠폰입니다.");
        }

        userCoupon.use();
    }


    // 쿠폰 복구 처리 (예약 취소 시 호출)
    @Override
    @Transactional
    public void restoreCoupon(Long userId, Long userCouponId) {
        // 쿠폰 조회
        UserCoupon userCoupon = userCouponJpaRepository.findById(userCouponId)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다."));

        // 본인 쿠폰인지 확인
        if (!userCoupon.getUserId().equals(userId)) {
            throw new IllegalArgumentException("본인의 쿠폰만 복구할 수 있습니다.");
        }

        // 사용 완료 상태인지 확인
        if (userCoupon.getStatus() != UserCouponStatus.USED) {
            return;  // 이미 복구되었거나 다른 상태면 무시
        }

        // 만료일 체크 후 복구
        if (userCoupon.isRestorable()) {
            userCoupon.restore();
        }
        // 만료일이 지났으면 복구하지 않음 (만료 상태로 변경)
        else {
            userCoupon.expire();
        }
    }


    // 만료된 쿠폰 상태 변경 (스케줄러에서 호출)
    @Override
    @Transactional
    public int expireOverdueCoupons() {
        List<UserCoupon> expiredCoupons = userCouponJpaRepository
                .findByStatusAndExpiredAtBefore(UserCouponStatus.ISSUED, LocalDateTime.now());

        for (UserCoupon coupon : expiredCoupons) {
            coupon.expire();
        }

        return expiredCoupons.size();
    }
}
