package com.ssg9th2team.geharbang.domain.auth.repository;

import com.ssg9th2team.geharbang.domain.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // 이메일로 사용자 조회
    Optional<User> findByEmail(String email);

    // 이메일 중복 체크
    boolean existsByEmail(String email);

    // 전화번호로 사용자 조회
    Optional<User> findByPhone(String phone);

    // 이름과 전화번호로 사용자 조회
    Optional<User> findByNameAndPhone(String name, String phone);
}
