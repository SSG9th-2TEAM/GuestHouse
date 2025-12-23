package com.ssg9th2team.geharbang.domain.wishlist.repository.mybatis;

import com.ssg9th2team.geharbang.domain.accommodation.dto.AccommodationResponseDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface WishlistMapper {

  // (마이페이지용 - 상세 정보 포함)
    List<AccommodationResponseDto> selectWishlistByUserId(Long userId);


    // 메인페이지용 : 내가 찜한 숙소 ID목록 (하트 색칠용)
    List<Long> selectWishlistAccommodationIds(Long userId);
}
