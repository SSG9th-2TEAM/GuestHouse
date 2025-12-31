package com.ssg9th2team.geharbang.domain.review.repository.jpa;

import com.ssg9th2team.geharbang.domain.review.entity.ReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReviewJpaRepository extends JpaRepository<ReviewEntity, Long> {

  //  save(entity)       저장/수정
  //  findById(id)       조회
  //  deleteById(id)     삭제 (hard delete)


    // 리뷰 단건 조회 (삭제되지 않은 것만)
    // 리뷰 수정, 리뷰 삭제에서 사용
    Optional<ReviewEntity> findByReviewIdAndIsDeletedFalse(Long reviewId);

    // 특정 사용자의 특정 리뷰 조회 (권한 체크용)
    Optional<ReviewEntity> findByReviewIdAndUserIdAndIsDeletedFalse(Long reviewId, Long userId);

    // 이미 리뷰 작성했는지 확인
    boolean existsByUserIdAndAccommodationsIdAndIsDeletedFalse(Long userId, Long accommodationsId);

}

