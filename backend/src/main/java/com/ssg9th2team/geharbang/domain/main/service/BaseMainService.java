package com.ssg9th2team.geharbang.domain.main.service;

import com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation;
import com.ssg9th2team.geharbang.domain.accommodation.entity.ApprovalStatus;
import com.ssg9th2team.geharbang.domain.accommodation_theme.entity.AccommodationTheme;
import com.ssg9th2team.geharbang.domain.accommodation_theme.repository.AccommodationThemeRepository;
import com.ssg9th2team.geharbang.domain.auth.entity.User;
import com.ssg9th2team.geharbang.domain.auth.repository.UserRepository;
import com.ssg9th2team.geharbang.domain.main.dto.ListDto;
import com.ssg9th2team.geharbang.domain.main.dto.MainAccommodationListResponse;
import com.ssg9th2team.geharbang.domain.main.repository.AccommodationImageProjection;
import com.ssg9th2team.geharbang.domain.main.repository.MainRepository;
import com.ssg9th2team.geharbang.domain.theme.entity.Theme;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BaseMainService implements MainService {

    private final MainRepository mainRepository;
    private final UserRepository userRepository;
    private final AccommodationThemeRepository accommodationThemeRepository;

    private static final int RECOMMENDATION_LIMIT = 5; // 최대 추천 숙소 개수

    @Override
    public MainAccommodationListResponse getMainAccommodationList(Long userId, List<Long> filterThemeIds) {
        List<Accommodation> allApprovedAccommodations = mainRepository.findByAccommodationStatusAndApprovalStatus(1, ApprovalStatus.APPROVED);

        List<ListDto> recommendedAccommodations = new ArrayList<>();
        List<ListDto> generalAccommodations = new ArrayList<>();

        Set<Long> userThemeIds = getUserThemeIds(userId);

        if (!userThemeIds.isEmpty() && (filterThemeIds == null || filterThemeIds.isEmpty())) {
            // 사용자 테마 기반 추천 로직 (필터 테마 ID가 없을 때만 적용)
            recommendedAccommodations = getRecommendedAccommodations(allApprovedAccommodations, userThemeIds);
            Set<Long> recommendedAccommodationIds = recommendedAccommodations.stream()
                    .map(ListDto::getAccomodationsId)
                    .collect(Collectors.toSet());

            // 추천 숙소를 제외한 나머지 숙소를 일반 목록에 추가
            generalAccommodations = allApprovedAccommodations.stream()
                    .filter(acc -> !recommendedAccommodationIds.contains(acc.getAccommodationsId()))
                    .map(acc -> toListDto(acc, loadRepresentativeImages(List.of(acc)))) // 최적화 필요
                    .toList();
        } else if (filterThemeIds != null && !filterThemeIds.isEmpty()) {
            // 필터 테마 ID가 있을 경우 해당 테마로만 필터링
            Set<Long> filteredAccommodationIds = accommodationThemeRepository.findAccommodationIdsByThemeIds(new HashSet<>(filterThemeIds));
            generalAccommodations = allApprovedAccommodations.stream()
                    .filter(acc -> filteredAccommodationIds.contains(acc.getAccommodationsId()))
                    .map(acc -> toListDto(acc, loadRepresentativeImages(List.of(acc)))) // 최적화 필요
                    .toList();
        } else {
            // 테마가 없거나 비로그인 상태일 경우 모든 승인된 숙소를 일반 목록에 추가
            generalAccommodations = allApprovedAccommodations.stream()
                    .map(acc -> toListDto(acc, loadRepresentativeImages(List.of(acc)))) // 최적화 필요
                    .toList();
        }

        return MainAccommodationListResponse.builder()
                .recommendedAccommodations(recommendedAccommodations)
                .generalAccommodations(generalAccommodations)
                .build();
    }


    private Set<Long> getUserThemeIds(Long userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        return userRepository.findById(userId)
                .map(User::getThemes)
                .orElse(Collections.emptySet())
                .stream()
                .map(Theme::getId)
                .collect(Collectors.toSet());
    }

    private List<ListDto> getRecommendedAccommodations(List<Accommodation> allApprovedAccommodations, Set<Long> userThemeIds) {
        if (userThemeIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 모든 승인된 숙소의 테마 정보 한 번에 가져오기
        List<Long> allAccommodationIds = allApprovedAccommodations.stream()
                .map(Accommodation::getAccommodationsId)
                .toList();
        List<AccommodationTheme> allAccommodationThemes = accommodationThemeRepository.findByAccommodationIds(allAccommodationIds);

        // 숙소별 테마 매칭 점수 계산
        Map<Long, Integer> accommodationScores = new HashMap<>();
        Map<Long, Set<Long>> accommodationToThemes = new HashMap<>();

        for (AccommodationTheme at : allAccommodationThemes) {
            accommodationToThemes.computeIfAbsent(at.getAccommodation().getAccommodationsId(), k -> new HashSet<>()).add(at.getTheme().getId());
        }

        for (Accommodation acc : allApprovedAccommodations) {
            Set<Long> themesOfAccommodation = accommodationToThemes.getOrDefault(acc.getAccommodationsId(), Collections.emptySet());
            int score = 0;
            for (Long userThemeId : userThemeIds) {
                if (themesOfAccommodation.contains(userThemeId)) {
                    score++;
                }
            }
            accommodationScores.put(acc.getAccommodationsId(), score);
        }

        // 점수 기준으로 정렬 및 상위 N개 선택
        return allApprovedAccommodations.stream()
                .filter(acc -> accommodationScores.getOrDefault(acc.getAccommodationsId(), 0) > 0) // 점수가 0보다 큰 숙소만 추천
                .sorted((acc1, acc2) -> {
                    int score1 = accommodationScores.getOrDefault(acc1.getAccommodationsId(), 0);
                    int score2 = accommodationScores.getOrDefault(acc2.getAccommodationsId(), 0);
                    if (score1 != score2) {
                        return Integer.compare(score2, score1); // 점수 내림차순
                    }
                    // 2차 정렬 기준: 평점 내림차순, 리뷰 수 내림차순
                    double rating1 = acc1.getRating() != null ? acc1.getRating() : 0.0;
                    double rating2 = acc2.getRating() != null ? acc2.getRating() : 0.0;
                    if (rating1 != rating2) {
                        return Double.compare(rating2, rating1);
                    }
                    int reviewCount1 = acc1.getReviewCount() != null ? acc1.getReviewCount() : 0;
                    int reviewCount2 = acc2.getReviewCount() != null ? acc2.getReviewCount() : 0;
                    return Integer.compare(reviewCount2, reviewCount1);
                })
                .limit(RECOMMENDATION_LIMIT)
                .map(acc -> toListDto(acc, loadRepresentativeImages(List.of(acc)))) // 최적화 필요
                .toList();
    }


    private Map<Long, String> loadRepresentativeImages(List<Accommodation> accommodations) {
        if (accommodations.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = accommodations.stream()
                .map(Accommodation::getAccommodationsId)
                .toList();
        return mainRepository.findRepresentativeImages(ids)
                .stream()
                .collect(Collectors.toMap(
                        AccommodationImageProjection::getAccommodationsId,
                        AccommodationImageProjection::getImageUrl,
                        (existing, ignored) -> existing
                ));
    }

    private ListDto toListDto(Accommodation accommodation, Map<Long, String> imageById) {
        return ListDto.builder()
                .accomodationsId(accommodation.getAccommodationsId())
                .accomodationsName(accommodation.getAccommodationsName())
                .shortDescription(accommodation.getShortDescription())
                .city(accommodation.getCity())
                .district(accommodation.getDistrict())
                .township(accommodation.getTownship())
                .latitude(accommodation.getLatitude() != null ? accommodation.getLatitude().doubleValue() : null)
                .longitude(accommodation.getLongitude() != null ? accommodation.getLongitude().doubleValue() : null)
                .minPrice(accommodation.getMinPrice() != null ? accommodation.getMinPrice().longValue() : null)
                .rating(accommodation.getRating() != null ? accommodation.getRating().doubleValue() : 0.0)
                .reviewCount(accommodation.getReviewCount())
                .imageUrl(imageById.get(accommodation.getAccommodationsId()))
                .build();
    }
}
