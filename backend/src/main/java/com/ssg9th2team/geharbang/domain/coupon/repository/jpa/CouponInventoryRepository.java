package com.ssg9th2team.geharbang.domain.coupon.repository.jpa;

import com.ssg9th2team.geharbang.domain.coupon.entity.CouponInventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface CouponInventoryRepository extends JpaRepository<CouponInventory, Long> {

    /**
     * 쿠폰 ID로 선착순 재고 엔티티 조회.
     */
    Optional<CouponInventory> findByCouponId(Long couponId);

    /**
     * 선착순 차감 시 동시성 제어를 위해 PESSIMISTIC_WRITE로 락을 잡고 조회.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ci from CouponInventory ci where ci.couponId = :couponId")
    Optional<CouponInventory> findWithLockByCouponId(@Param("couponId") Long couponId);

    /**
     * 모든 선착순 쿠폰의 일일 재고를 한 번에 초기화한다.
     * 일자 비교는 DB에서 수행하므로 애플리케이션이 전체 데이터를 읽지 않는다.
     */
    @Modifying(clearAutomatically = true)
    @Query("update CouponInventory ci set ci.availableToday = ci.dailyLimit, ci.lastResetDate = :today "
            + "where ci.lastResetDate is null or ci.lastResetDate < :today")
    int resetAllInventories(@Param("today") LocalDate today);
}
