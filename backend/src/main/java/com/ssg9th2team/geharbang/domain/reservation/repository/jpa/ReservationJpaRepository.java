package com.ssg9th2team.geharbang.domain.reservation.repository.jpa;

import com.ssg9th2team.geharbang.domain.reservation.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservationJpaRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByUserId(Long userId);

    List<Reservation> findByAccommodationsId(Long accommodationsId);

    List<Reservation> findByUserIdOrderByCreatedAtDesc(Long userId);
}
