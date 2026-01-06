package com.ssg9th2team.geharbang.domain.search.service;

import com.ssg9th2team.geharbang.domain.main.dto.ListDto;
import com.ssg9th2team.geharbang.domain.main.dto.PublicListResponse;
import com.ssg9th2team.geharbang.domain.main.repository.ListDtoProjection;
import com.ssg9th2team.geharbang.domain.search.repository.SearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchServiceImpl implements SearchService {

    private final SearchRepository searchRepository;

    @Override
    public PublicListResponse searchPublicList(
            List<Long> themeIds,
            String keyword,
            int page,
            int size,
            Double minLat,
            Double maxLat,
            Double minLng,
            Double maxLng,
            LocalDateTime checkin,
            LocalDateTime checkout,
            Integer guestCount
    ) {
        PageRequest pageable = PageRequest.of(page, size);
        String normalizedKeyword = normalizeKeyword(keyword);
        Page<ListDtoProjection> resultPage;

        boolean hasBounds = minLat != null && maxLat != null && minLng != null && maxLng != null;
        boolean hasStayDates = checkin != null && checkout != null;
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
                if (hasStayDates) {
                    resultPage = searchRepository.searchPublicListByBounds(
                            normalizedKeyword,
                            south,
                            north,
                            west,
                            east,
                            checkin,
                            checkout,
                            guestCount,
                            pageable
                    );
                } else {
                    resultPage = searchRepository.searchPublicListByBoundsNoDates(
                            normalizedKeyword,
                            south,
                            north,
                            west,
                            east,
                            guestCount,
                            pageable
                    );
                }
            } else {
                if (hasStayDates) {
                    resultPage = searchRepository.searchPublicList(
                            normalizedKeyword,
                            checkin,
                            checkout,
                            guestCount,
                            pageable
                    );
                } else {
                    resultPage = searchRepository.searchPublicListNoDates(
                            normalizedKeyword,
                            guestCount,
                            pageable
                    );
                }
            }
        } else {
            if (hasBounds) {
                if (hasStayDates) {
                    resultPage = searchRepository.searchPublicListByThemeAndBounds(
                            themeIds,
                            normalizedKeyword,
                            south,
                            north,
                            west,
                            east,
                            checkin,
                            checkout,
                            guestCount,
                            pageable
                    );
                } else {
                    resultPage = searchRepository.searchPublicListByThemeAndBoundsNoDates(
                            themeIds,
                            normalizedKeyword,
                            south,
                            north,
                            west,
                            east,
                            guestCount,
                            pageable
                    );
                }
            } else {
                if (hasStayDates) {
                    resultPage = searchRepository.searchPublicListByTheme(
                            themeIds,
                            normalizedKeyword,
                            checkin,
                            checkout,
                            guestCount,
                            pageable
                    );
                } else {
                    resultPage = searchRepository.searchPublicListByThemeNoDates(
                            themeIds,
                            normalizedKeyword,
                            guestCount,
                            pageable
                    );
                }
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
}
