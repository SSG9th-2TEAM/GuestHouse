package com.ssg9th2team.geharbang.domain.reservation.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "reservation")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_id")
    private Long id;

    @Column(name = "accommodations_id", nullable = false)
    private Long accommodationsId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "checkin", nullable = false)
    private LocalDateTime checkin;

    @Column(name = "checkout", nullable = false)
    private LocalDateTime checkout;

    @Column(name = "stay_nights", nullable = false)
    private Integer stayNights;

    @Column(name = "guest_count", nullable = false)
    private Integer guestCount;

    @Column(name = "reservation_status", nullable = false)
    private Integer reservationStatus;

    @Column(name = "total_amount_before_dc", nullable = false)
    private Integer totalAmountBeforeDc;

    @Column(name = "coupon_discount_amount", nullable = false)
    private Integer couponDiscountAmount;

    @Column(name = "final_payment_amount", nullable = false)
    private Integer finalPaymentAmount;

    @Column(name = "payment_status", nullable = false)
    private Integer paymentStatus;

    @Column(name = "reserver_name", nullable = false, length = 50)
    private String reserverName;

    @Column(name = "reserver_phone", nullable = false, length = 20)
    private String reserverPhone;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
