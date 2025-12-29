package com.ssg9th2team.geharbang.domain.accommodation.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AccommodationImageDto {
    private String imageUrl;
    private String imageType;      // banner, detail
    private Integer sortOrder;
}
