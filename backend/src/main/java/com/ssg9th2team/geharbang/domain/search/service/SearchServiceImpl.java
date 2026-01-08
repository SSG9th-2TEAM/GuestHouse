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

import org.springframework.data.domain.Sort;

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
            Integer guestCount,
            Integer minPrice,
            Integer maxPrice,
            String sort) {
        Sort sortObj = Sort.by(Sort.Direction.DESC, "accommodationsId");
        if (sort != null && !sort.isEmpty()) {
            switch (sort) {
                case "reviews":
                    sortObj = Sort.by(Sort.Direction.DESC, "reviewCount");
                    break;
                case "rating":
                    sortObj = Sort.by(Sort.Direction.DESC, "rating");
                    break;
                case "priceHigh":
                    sortObj = Sort.by(Sort.Direction.DESC, "minPrice");
                    break;
                case "priceLow":
                    sortObj = Sort.by(Sort.Direction.ASC, "minPrice");
                    break;
                case "recommended":
                default:
                    // Fallback to default or rating
                    if ("recommended".equals(sort)) {
                        sortObj = Sort.by(Sort.Direction.DESC, "rating", "reviewCount");
                    } else {
                        sortObj = Sort.by(Sort.Direction.DESC, "accommodationsId");
                    }
                    break;
            }
        }
        PageRequest pageable = PageRequest.of(page, size, sortObj);
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
                        pageable);
            } else {
                resultPage = searchRepository.searchPublicList(
                        normalizedKeyword,
                        checkin,
                        checkout,
                        guestCount,
                        minPrice,
                        maxPrice,
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
                        pageable);
            } else if (hasThemes) {
                resultPage = searchRepository.searchPublicListByThemeNoDates(
                        themeIds,
                        normalizedKeyword,
                        guestCount,
                        minPrice,
                        maxPrice,
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
                        pageable);
            } else {
                resultPage = searchRepository.searchPublicListNoDates(
                        normalizedKeyword,
                        guestCount,
                        minPrice,
                        maxPrice,
                        pageable);
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
