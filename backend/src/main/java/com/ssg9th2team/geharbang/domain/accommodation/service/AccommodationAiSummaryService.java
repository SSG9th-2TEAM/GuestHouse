package com.ssg9th2team.geharbang.domain.accommodation.service;

import com.ssg9th2team.geharbang.domain.accommodation.dto.AccommodationAiSummaryResponse;
import com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation;
import com.ssg9th2team.geharbang.domain.accommodation.repository.jpa.AccommodationJpaRepository;
import com.ssg9th2team.geharbang.domain.review.entity.ReviewEntity;
import com.ssg9th2team.geharbang.domain.review.repository.jpa.ReviewJpaRepository;
import com.ssg9th2team.geharbang.domain.review.repository.mybatis.ReviewMapper;
import com.ssg9th2team.geharbang.domain.room.entity.Room;
import com.ssg9th2team.geharbang.domain.room.repository.jpa.RoomJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AccommodationAiSummaryService {

    private final AccommodationJpaRepository accommodationRepository;
    private final ReviewMapper reviewMapper;
    private final ReviewJpaRepository reviewRepository;
    private final RoomJpaRepository roomRepository;
    private final GuestSummaryFacade guestSummaryFacade;

    @Transactional(readOnly = true)
    public AccommodationAiSummaryResponse generateSummary(Long accommodationId) {
        // 1. 데이터 조회
        Accommodation accommodation = accommodationRepository.findById(accommodationId)
                .orElseThrow(() -> new IllegalArgumentException("Accommodation not found: " + accommodationId));

        List<String> topTags = reviewMapper.selectTop3TagsByAccommodationId(accommodationId);
        long reviewCount = reviewRepository.countByAccommodationsIdAndIsDeletedFalse(accommodationId);
        
        // 최신 리뷰 10개 조회 (OpenAI 프롬프트용)
        List<ReviewEntity> reviews = reviewRepository.findByAccommodationsIdAndIsDeletedFalse(
                accommodationId,
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        List<Room> rooms = roomRepository.findByAccommodationsId(accommodationId);
        int minPrice = rooms.stream()
                .map(Room::getPrice)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .min()
                .orElse(0);
        boolean hasDormitory = rooms.stream()
                .anyMatch(room -> room.getRoomName() != null && room.getRoomName().contains("도미토리"));

        // 2. Facade를 통해 요약 생성 (OpenAI -> Rule-based Fallback)
        AccommodationAiSummaryResponse response = guestSummaryFacade.generate(accommodation, reviews, topTags, minPrice, hasDormitory);

        // 3. 리뷰 개수는 Service에서 최종 주입 (Client는 0으로 반환했을 수 있음)
        return new AccommodationAiSummaryResponse(
                response.getAccommodationName(),
                response.getLocationTag(),
                response.getKeywords(),
                response.getMoodDescription(),
                response.getTip(),
                reviewCount
        );
    }
}
