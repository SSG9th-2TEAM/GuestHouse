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
     * 선착순 제한이 있는 쿠폰이면 PESSIMISTIC_WRITE로 재고를 조회한 뒤
     * 일일 재고를 리셋하고 1장을 차감한다.
     *
     * @return 재고가 남아 차감 성공 시 true, 모두 소진 시 false
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
     * JPA 배치 업데이트로 처리하여 대량 데이터를 메모리에 적재하지 않는다.
     *
     * @return 초기화된 레코드 수
     */
    @Transactional
    public int resetDailyInventories() {
        return couponInventoryRepository.resetAllInventories(LocalDate.now());
    }
}
