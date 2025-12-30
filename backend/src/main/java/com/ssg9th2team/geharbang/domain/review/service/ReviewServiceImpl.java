package com.ssg9th2team.geharbang.domain.review.service;

import com.ssg9th2team.geharbang.domain.auth.entity.User;
import com.ssg9th2team.geharbang.domain.auth.repository.UserRepository;
import com.ssg9th2team.geharbang.domain.reservation.entity.Reservation;
import com.ssg9th2team.geharbang.domain.reservation.repository.jpa.ReservationJpaRepository;
import com.ssg9th2team.geharbang.domain.review.dto.ReviewCreateDto;
import com.ssg9th2team.geharbang.domain.review.dto.ReviewResponseDto;
import com.ssg9th2team.geharbang.domain.review.dto.ReviewTagDto;
import com.ssg9th2team.geharbang.domain.review.entity.ReviewEntity;
import com.ssg9th2team.geharbang.domain.review.entity.ReviewImageEntity;
import com.ssg9th2team.geharbang.domain.review.repository.jpa.ReviewJpaRepository;
import com.ssg9th2team.geharbang.domain.review.repository.mybatis.ReviewMapper;
import com.ssg9th2team.geharbang.global.storage.ObjectStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewMapper reviewMapper;
    private final ReviewJpaRepository reviewJpaRepository;
    private final ObjectStorageService objectStorageService;
    private final ReservationJpaRepository reservationJpaRepository;
    private final UserRepository userRepository;


    // 리뷰 등록
    @Override
    @Transactional
    public void createReview(Long userId, ReviewCreateDto reviewCreateDto) {
       // 해당 숙소 예약 내역 조회 (확정된 예약 + 체크아웃 완료된 것 중 가장 최근 1개)
        Reservation reservation = reservationJpaRepository.findFirstByUserIdAndAccommodationsIdAndReservationStatusAndCheckoutBeforeOrderByCheckoutDesc(
                userId, reviewCreateDto.getAccommodationsId(), 2, LocalDateTime.now())
                .orElseThrow(()-> new IllegalArgumentException("체크아웃이 완료된 예약 내역이 없습니다"));

        // 이미 리뷰 작성했는지 확인
        if(reviewJpaRepository.existsByUserIdAndAccommodationsIdAndIsDeletedFalse(userId, reviewCreateDto.getAccommodationsId())) {
            throw new IllegalArgumentException("이미 리뷰를 작성했습니다");
        }

        // 사용자 닉네임 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        // 리뷰 엔티티 생성
        ReviewEntity reviewEntity = ReviewEntity.builder()
                .accommodationsId(reviewCreateDto.getAccommodationsId())
                .userId(userId)
                .authorName(user.getNickname())  // 작성 시점 닉네임 저장
                .rating(reviewCreateDto.getRating())
                .content(reviewCreateDto.getContent())
                .visitDate(reviewCreateDto.getVisitDate())
                .build();

        // 이미지 업로드 → ReviewImageEntity 추가
        if (reviewCreateDto.getImageUrls() != null && !reviewCreateDto.getImageUrls().isEmpty()) {
            for (int i = 0; i < reviewCreateDto.getImageUrls().size(); i++) {
                // Base64 이미지면 업로드, URL이면 그대로 사용
                String imageUrl = objectStorageService.uploadBase64Image(
                        reviewCreateDto.getImageUrls().get(i), "reviews");
                reviewEntity.addImage(ReviewImageEntity.of(imageUrl, i));
            }
        }

        // 리뷰 저장 (cascade로 이미지도 같이 저장됨)
        ReviewEntity savedReview = reviewJpaRepository.save(reviewEntity);

        // 태그 저장 (review_tag_map 테이블)
        if (reviewCreateDto.getTagIds() != null && !reviewCreateDto.getTagIds().isEmpty()) {
            reviewMapper.insertReviewTags(savedReview.getReviewId(), reviewCreateDto.getTagIds());
        }
    }


    @Override
    public void updateReview(Long userId, Long accommodationId) {
        // TODO: 리뷰 수정 구현
    }




    // 숙소별 리뷰 조회
    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getReviewsByAccommodation(Long accommodationsId) {
        return reviewMapper.selectReviewsByAccommodationId(accommodationsId);
    }



    // 전체 태그 목록 조회
    @Override
    @Transactional(readOnly = true)
    public List<ReviewTagDto> getAllReviewTags() {
        return reviewMapper.selectAllReviewTags();
    }
}
