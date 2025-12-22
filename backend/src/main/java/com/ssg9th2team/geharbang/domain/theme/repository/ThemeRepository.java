package com.ssg9th2team.geharbang.domain.theme.repository;

import com.ssg9th2team.geharbang.domain.theme.entity.Theme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ThemeRepository extends JpaRepository<Theme, Long> {
}
