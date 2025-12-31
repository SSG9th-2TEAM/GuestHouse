package com.ssg9th2team.geharbang.domain.room.repository.jpa;

import com.ssg9th2team.geharbang.domain.room.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import java.util.Optional;

public interface RoomJpaRepository extends JpaRepository<Room, Long> {

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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Room r WHERE r.id = :id")
    Optional<Room> findByIdWithLock(@Param("id") Long id);
}
