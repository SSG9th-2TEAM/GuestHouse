package com.ssg9th2team.geharbang.domain.search.repository;

import com.ssg9th2team.geharbang.domain.main.dto.ListDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface SearchRepositoryCustom {
    Page<ListDto> searchDynamic(
            List<Long> themeIds,
            String keyword,
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
            Pageable pageable);
}
