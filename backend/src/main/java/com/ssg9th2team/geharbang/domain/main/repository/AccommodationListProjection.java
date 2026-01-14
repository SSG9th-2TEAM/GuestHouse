package com.ssg9th2team.geharbang.domain.main.repository;

import java.math.BigDecimal;

public interface AccommodationListProjection {
    Long getAccommodationsId();

    String getAccommodationsName();

    String getShortDescription();

    String getAccommodationsDescription();

    String getCity();

    String getDistrict();

    String getTownship();

    BigDecimal getLatitude();

    BigDecimal getLongitude();

    Integer getMinPrice();

    Double getRating();

    Integer getReviewCount();
}
