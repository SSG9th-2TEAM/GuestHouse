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
    private Double rating;
    private Integer reviewCount;
    private String imageUrl;

}
