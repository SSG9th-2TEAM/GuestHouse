package com.ssg9th2team.geharbang.domain.review.service;

import com.ssg9th2team.geharbang.domain.review.dto.ReviewCreateDto;
import com.ssg9th2team.geharbang.domain.review.dto.ReviewResponseDto;
import com.ssg9th2team.geharbang.domain.review.dto.ReviewTagDto;

import java.util.List;

public interface ReviewService {

    // 리뷰 등록
    void createReview(Long userId, ReviewCreateDto reviewCreateDto);

    // 리뷰 수정
    void updateReview(Long userId, Long accommodationId);

    // 숙소별 리뷰 조회
    List<ReviewResponseDto> getReviewsByAccommodation(Long accommodationsId);

    // 전체 태그 목록 조회
    List<ReviewTagDto> getAllReviewTags();
}
