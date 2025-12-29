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

@Repository
public interface ReservationJpaRepository extends JpaRepository<Reservation, Long>, JpaSpecificationExecutor<Reservation> {

    List<Reservation> findByUserId(Long userId);

    List<Reservation> findByAccommodationsId(Long accommodationsId);

    List<Reservation> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 30분 이상 경과한 대기(0) 상태 예약 삭제
     */
    @Modifying
    @Query("DELETE FROM Reservation r WHERE r.reservationStatus = 0 AND r.createdAt < :cutoffTime")
    int deleteOldPendingReservations(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * 대기 상태 예약 삭제 (사용자가 결제 취소 시)
     */
    @Modifying
    @Query("DELETE FROM Reservation r WHERE r.id = :reservationId AND r.reservationStatus = 0")
    int deletePendingReservation(@Param("reservationId") Long reservationId);

    /**
     * 같은 객실에 날짜가 겹치는 예약이 있는지 확인 (확정된 예약만)
     * 날짜 겹침 조건: 새 체크인 < 기존 체크아웃 AND 새 체크아웃 > 기존 체크인
     */
    @Query("SELECT COUNT(r) > 0 FROM Reservation r " +
            "WHERE r.roomId = :roomId " +
            "AND r.reservationStatus = 2 " + // 확정된 예약만
            "AND r.checkin < :checkout " +
            "AND r.checkout > :checkin")
    boolean hasConflictingReservation(
            @Param("roomId") Long roomId,
            @Param("checkin") LocalDateTime checkin,
            @Param("checkout") LocalDateTime checkout);

    @Query("select count(distinct r.userId) from Reservation r " +
            "where r.createdAt >= :start and r.createdAt < :end")
    long countDistinctGuest(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("select count(distinct a.userId) from Reservation r " +
            "join Accommodation a on r.accommodationsId = a.accommodationsId " +
            "where r.createdAt >= :start and r.createdAt < :end")
    long countDistinctHost(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
