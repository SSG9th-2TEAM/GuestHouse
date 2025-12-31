package com.ssg9th2team.geharbang.domain.review.controller;

import com.ssg9th2team.geharbang.domain.auth.entity.User;
import com.ssg9th2team.geharbang.domain.auth.repository.UserRepository;
import com.ssg9th2team.geharbang.domain.review.dto.ReviewCreateDto;
import com.ssg9th2team.geharbang.domain.review.dto.ReviewUpdateDto;
import com.ssg9th2team.geharbang.domain.review.dto.ReviewResponseDto;
import com.ssg9th2team.geharbang.domain.review.dto.ReviewTagDto;
import com.ssg9th2team.geharbang.domain.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final UserRepository userRepository;

    // 리뷰 등록
    @PostMapping
    public ResponseEntity<String> createReview(
            Authentication authentication,
            @RequestBody ReviewCreateDto reviewCreateDto) {

        // JWT에서 email 추출 → userId 조회
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        reviewService.createReview(user.getId(), reviewCreateDto);

        return ResponseEntity.ok("리뷰가 등록되었습니다");
    }

    // 리뷰 수정
    @PutMapping("/{reviewId}")
    public ResponseEntity<String> updateReview( // HTTP 응답 본문(body)에 String 타입 데이터를 담아서 보내겠다
            Authentication authentication,
            @PathVariable Long reviewId,  // url에 리뷰 아이디 /api/reviews/3
            @RequestBody ReviewUpdateDto reviewUpdateDto) {

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        reviewService.updateReview(user.getId(), reviewId, reviewUpdateDto);

        return ResponseEntity.ok("리뷰가 수정되었습니다");
    }



    // 내 리뷰 조회
    @GetMapping("/my")
    public ResponseEntity<List<ReviewResponseDto>> getMyReviews(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        List<ReviewResponseDto> reviews = reviewService.getReviewsByUserId(user.getId());
        return ResponseEntity.ok(reviews);
    }

    // 숙소별 리뷰 조회
    @GetMapping("/accommodations/{accommodationsId}")
    public ResponseEntity<List<ReviewResponseDto>> getReviewsByAccommodation(
            @PathVariable Long accommodationsId) {

        List<ReviewResponseDto> reviews = reviewService.getReviewsByAccommodation(accommodationsId);

        return ResponseEntity.ok(reviews);
    }



    // 전체 태그 목록 조회
    @GetMapping("/tags")
    public ResponseEntity<List<ReviewTagDto>> getAllReviewTags() {
        List<ReviewTagDto> tags = reviewService.getAllReviewTags();
        return ResponseEntity.ok(tags);
    }
}
