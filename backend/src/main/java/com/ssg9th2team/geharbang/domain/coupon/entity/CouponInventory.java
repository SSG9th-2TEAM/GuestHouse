package com.ssg9th2team.geharbang.domain.coupon.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "coupon_inventory")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CouponInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventory_id")
    private Long id;

    @Column(name = "coupon_id", unique = true, nullable = false)
    private Long couponId;

    @Column(name = "daily_limit", nullable = false)
    private Integer dailyLimit;

    @Column(name = "available_today", nullable = false)
    private Integer availableToday;

    @Column(name = "last_reset_date")
    private LocalDate lastResetDate;

    public void resetIfNeeded(LocalDate today) {
        if (lastResetDate == null || lastResetDate.isBefore(today)) {
            this.availableToday = dailyLimit;
            this.lastResetDate = today;
        }
    }

    public boolean hasAvailable() {
        return availableToday != null && availableToday > 0;
    }

    public void consumeOne() {
        if (availableToday != null && availableToday > 0) {
            this.availableToday -= 1;
        }
    }
}
