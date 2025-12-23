package com.ssg9th2team.geharbang.domain.main.service;

import com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation;
import com.ssg9th2team.geharbang.domain.main.dto.ListDto;
import com.ssg9th2team.geharbang.domain.main.repository.MainRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BaseMainService implements MainService {

    private final MainRepository mainRepository;

    @Override
    public List<ListDto> findByTheme(List<Long> themeIds) {
        List<Accommodation> accommodations = (themeIds == null || themeIds.isEmpty())
                ? mainRepository.findAll()
                : mainRepository.findByThemeIds(themeIds);
        return accommodations.stream()
                .map(this::toListDto)
                .toList();
    }

    private ListDto toListDto(Accommodation accommodation) {
        return ListDto.builder()
                .accomodationsId(accommodation.getAccommodationsId())
                .accomodationsName(accommodation.getAccommodationsName())
                .shortDescription(accommodation.getShortDescription())
                .city(accommodation.getCity())
                .district(accommodation.getDistrict())
                .township(accommodation.getTownship())
                .minPrice(accommodation.getMinPrice() != null ? accommodation.getMinPrice().longValue() : null)
                .rating(null) // TODO: populate when rating available
                .reviewCount(null) // TODO: populate when review count available
                .imageUrl(null) // TODO: populate when image URL available
                .build();
    }
}
