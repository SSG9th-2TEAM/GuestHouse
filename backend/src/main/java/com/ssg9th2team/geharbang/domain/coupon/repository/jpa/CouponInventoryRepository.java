package com.ssg9th2team.geharbang.domain.coupon.repository.jpa;

import com.ssg9th2team.geharbang.domain.coupon.entity.CouponInventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CouponInventoryRepository extends JpaRepository<CouponInventory, Long> {

    Optional<CouponInventory> findByCouponId(Long couponId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ci from CouponInventory ci where ci.couponId = :couponId")
    Optional<CouponInventory> findWithLockByCouponId(@Param("couponId") Long couponId);
}
