package com.ssg9th2team.geharbang.domain.auth.repository;

import com.ssg9th2team.geharbang.domain.auth.entity.Theme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ThemeRepository extends JpaRepository<Theme, Long> {

    // 테마 이름으로 조회
    Optional<Theme> findByThemeName(String themeName);

    // 테마 카테고리로 조회
    List<Theme> findByThemeCategory(String themeCategory);
}
