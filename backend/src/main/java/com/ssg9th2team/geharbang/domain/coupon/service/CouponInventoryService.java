package com.ssg9th2team.geharbang.domain.coupon.service;

import com.ssg9th2team.geharbang.domain.coupon.entity.CouponInventory;
import com.ssg9th2team.geharbang.domain.coupon.repository.jpa.CouponInventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class CouponInventoryService {

    private final CouponInventoryRepository couponInventoryRepository;

    /**
     * 선착순 제한이 있는 쿠폰이면 하루 수량을 확인하고 1장 차감한다.
     * 수량이 모두 소진되면 false 반환.
     */
    @Transactional
    public boolean consumeSlotIfLimited(Long couponId) {
        return couponInventoryRepository.findWithLockByCouponId(couponId)
                .map(inventory -> {
                    inventory.resetIfNeeded(LocalDate.now());
                    if (!inventory.hasAvailable()) {
                        return false;
                    }
                    inventory.consumeOne();
                    return true;
                })
                .orElse(true);
    }

    /**
     * 매일 00시에 모든 선착순 쿠폰 재고를 초기화한다.
     * 재고 레코드 개수를 반환한다.
     */
    @Transactional
    public int resetDailyInventories() {
        return couponInventoryRepository.resetAllInventories(LocalDate.now());
    }
}
