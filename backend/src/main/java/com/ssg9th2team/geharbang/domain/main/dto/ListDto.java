package com.ssg9th2team.geharbang.domain.main.dto;


import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ListDto {
    private Long accomodationsId;
    private String accomodationsName;
    private String shortDescription;
    private String city;
    private String district;
    private String township;
    private Long minPrice;
    private Integer rating;
    private Integer reviewCount;
    private String imageUrl;

    /*
    // 나중에 Accommodation.java로 이동
    public ListDto toRead() {
        return ListDto.builder()
                .accomodationsId(accommodationsId)
                .accomodationsName(accommodationsName)
                .shortDescription(shortDescription)
                .city(city)
                .district(district)
                .township(township)
//                .minPrice(minPrice)
//                .rating(rating)
                .build();
    }
    */
}
