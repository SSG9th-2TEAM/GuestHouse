package com.ssg9th2team.geharbang.domain.accommodation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class HostAccommodationSummaryResponse {
    private Long accommodationsId;
    private String accommodationsName;
    private String status;
    private String city;
    private String district;
    private String township;
    private String addressDetail;
    private Integer maxGuests;
    private Integer roomCount;
    private Integer pricePerNight;
    private String imageUrl;
}
