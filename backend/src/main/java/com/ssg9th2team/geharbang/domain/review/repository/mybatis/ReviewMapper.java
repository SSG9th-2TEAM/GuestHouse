package com.ssg9th2team.geharbang.domain.review.repository.mybatis;

import com.ssg9th2team.geharbang.domain.review.dto.ReviewImageDto;
import com.ssg9th2team.geharbang.domain.review.dto.ReviewResponseDto;
import com.ssg9th2team.geharbang.domain.review.dto.ReviewTagDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReviewMapper {

    List<ReviewResponseDto> selectReviewsByAccommodationId(@Param("accommodationsId") Long accommodationsId);

    List<ReviewImageDto> selectReviewImagesByReviewId(@Param("reviewId") Long reviewId);

    List<ReviewTagDto> selectReviewTagsByReviewId(@Param("reviewId") Long reviewId);
}
