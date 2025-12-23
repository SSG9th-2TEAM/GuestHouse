package com.ssg9th2team.geharbang.domain.main.repository;

import com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MainRepository extends JpaRepository<Accommodation, Long> {
    @Query(value = """
            SELECT DISTINCT a.*
            FROM accommodation a
            JOIN accommodation_theme at ON at.accommodations_id = a.accommodations_id
            WHERE at.theme_id IN (:themeIds)
            """, nativeQuery = true)
    List<Accommodation> findByThemeIds(@Param("themeIds") List<Long> themeIds);
}
