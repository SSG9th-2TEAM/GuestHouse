package com.ssg9th2team.geharbang.domain.main.service;

import com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation;
import com.ssg9th2team.geharbang.domain.accommodation.entity.ApprovalStatus;
import com.ssg9th2team.geharbang.domain.accommodation_theme.entity.AccommodationTheme;
import com.ssg9th2team.geharbang.domain.accommodation_theme.repository.AccommodationThemeRepository;
import com.ssg9th2team.geharbang.domain.auth.entity.User;
import com.ssg9th2team.geharbang.domain.auth.repository.UserRepository;
import com.ssg9th2team.geharbang.domain.main.dto.ListDto;
import com.ssg9th2team.geharbang.domain.main.dto.MainAccommodationListResponse;
import com.ssg9th2team.geharbang.domain.main.dto.PublicListResponse;
import com.ssg9th2team.geharbang.domain.main.repository.AccommodationImageProjection;
import com.ssg9th2team.geharbang.domain.main.repository.ListDtoProjection;
import com.ssg9th2team.geharbang.domain.main.repository.MainRepository;
import com.ssg9th2team.geharbang.domain.room.repository.jpa.AccommodationGuestStats;
import com.ssg9th2team.geharbang.domain.room.repository.jpa.RoomJpaRepository;
import com.ssg9th2team.geharbang.domain.theme.entity.Theme;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
    private final RoomJpaRepository roomJpaRepository;

    private static final int RECOMMENDATION_LIMIT = 5; // 최대 추천 숙소 개수

    @Override
    public MainAccommodationListResponse getMainAccommodationList(Long userId, List<Long> filterThemeIds, String keyword) {
        List<ListDto> recommendedAccommodations = new ArrayList<>();
        List<ListDto> generalAccommodations = new ArrayList<>();

        Set<Long> userThemeIds = getUserThemeIds(userId);
        String normalizedKeyword = normalizeKeyword(keyword);

        if (filterThemeIds != null && !filterThemeIds.isEmpty()) {
            List<Accommodation> filteredAccommodations = loadApprovedAccommodationsByTheme(filterThemeIds, normalizedKeyword);
            generalAccommodations = toListDtos(filteredAccommodations);
        } else if (!userThemeIds.isEmpty()) {
            List<Accommodation> approvedAccommodations = loadApprovedAccommodations(normalizedKeyword);
            // 사용자 테마 기반 추천 로직 (필터 테마 ID가 없을 때만 적용)
            recommendedAccommodations = getRecommendedAccommodations(approvedAccommodations, userThemeIds);
            Set<Long> recommendedAccommodationIds = recommendedAccommodations.stream()
                    .map(ListDto::getAccomodationsId)
                    .collect(Collectors.toSet());

            // 추천 숙소를 제외한 나머지 숙소를 일반 목록에 추가
            List<Accommodation> remainingAccommodations = approvedAccommodations.stream()
                    .filter(acc -> !recommendedAccommodationIds.contains(acc.getAccommodationsId()))
                    .toList();
            generalAccommodations = toListDtos(remainingAccommodations);
        } else {
            // 테마가 없거나 비로그인 상태일 경우 모든 승인된 숙소를 일반 목록에 추가
            List<Accommodation> approvedAccommodations = loadApprovedAccommodations(normalizedKeyword);
            generalAccommodations = toListDtos(approvedAccommodations);
        }

        return MainAccommodationListResponse.builder()
                .recommendedAccommodations(recommendedAccommodations)
                .generalAccommodations(generalAccommodations)
                .build();
    }


    @Override
    public PublicListResponse searchPublicList(
            List<Long> themeIds,
            String keyword,
            int page,
            int size,
            Double minLat,
            Double maxLat,
            Double minLng,
            Double maxLng
    ) {
        PageRequest pageable = PageRequest.of(page, size);
        String normalizedKeyword = normalizeKeyword(keyword);
        Page<ListDtoProjection> resultPage;

        boolean hasBounds = minLat != null && maxLat != null && minLng != null && maxLng != null;
        Double south = null;
        Double north = null;
        Double west = null;
        Double east = null;
        if (hasBounds) {
            south = Math.min(minLat, maxLat);
            north = Math.max(minLat, maxLat);
            west = Math.min(minLng, maxLng);
            east = Math.max(minLng, maxLng);
        }

        if (themeIds == null || themeIds.isEmpty()) {
            if (hasBounds) {
                resultPage = mainRepository.searchPublicListByBounds(normalizedKeyword, south, north, west, east, pageable);
            } else {
                resultPage = mainRepository.searchPublicList(normalizedKeyword, pageable);
            }
        } else {
            if (hasBounds) {
                resultPage = mainRepository.searchPublicListByThemeAndBounds(themeIds, normalizedKeyword, south, north, west, east, pageable);
            } else {
                resultPage = mainRepository.searchPublicListByTheme(themeIds, normalizedKeyword, pageable);
            }
        }

        List<ListDto> items = resultPage.getContent().stream()
                .map(this::toListDto)
                .toList();

        return PublicListResponse.of(items, resultPage);
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String normalized = keyword.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private List<Accommodation> loadApprovedAccommodations(String keyword) {
        if (keyword == null) {
            return mainRepository.findByAccommodationStatusAndApprovalStatus(1, ApprovalStatus.APPROVED);
        }
        return mainRepository.findApprovedByKeyword(keyword);
    }

    private List<Accommodation> loadApprovedAccommodationsByTheme(List<Long> themeIds, String keyword) {
        if (keyword == null) {
            return mainRepository.findByThemeIds(themeIds);
        }
        return mainRepository.findByThemeIdsAndKeyword(themeIds, keyword);
    }

    private List<ListDto> toListDtos(List<Accommodation> accommodations) {
        if (accommodations == null || accommodations.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, String> imageById = loadRepresentativeImages(accommodations);
        Map<Long, Integer> maxGuestsById = loadMaxGuests(accommodations);
        return accommodations.stream()
                .map(accommodation -> toListDto(accommodation, imageById, maxGuestsById))
                .toList();
    }

    private ListDto toListDto(ListDtoProjection projection) {
        return ListDto.builder()
                .accomodationsId(projection.getAccomodationsId())
                .accomodationsName(projection.getAccomodationsName())
                .shortDescription(projection.getShortDescription())
                .city(projection.getCity())
                .district(projection.getDistrict())
                .township(projection.getTownship())
                .latitude(projection.getLatitude())
                .longitude(projection.getLongitude())
                .minPrice(projection.getMinPrice())
                .rating(projection.getRating())
                .reviewCount(projection.getReviewCount())
                .maxGuests(projection.getMaxGuests())
                .imageUrl(projection.getImageUrl())
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
        if (allApprovedAccommodations.isEmpty()) {
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
        List<Accommodation> recommended = allApprovedAccommodations.stream()
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
                .toList();
        return toListDtos(recommended);
    }

    private Map<Long, Integer> loadMaxGuests(List<Accommodation> accommodations) {
        if (accommodations.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = accommodations.stream()
                .map(Accommodation::getAccommodationsId)
                .toList();
        return roomJpaRepository.findMaxGuestsByAccommodationIds(ids)
                .stream()
                .collect(Collectors.toMap(
                        AccommodationGuestStats::getAccommodationsId,
                        stats -> stats.getMaxGuests() != null ? stats.getMaxGuests() : 0,
                        (existing, ignored) -> existing
                ));
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

    private ListDto toListDto(Accommodation accommodation, Map<Long, String> imageById, Map<Long, Integer> maxGuestsById) {
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
                .maxGuests(maxGuestsById.getOrDefault(accommodation.getAccommodationsId(), 0))
                .imageUrl(imageById.get(accommodation.getAccommodationsId()))
                .build();
    }
}
