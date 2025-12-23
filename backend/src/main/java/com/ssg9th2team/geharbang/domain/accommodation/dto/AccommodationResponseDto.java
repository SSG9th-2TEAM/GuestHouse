package com.ssg9th2team.geharbang.domain.accommodation.dto;

import com.ssg9th2team.geharbang.domain.room.dto.RoomResponseListDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccommodationResponseDto {

    // 숙소 조회용 DTO
    private Long accommodationsId;
    private Long accountNumberId;
    private Long userId;
    private String accommodationsName;
    private String accommodationsCategory;
    private String accommodationsDescription;
    private String shortDescription;
    private String city;
    private String district;
    private String township;
    private String addressDetail;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String transportInfo;
    private Integer accommodationStatus;
    private String approvalStatus;
    private LocalDateTime createdAt;
    private String phone;
    private String businessRegistrationNumber;
    private String businessRegistrationImage;
    private String parkingInfo;
    private String sns;
    private String checkInTime;
    private String checkOutTime;
    private String rejectionReason;

    private Double rating;
    
    // 대표 이미지 URL (리스트 조회용)
    private String mainImageUrl;

    // 정산계좌 정보 (조인)
    private String bankName;
    private String accountNumber;
    private String accountHolder;

    // 편의시설 및 테마 (1:N 조인)
    private List<String> amenities;
    private List<String> themes;

    // 숙소 상세보기에서 객실 리스트
    private List<RoomResponseListDto> rooms;

    // 숙소 이미지 (1:N 조인)
    private List<AccommodationImageDto> images;

    // 편의시설/테마 ID 목록 (수정 페이지용)
    private List<Long> amenityIds;
    private List<Long> themeIds;

}