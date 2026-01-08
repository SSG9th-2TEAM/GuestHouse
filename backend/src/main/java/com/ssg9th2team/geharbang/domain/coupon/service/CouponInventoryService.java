package com.ssg9th2team.geharbang.domain.coupon.service;

import com.ssg9th2team.geharbang.domain.coupon.entity.CouponInventory;
import com.ssg9th2team.geharbang.domain.coupon.repository.jpa.CouponInventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponInventoryService {

    private final CouponInventoryRepository couponInventoryRepository;

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

    @Transactional
    public int resetDailyInventories() {
        LocalDate today = LocalDate.now();
        List<CouponInventory> inventories = couponInventoryRepository.findAll();
        inventories.forEach(inventory -> inventory.resetIfNeeded(today));
        return inventories.size();
    }
}
