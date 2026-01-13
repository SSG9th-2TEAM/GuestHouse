package com.ssg9th2team.geharbang.domain.coupon.repository.jpa;

import com.ssg9th2team.geharbang.domain.coupon.entity.UserCoupon;
import com.ssg9th2team.geharbang.domain.coupon.entity.UserCouponStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface UserCouponJpaRepository extends JpaRepository<UserCoupon, Long> {


    // 중복 쿠폰 발급 체크 ( 같은 쿠폰 또 주면 안됨)
    boolean existsByUserIdAndCouponId(Long userId, Long couponId);

    // 만료 대상 쿠폰 조회 (ISSUED 상태이면서 만료일이 지난 쿠폰)
    List<UserCoupon> findByStatusAndExpiredAtBefore(UserCouponStatus status, LocalDateTime now);

    List<UserCoupon> findByUserIdAndStatus(Long userId, UserCouponStatus status);

    void deleteAllByUserId(Long userId);

    @Query("select uc.couponId from UserCoupon uc where uc.userId = :userId")
    Set<Long> findCouponIdsByUserId(@Param("userId") Long userId);

    // [HIGH] 성능 최적화: couponId로 직접 조회 (전체 조회 후 필터링 X)
    List<UserCoupon> findAllByCouponId(Long couponId);

    // [HIGH] OOM 방지: 전체 조회 시 Stream 사용
    @Query("select uc from UserCoupon uc")
    Stream<UserCoupon> streamAll();

    // 일일 쿠폰 리셋: couponId로 모든 발급 기록 삭제
    @Modifying
    @Transactional
    int deleteByCouponId(Long couponId);
}
