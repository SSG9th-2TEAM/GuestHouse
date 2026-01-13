package com.ssg9th2team.geharbang.domain.accommodation.service;

import com.ssg9th2team.geharbang.domain.accommodation.dto.AccommodationAiSummaryResponse;
import com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation;
import com.ssg9th2team.geharbang.domain.accommodation.repository.jpa.AccommodationJpaRepository;
import com.ssg9th2team.geharbang.domain.report.host.ai.OpenAiSummaryClient;
import com.ssg9th2team.geharbang.domain.review.entity.ReviewEntity;
import com.ssg9th2team.geharbang.domain.review.repository.jpa.ReviewJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccommodationAiSummaryService {

    private final AccommodationJpaRepository accommodationRepository;
    private final ReviewJpaRepository reviewRepository;
    private final OpenAiSummaryClient openAiSummaryClient;

    @Transactional(readOnly = true)
    public AccommodationAiSummaryResponse generateSummary(Long accommodationId) {
        Accommodation accommodation = accommodationRepository.findById(accommodationId)
                .orElseThrow(() -> new IllegalArgumentException("Accommodation not found: " + accommodationId));

        // 최신 리뷰 10개 조회
        List<ReviewEntity> reviews = reviewRepository.findByAccommodationsIdAndIsDeletedFalse(
                accommodationId,
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        return openAiSummaryClient.generateGuestSummary(
                accommodation.getAccommodationsDescription(),
                reviews
        );
    }
}
