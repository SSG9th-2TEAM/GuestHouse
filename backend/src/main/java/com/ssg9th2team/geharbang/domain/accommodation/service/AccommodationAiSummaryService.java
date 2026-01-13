package com.ssg9th2team.geharbang.domain.accommodation.service;

import com.ssg9th2team.geharbang.domain.accommodation.dto.AccommodationAiSummaryResponse;
import com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation;
import com.ssg9th2team.geharbang.domain.accommodation.repository.jpa.AccommodationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccommodationAiSummaryService {

    private final AccommodationJpaRepository accommodationRepository;

    @Transactional(readOnly = true)
    public AccommodationAiSummaryResponse generateSummary(Long accommodationId) {
        // 1. DB에서 숙소 정보 조회 (이름 가져오기 위해)
        Accommodation accommodation = accommodationRepository.findById(accommodationId)
                .orElseThrow(() -> new IllegalArgumentException("Accommodation not found: " + accommodationId));

        String name = accommodation.getAccommodationsName(); // 숙소 이름

        // 2. 이름이 포함된 동적 Mock 데이터 생성
        String mockSummary = String.format(
                "**%s**은(는) 제주 여행객들이 뽑은 감성 숙소입니다.\n\n" +
                        "🔑 **핵심 키워드**: #뷰맛집 #친절한사장님 #조용한힐링\n\n" +
                        "🏡 **분위기 & 특징**\n" +
                        "제주의 낭만이 가득한 따뜻한 감성 숙소예요. 사장님이 직접 관리해서 침구류가 정말 깨끗하고, 도보 3분 거리에 편의점이 있어서 편리해요.\n\n" +
                        "💡 **AI의 이용 꿀팁**\n" +
                        "저녁에는 공용 라운지 파티가 핫해요! 체크인 전 짐 보관도 가능하니 참고하세요.",
                name
        );

        return new AccommodationAiSummaryResponse(mockSummary);
    }
}
