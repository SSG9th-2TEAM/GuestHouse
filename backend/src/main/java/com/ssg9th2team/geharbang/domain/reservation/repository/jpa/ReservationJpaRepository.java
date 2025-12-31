package com.ssg9th2team.geharbang.domain.reservation.repository.jpa;

import com.ssg9th2team.geharbang.domain.reservation.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationJpaRepository
                extends JpaRepository<Reservation, Long>, JpaSpecificationExecutor<Reservation> {

        // Soft Delete 적용: 삭제되지 않은 예약만 조회

        @Query("SELECT r FROM Reservation r WHERE r.userId = :userId AND r.isDeleted = false")
        List<Reservation> findByUserId(@Param("userId") Long userId);

        @Query("SELECT r FROM Reservation r WHERE r.accommodationsId = :accommodationsId AND r.isDeleted = false")
        List<Reservation> findByAccommodationsId(@Param("accommodationsId") Long accommodationsId);

        @Query("SELECT r FROM Reservation r WHERE r.userId = :userId AND r.isDeleted = false ORDER BY r.createdAt DESC")
        List<Reservation> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

        // roomId로 예약 조회
        @Query("SELECT r FROM Reservation r WHERE r.roomId = :roomId AND r.isDeleted = false")
        List<Reservation> findByRoomId(@Param("roomId") Long roomId);

        /**
         * 30분 이상 경과한 대기(0) 상태 예약 삭제 (물리적 삭제 유지)
         */
        @Modifying
        @Query("DELETE FROM Reservation r WHERE r.reservationStatus = 0 AND r.createdAt < :cutoffTime")
        int deleteOldPendingReservations(@Param("cutoffTime") LocalDateTime cutoffTime);

        /**
         * 대기 상태 예약 삭제 (사용자가 결제 취소 시) (물리적 삭제 유지)
         */
        @Modifying
        @Query("DELETE FROM Reservation r WHERE r.id = :reservationId AND r.reservationStatus = 0")
        int deletePendingReservation(@Param("reservationId") Long reservationId);

        /**
         * 이용 완료된 예약 삭제 (Soft Delete 처리)
         * 체크아웃 시간이 지난 확정 또는 대기 예약
         * Soft Delete: is_deleted = true 로 업데이트
         */
        @Modifying
        @Query("UPDATE Reservation r SET r.isDeleted = true WHERE r.id = :reservationId AND (r.reservationStatus = 2 OR r.reservationStatus = 0) AND r.checkout < :now")
        int deleteCompletedReservation(@Param("reservationId") Long reservationId, @Param("now") LocalDateTime now);

        /**
         * 같은 객실에 날짜가 겹치는 예약이 있는지 확인 (확정된 예약만)
         * 날짜 겹침 조건: 새 체크인 < 기존 체크아웃 AND 새 체크아웃 > 기존 체크인
         */
        @Query("SELECT COUNT(r) > 0 FROM Reservation r " +
                        "WHERE r.roomId = :roomId " +
                        "AND r.reservationStatus = 2 " +
                        "AND r.isDeleted = false " + // 삭제된 예약은 제외
                        "AND r.checkin < :checkout " +
                        "AND r.checkout > :checkin")
        boolean hasConflictingReservation(
                        @Param("roomId") Long roomId,
                        @Param("checkin") LocalDateTime checkin,
                        @Param("checkout") LocalDateTime checkout);

        // 리뷰 작성용 조회 메서드 (체크아웃 완료된 예약 중 가장 최근 1개)
        Optional<Reservation> findFirstByUserIdAndAccommodationsIdAndReservationStatusAndCheckoutBeforeOrderByCheckoutDesc(
                        Long userId, Long accommodationsId, Integer reservationStatus, LocalDateTime checkoutBefore);

        @Query("select count(distinct r.userId) from Reservation r " +
                        "where r.createdAt >= :start and r.createdAt < :end")
        long countDistinctGuest(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

        @Query("select count(distinct a.userId) from Reservation r " +
                        "join Accommodation a on r.accommodationsId = a.accommodationsId " +
                        "where r.createdAt >= :start and r.createdAt < :end")
        long countDistinctHost(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

}
