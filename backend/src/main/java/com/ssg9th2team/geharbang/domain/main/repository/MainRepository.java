package com.ssg9th2team.geharbang.domain.main.repository;

import com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MainRepository extends JpaRepository<Accommodation, Long> {
}
