package com.ssg9th2team.geharbang.domain.accommodation.repository.mybatis;

import com.ssg9th2team.geharbang.domain.accommodation.dto.AccommodationImageDto;
import com.ssg9th2team.geharbang.domain.accommodation.dto.AccommodationResponseDto;
import com.ssg9th2team.geharbang.domain.accommodation.dto.AccountNumberDto;
import com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AccommodationMapper {

    // ===== 숙소 CRUD =====

    void insertAccommodation(Accommodation accommodation);  // 숙소 등록

    AccommodationResponseDto selectAccommodationById(@Param("accommodationsId") Long accommodationsId); // 숙소 조회

    void updateAccommodation(@Param("accommodationsId") Long accommodationsId,  // 숙소 수정
                             @Param("Accommodation") Accommodation accommodation);

    void deleteAccommodation(@Param("accommodationsId") Long accommodationsId);  // 숙소 삭제


    // ===== 연관 테이블 (이미지, 편의시설, 테마) =====

    void insertAccommodationImages(@Param("accommodationsId") Long accommodationsId,      // 이미지 등록
                                   @Param("images") List<AccommodationImageDto> images);

    void insertAccommodationAmenities(@Param("accommodationsId") Long accommodationsId,   // 편의시설 등록
                                      @Param("amenityIds") List<Long> amenityIds);

    void deleteAccommodationAmenities(@Param("accommodationsId") Long accommodationsId);  // 편의시설 삭제

    void insertAccommodationThemes(@Param("accommodationsId") Long accommodationsId,    // 테마 등록
                                   @Param("themeIds") List<Long> themeIds);

    void deleteAccommodationThemes(@Param("accommodationsId") Long accommodationsId);  // 테마 삭제


    // ===== 정산계좌 =====

    void insertAccountNumber(AccountNumberDto accountNumberDto);
}
