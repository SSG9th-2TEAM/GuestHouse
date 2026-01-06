package com.ssg9th2team.geharbang.domain.coupon.repository.jpa;

import com.ssg9th2team.geharbang.domain.coupon.entity.UserCoupon;
import com.ssg9th2team.geharbang.domain.coupon.entity.UserCouponStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface UserCouponJpaRepository extends JpaRepository<UserCoupon, Long> {


    // 중복 쿠폰 발급 체크 ( 같은 쿠폰 또 주면 안됨)
    boolean existsByUserIdAndCouponId(Long userId, Long couponId);

    // 만료 대상 쿠폰 조회 (ISSUED 상태이면서 만료일이 지난 쿠폰)
    List<UserCoupon> findByStatusAndExpiredAtBefore(UserCouponStatus status, LocalDateTime now);

    List<UserCoupon> findByUserIdAndStatus(Long userId, UserCouponStatus status);

    void deleteAllByUserId(Long userId);
}
