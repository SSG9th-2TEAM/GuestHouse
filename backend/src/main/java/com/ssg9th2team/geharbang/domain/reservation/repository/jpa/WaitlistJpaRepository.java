package com.ssg9th2team.geharbang.domain.reservation.repository.jpa;

import com.ssg9th2team.geharbang.domain.reservation.entity.Waitlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WaitlistJpaRepository extends JpaRepository<Waitlist, Long> {

    /**
     * 특정 객실/날짜에 대기 중인 사용자 조회 (알림 미발송)
     */
    @Query("SELECT w FROM Waitlist w " +
            "WHERE w.roomId = :roomId " +
            "AND w.isNotified = false " +
            "AND w.checkin < :checkout AND w.checkout > :checkin " +
            "ORDER BY w.createdAt ASC")
    List<Waitlist> findWaitingByRoomAndDateRange(
            @Param("roomId") Long roomId,
            @Param("checkin") LocalDateTime checkin,
            @Param("checkout") LocalDateTime checkout);

    /**
     * 사용자별 대기 목록 조회
     */
    List<Waitlist> findByUserIdAndIsNotifiedFalseOrderByCreatedAtDesc(Long userId);

    /**
     * 중복 대기 등록 확인
     */
    boolean existsByUserIdAndRoomIdAndCheckinAndCheckoutAndIsNotifiedFalse(
            Long userId, Long roomId, LocalDateTime checkin, LocalDateTime checkout);

    /**
     * 30일 이상된 대기 목록 삭제
     */
    @Modifying
    @Query("DELETE FROM Waitlist w WHERE w.createdAt < :cutoffTime")
    int deleteOldWaitlist(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * 사용자별 대기 목록 삭제
     */
    @Modifying
    @Query("DELETE FROM Waitlist w WHERE w.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
