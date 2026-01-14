package com.ssg9th2team.geharbang.domain.search.service;

import com.ssg9th2team.geharbang.domain.main.dto.ListDto;
import com.ssg9th2team.geharbang.domain.main.dto.PublicListResponse;
import com.ssg9th2team.geharbang.domain.main.repository.ListDtoProjection;
import com.ssg9th2team.geharbang.domain.search.dto.SearchResolveResponse;
import com.ssg9th2team.geharbang.domain.search.dto.SearchSortType;
import com.ssg9th2team.geharbang.domain.search.dto.SearchSuggestionResponse;
import com.ssg9th2team.geharbang.domain.search.repository.SearchRepository;
import com.ssg9th2team.geharbang.domain.search.repository.SearchResolveProjection;
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

    // 검색 제안 상수
    private static final int MAX_SUGGESTION_LIMIT = 20;
    private static final int MIN_KEYWORD_LENGTH = 2;

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
            Integer guestCount,
            Integer minPrice,
            Integer maxPrice,
            boolean includeUnavailable,
            String sort) {
        SearchSortType sortType = SearchSortType.fromValue(sort);
        PageRequest pageable = PageRequest.of(page, size, sortType.toSort());
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

        boolean hasThemes = themeIds != null && !themeIds.isEmpty();
        if (hasStayDates) {
            if (hasThemes && hasBounds) {
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
                        minPrice,
                        maxPrice,
                        includeUnavailable,
                        pageable);
            } else if (hasThemes) {
                resultPage = searchRepository.searchPublicListByTheme(
                        themeIds,
                        normalizedKeyword,
                        checkin,
                        checkout,
                        guestCount,
                        minPrice,
                        maxPrice,
                        includeUnavailable,
                        pageable);
            } else if (hasBounds) {
                resultPage = searchRepository.searchPublicListByBounds(
                        normalizedKeyword,
                        south,
                        north,
                        west,
                        east,
                        checkin,
                        checkout,
                        guestCount,
                        minPrice,
                        maxPrice,
                        includeUnavailable,
                        pageable);
            } else {
                resultPage = searchRepository.searchPublicList(
                        normalizedKeyword,
                        checkin,
                        checkout,
                        guestCount,
                        minPrice,
                        maxPrice,
                        includeUnavailable,
                        pageable);
            }
        } else {
            if (hasThemes && hasBounds) {
                resultPage = searchRepository.searchPublicListByThemeAndBoundsNoDates(
                        themeIds,
                        normalizedKeyword,
                        south,
                        north,
                        west,
                        east,
                        guestCount,
                        minPrice,
                        maxPrice,
                        includeUnavailable,
                        pageable);
            } else if (hasThemes) {
                resultPage = searchRepository.searchPublicListByThemeNoDates(
                        themeIds,
                        normalizedKeyword,
                        guestCount,
                        minPrice,
                        maxPrice,
                        includeUnavailable,
                        pageable);
            } else if (hasBounds) {
                resultPage = searchRepository.searchPublicListByBoundsNoDates(
                        normalizedKeyword,
                        south,
                        north,
                        west,
                        east,
                        guestCount,
                        minPrice,
                        maxPrice,
                        includeUnavailable,
                        pageable);
            } else {
                resultPage = searchRepository.searchPublicListNoDates(
                        normalizedKeyword,
                        guestCount,
                        minPrice,
                        maxPrice,
                        includeUnavailable,
                        pageable);
            }
        }

        List<ListDto> items = resultPage.getContent().stream()
                .map(this::toListDto)
                .toList();

        return PublicListResponse.of(items, resultPage);
    }

    @Override
    public List<SearchSuggestionResponse> suggestPublicSearch(String keyword, int limit) {
        String normalizedKeyword = normalizeKeyword(keyword);
        if (normalizedKeyword == null || normalizedKeyword.length() < MIN_KEYWORD_LENGTH) {
            return List.of();
        }
        if (limit <= 0) {
            return List.of();
        }
        int safeLimit = Math.min(limit, MAX_SUGGESTION_LIMIT);
        int regionLimit = (safeLimit + 1) / 2;
        int accommodationLimit = safeLimit / 2;

        List<String> regions = regionLimit > 0
                ? searchRepository.suggestRegions(normalizedKeyword, PageRequest.of(0, regionLimit))
                : List.of();
        List<String> accommodationNames = searchRepository.suggestAccommodationNames(
                normalizedKeyword,
                PageRequest.of(0, accommodationLimit));

        return java.util.stream.Stream.concat(
                regions.stream()
                        .filter(region -> region != null && !region.trim().isEmpty())
                        .map(SearchSuggestionResponse::region),
                accommodationNames.stream()
                        .filter(name -> name != null && !name.trim().isEmpty())
                        .map(SearchSuggestionResponse::accommodation))
                .toList();
    }

    @Override
    public SearchResolveResponse resolvePublicAccommodation(String keyword) {
        String normalizedKeyword = normalizeKeyword(keyword);
        if (normalizedKeyword == null) {
            return null;
        }

        List<SearchResolveProjection> matches = searchRepository.resolveAccommodationByName(normalizedKeyword);
        if (matches.size() != 1) {
            return null;
        }

        SearchResolveProjection match = matches.get(0);
        return SearchResolveResponse.of(match.getAccommodationsId(), match.getAccommodationsName());
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
                .accommodationsId(projection.getAccommodationsId())
                .accommodationsName(projection.getAccommodationsName())
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
