package com.ssg9th2team.geharbang.domain.reservation.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 대기 목록 엔티티
 * 정원 초과로 예약 실패 시 대기 등록하여 빈자리 알림 수신
 */
@Entity
@Table(name = "waitlist")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Waitlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "waitlist_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "accommodation_id", nullable = false)
    private Long accommodationId;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Column(name = "checkin", nullable = false)
    private LocalDateTime checkin;

    @Column(name = "checkout", nullable = false)
    private LocalDateTime checkout;

    @Column(name = "guest_count", nullable = false)
    private Integer guestCount;

    @Column(name = "is_notified", nullable = false)
    @Builder.Default
    private Boolean isNotified = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 알림 발송 완료 처리
     */
    public void markAsNotified() {
        this.isNotified = true;
    }
}
