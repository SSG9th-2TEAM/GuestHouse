package com.ssg9th2team.geharbang.domain.room.repository.jpa;

import com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomJpaRepository extends JpaRepository<Accommodation, Long> {
}
