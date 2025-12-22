package com.ssg9th2team.geharbang.domain.accommodation.repository.jpa;

import com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

public interface AccommodationJpaRepository extends JpaRepository<Accommodation, Long> {
}
