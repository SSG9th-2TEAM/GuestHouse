package com.ssg9th2team.geharbang.domain.main.dto;


import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ListDto {
    private String accomodationsId;
    private String accomodationsName;
    private String shortDescription;
    private String city;
    private String district;
    private String township;
    private Long minPrice;
    private BigDecimal rating;
    private Long reviewCount;
}
