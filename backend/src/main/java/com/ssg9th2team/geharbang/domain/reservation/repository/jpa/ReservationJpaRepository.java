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

    @Query("select count(distinct r.userId) from Reservation r " +
            "where r.createdAt >= :start and r.createdAt < :end")
    long countDistinctGuest(LocalDateTime start, LocalDateTime end);

    @Query("select count(distinct a.userId) from Reservation r " +
            "join Accommodation a on r.accommodationsId = a.accommodationsId " +
            "where r.createdAt >= :start and r.createdAt < :end")
    long countDistinctHost(LocalDateTime start, LocalDateTime end);
}
