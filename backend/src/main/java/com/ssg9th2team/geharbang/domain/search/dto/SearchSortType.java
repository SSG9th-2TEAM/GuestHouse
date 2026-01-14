package com.ssg9th2team.geharbang.domain.search.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;

/**
 * 검색 결과 정렬 타입 Enum
 * 정렬 로직을 중앙화하여 유지보수성 향상
 */
@Getter
@RequiredArgsConstructor
public enum SearchSortType {

    REVIEWS("reviews", "reviewCount", Sort.Direction.DESC),
    RATING("rating", "rating", Sort.Direction.DESC),
    PRICE_HIGH("priceHigh", "minPrice", Sort.Direction.DESC),
    PRICE_LOW("priceLow", "minPrice", Sort.Direction.ASC),
    RECOMMENDED("recommended", "(bayesianScore)", Sort.Direction.DESC),
    DEFAULT("default", "accommodationsId", Sort.Direction.DESC);

    private final String value;
    private final String field;
    private final Sort.Direction direction;

    /**
     * 문자열 값으로 SearchSortType을 찾습니다.
     * 
     * @param value 정렬 타입 문자열 (예: "reviews", "rating")
     * @return 해당하는 SearchSortType, 없으면 DEFAULT
     */
    public static SearchSortType fromValue(String value) {
        if (value == null || value.isEmpty()) {
            return DEFAULT;
        }
        for (SearchSortType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return DEFAULT;
    }

    /**
     * JpaSort 객체를 생성합니다.
     * 
     * @return 정렬에 사용할 Sort 객체
     */
    public Sort toSort() {
        return JpaSort.unsafe(direction, field);
    }
}
