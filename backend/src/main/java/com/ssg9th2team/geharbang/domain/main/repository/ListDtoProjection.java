package com.ssg9th2team.geharbang.domain.main.repository;

public interface ListDtoProjection {
    Long getAccomodationsId();

    String getAccomodationsName();

    String getShortDescription();

    String getCity();

    String getDistrict();

    String getTownship();

    Double getLatitude();

    Double getLongitude();

    Long getMinPrice();

    Double getRating();

    Integer getReviewCount();

    String getImageUrl();
}
