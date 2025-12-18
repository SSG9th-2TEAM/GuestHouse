package com.ssg9th2team.geharbang.domain.auth.repository;

import com.ssg9th2team.geharbang.domain.auth.entity.User;
import com.ssg9th2team.geharbang.domain.auth.entity.UserTheme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserThemeRepository extends JpaRepository<UserTheme, Long> {

    // 특정 사용자의 모든 테마 조회
    List<UserTheme> findByUser(User user);

    // 특정 사용자의 테마 삭제
    void deleteByUser(User user);

    // 특정 사용자의 특정 테마 삭제
    void deleteByUserAndTheme_Id(User user, Long themeId);
}
