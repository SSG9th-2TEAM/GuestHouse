package com.ssg9th2team.geharbang.domain.room.repository.jpa;

import com.ssg9th2team.geharbang.domain.room.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface RoomJpaRepository extends JpaRepository<Room, Long> {

        /**
         * 비관적 락(Pessimistic Lock)으로 Room 조회
         * 동시성 제어를 위해 먼저 조회하는 트랜잭션이 락을 획득함
         */
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT r FROM Room r WHERE r.roomId = :id")
        Optional<Room> findByIdWithLock(@Param("id") Long id);

        @Modifying
        @Query("UPDATE Room r SET r.maxGuests = r.maxGuests - :guestCount WHERE r.roomId = :roomId AND r.maxGuests >= :guestCount")
        int decreaseMaxGuests(@Param("roomId") Long roomId, @Param("guestCount") Integer guestCount);

        @Query("""
                        SELECT COUNT(r) AS roomCount,
                               MAX(r.maxGuests) AS maxGuests,
                               MIN(r.price) AS minPrice
                        FROM Room r
                        WHERE r.accommodationsId = :accommodationId
                        """)
        RoomStats findRoomStats(@Param("accommodationId") Long accommodationId);

        @Query("""
                        SELECT r.accommodationsId AS accommodationsId,
                               MAX(r.maxGuests) AS maxGuests
                        FROM Room r
                        WHERE r.accommodationsId IN :accommodationIds
                          AND r.roomStatus = 1
                        GROUP BY r.accommodationsId
                        """)
        List<AccommodationGuestStats> findMaxGuestsByAccommodationIds(
                        @Param("accommodationIds") List<Long> accommodationIds);
}
