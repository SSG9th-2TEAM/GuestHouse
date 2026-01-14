package com.ssg9th2team.geharbang.domain.accommodation.service;

import com.ssg9th2team.geharbang.domain.accommodation.dto.AccommodationAiSummaryResponse;
import com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation;
import com.ssg9th2team.geharbang.domain.accommodation.repository.jpa.AccommodationJpaRepository;
import com.ssg9th2team.geharbang.domain.review.repository.jpa.ReviewJpaRepository;
import com.ssg9th2team.geharbang.domain.review.repository.mybatis.ReviewMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccommodationAiSummaryService {

    private final AccommodationJpaRepository accommodationRepository;
    private final ReviewMapper reviewMapper;
    private final ReviewJpaRepository reviewRepository;

    @Transactional(readOnly = true)
    public AccommodationAiSummaryResponse generateSummary(Long accommodationId) {
        // 1. DB에서 숙소 정보 조회
        Accommodation accommodation = accommodationRepository.findById(accommodationId)
                .orElseThrow(() -> new IllegalArgumentException("Accommodation not found: " + accommodationId));

        String name = accommodation.getAccommodationsName();
        String address = String.format("%s %s %s %s",
                accommodation.getCity(),
                accommodation.getDistrict(),
                accommodation.getTownship(),
                accommodation.getAddressDetail()).trim();
        String description = accommodation.getAccommodationsDescription() != null ? accommodation.getAccommodationsDescription() : "";

        // 2. 상위 3개 태그 조회
        List<String> topTags = reviewMapper.selectTop3TagsByAccommodationId(accommodationId);

        String keywords;
        String moodDescription;
        String tip;

        if (!topTags.isEmpty()) {
            // 태그 데이터가 있을 경우
            keywords = topTags.stream()
                    .map(tag -> "#" + tag.replace(" ", ""))
                    .collect(Collectors.joining(" "));

            String firstTag = topTags.get(0);
            String secondTag = topTags.size() > 1 ? topTags.get(1) : "";

            if (!secondTag.isEmpty()) {
                moodDescription = String.format("이 숙소를 다녀간 여행객들은 <strong>'%s'</strong>, <strong>'%s'</strong> 점을 최고의 장점으로 꼽았습니다. 실제 데이터가 증명하는 찐 맛집입니다!", firstTag, secondTag);
            } else {
                moodDescription = String.format("이 숙소를 다녀간 여행객들은 <strong>'%s'</strong> 점을 최고의 장점으로 꼽았습니다. 실제 데이터가 증명하는 찐 맛집입니다!", firstTag);
            }

            // 1위 태그에 따른 팁 생성
            if (firstTag.contains("파티")) {
                tip = "파티 참석을 원하시면 미리 신청하세요! 새로운 만남이 기다리고 있습니다.";
            } else if (firstTag.contains("조용") || firstTag.contains("힐링")) {
                tip = "조용한 휴식을 위해 소등 시간을 지켜주세요. 온전한 쉼을 즐길 수 있습니다.";
            } else if (firstTag.contains("뷰") || firstTag.contains("사진") || firstTag.contains("감성")) {
                tip = "인생샷 포인트는 옥상입니다! 카메라를 꼭 챙기세요.";
            } else if (firstTag.contains("깨끗") || firstTag.contains("침구")) {
                tip = "깔끔한 잠자리에서 꿀잠 예약입니다. 편안한 밤 보내세요.";
            } else if (firstTag.contains("조식") || firstTag.contains("맛")) {
                tip = "조식 맛집으로 소문난 곳입니다. 아침 식사를 꼭 챙겨 드세요!";
            } else {
                tip = "인기 숙소이니 마감 전 예약을 서두르세요! 체크인 전 짐 보관 가능 여부를 미리 확인하면 더 편한 여행이 될 거예요.";
            }

        } else {
            // 태그 데이터가 없을 경우 (기존 로직 Fallback)
            if (description.contains("파티")) {
                keywords = "#파티맛집 #새로운만남";
                moodDescription = "활기찬 에너지와 새로운 만남이 있는 곳입니다.";
                tip = "파티 참석을 원하시면 미리 신청하세요!";
            } else if (description.contains("조용") || description.contains("힐링")) {
                keywords = "#조용한저녁 #불멍타임";
                moodDescription = "조용한 휴식과 온전한 힐링을 즐길 수 있는 곳입니다.";
                tip = "조용한 휴식을 위해 소등 시간을 지켜주세요.";
            } else if (description.contains("감성")) {
                keywords = "#감성숙소 #인생샷명소";
                moodDescription = "감각적인 인테리어와 포토존이 가득한 곳입니다.";
                tip = "인생샷 포인트는 옥상입니다!";
            } else {
                keywords = "#가성비갑 #편안한잠자리";
                moodDescription = "편안하고 아늑한 잠자리를 제공하는 가성비 좋은 숙소입니다.";
                tip = "인기 숙소이니 마감 전 예약을 서두르세요!";
            }
        }

        // 위치 태그 생성 (공통)
        String locationTag;
        if (address.contains("애월")) {
            locationTag = "제주 서쪽의 핫플, 애월";
        } else if (address.contains("성산")) {
            locationTag = "일출이 아름다운 성산";
        } else if (address.contains("함덕")) {
            locationTag = "에메랄드빛 바다, 함덕";
        } else if (address.contains("서귀포")) {
            locationTag = "따뜻한 남쪽 나라, 서귀포";
        } else {
            locationTag = "제주 여행의 중심";
        }

        // 리뷰 개수 조회 및 Footer 생성
        long reviewCount = reviewRepository.countByAccommodationsIdAndIsDeletedFalse(accommodationId);
        String footer;
        if (reviewCount > 0) {
            footer = String.format("<span style=\"font-size: 13px; color: #9ca3af; margin-top: 20px; text-align: right; display: block;\">🔍 최근 <strong>%d건</strong>의 실제 방문자 리뷰와 데이터를 기반으로 분석했습니다.</span>", reviewCount);
        } else {
            footer = "<span style=\"font-size: 13px; color: #9ca3af; margin-top: 20px; text-align: right; display: block;\">🔍 숙소 상세 정보를 기반으로 분석했습니다.</span>";
        }

        // 최종 요약문 생성
        String summary = String.format(
                "<strong>%s</strong>은(는) <strong>%s</strong>에 위치한 매력적인 숙소입니다.<br><br>" +
                        "🔑 <strong>핵심 키워드</strong>: %s #제주감성<br><br>" +
                        "🏡 <strong>분위기 & 특징</strong><br>" +
                        "%s<br><br>" +
                        "💡 <strong>AI의 이용 꿀팁</strong><br>" +
                        "%s" +
                        "%s", // Footer 추가
                name,
                locationTag,
                keywords,
                moodDescription,
                tip,
                footer
        );

        return new AccommodationAiSummaryResponse(summary);
    }
}
