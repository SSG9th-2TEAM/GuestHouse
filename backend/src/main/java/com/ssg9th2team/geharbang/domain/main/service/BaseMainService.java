package com.ssg9th2team.geharbang.domain.main.service;

import com.ssg9th2team.geharbang.domain.accommodation.entity.ApprovalStatus;
import com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation;
import com.ssg9th2team.geharbang.domain.main.dto.ListDto;
import com.ssg9th2team.geharbang.domain.main.repository.AccommodationImageProjection;
import com.ssg9th2team.geharbang.domain.main.repository.MainRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BaseMainService implements MainService {

    private final MainRepository mainRepository;

    @Override
    public List<ListDto> findByTheme(List<Long> themeIds) {
        List<Accommodation> accommodations = (themeIds == null || themeIds.isEmpty())
                ? mainRepository.findByAccommodationStatusAndApprovalStatus(1, ApprovalStatus.APPROVED)
                : mainRepository.findByThemeIds(themeIds);
        Map<Long, String> imageById = loadRepresentativeImages(accommodations);
        return accommodations.stream()
                .map(accommodation -> toListDto(accommodation, imageById))
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
