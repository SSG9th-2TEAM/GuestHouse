package com.ssg9th2team.geharbang.domain.coupon.scheduler;

import com.ssg9th2team.geharbang.domain.coupon.service.CouponInventoryService;
import com.ssg9th2team.geharbang.domain.coupon.service.UserCouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponScheduler {

    private final UserCouponService userCouponService;
    private final CouponInventoryService couponInventoryService;

    // 매일 자정에 만료된 쿠폰 상태 변경
    @Scheduled(cron = "0 0 0 * * *")
    public void expireCoupons() {
        int expired = userCouponService.expireOverdueCoupons();
        int reset = couponInventoryService.resetDailyInventories();
        log.info("만료 처리된 쿠폰 수: {}, 선착순 재고 초기화 대상: {}", expired, reset);
    }
}
