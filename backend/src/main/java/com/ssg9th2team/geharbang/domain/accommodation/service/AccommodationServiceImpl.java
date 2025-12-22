package com.ssg9th2team.geharbang.domain.accommodation.service;

import com.ssg9th2team.geharbang.domain.accommodation.dto.AccountNumberDto;
import com.ssg9th2team.geharbang.domain.accommodation.dto.AccommodationCreateRequestDto;
import com.ssg9th2team.geharbang.domain.accommodation.dto.AccommodationResponseDto;
import com.ssg9th2team.geharbang.domain.accommodation.dto.AccommodationUpdateRequestDto;
import com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation;
import com.ssg9th2team.geharbang.domain.accommodation.entity.AccommodationsCategory;
import com.ssg9th2team.geharbang.domain.accommodation.entity.ApprovalStatus;
import com.ssg9th2team.geharbang.domain.accommodation.repository.mybatis.AccommodationMapper;
import com.ssg9th2team.geharbang.domain.room.repository.mybatis.RoomMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AccommodationServiceImpl implements AccommodationService {

    //  DTO → Entity 변환
    private final AccommodationMapper accommodationMapper;
    private final RoomMapper roomMapper;


    // 숙소 등록
    @Override
    @Transactional
    public Long createAccommodation(Long userId, AccommodationCreateRequestDto createRequestDto) {

        // 정산계좌 먼저 등록
        // 계좌테이블이 부모이므로 계좌를 먼저 등록하고 계좌 아이디를 가지고 숙소를 등록
        AccountNumberDto accountNumberDto = new AccountNumberDto();
        accountNumberDto.setBankName(createRequestDto.getBankName());
        accountNumberDto.setAccountNumber(createRequestDto.getAccountNumber());
        accountNumberDto.setAccountHolder(createRequestDto.getAccountHolder());

        accommodationMapper.insertAccountNumber(accountNumberDto);
        Long accountNumberId = accountNumberDto.getAccountNumberId(); // 계좌번호 등록 -> 정산 아이디 생성 -> accountNumberId에 저장

        // 2. 숙소 엔티티 생성
        Accommodation accommodation = Accommodation.builder()
                .accountNumberId(accountNumberId) // PK들은 그냥 가져오기
                .userId(userId)
                .accommodationsName(createRequestDto.getAccommodationsName())  // 컬럼들은 Dto에서 가져오기
                .accommodationsCategory(AccommodationsCategory.valueOf(createRequestDto.getAccommodationsCategory()))  // dto로 받은 값(String)을 enum타입으로 변경
                .accommodationsDescription(createRequestDto.getAccommodationsDescription())
                .shortDescription(createRequestDto.getShortDescription())
                .city(createRequestDto.getCity())
                .district(createRequestDto.getDistrict())
                .township(createRequestDto.getTownship())
                .addressDetail(createRequestDto.getAddressDetail())
                .latitude(createRequestDto.getLatitude())
                .longitude(createRequestDto.getLongitude())
                .transportInfo(createRequestDto.getTransportInfo())
                .accommodationStatus(1)
                .approvalStatus(ApprovalStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .phone(createRequestDto.getPhone())
                .businessRegistrationNumber(createRequestDto.getBusinessRegistrationNumber())
                .businessRegistrationImage(createRequestDto.getBusinessRegistrationImage())
                .parkingInfo(createRequestDto.getParkingInfo())
                .sns(createRequestDto.getSns())
                .checkInTime(createRequestDto.getCheckInTime())
                .checkOutTime(createRequestDto.getCheckOutTime())
                .build();


        // 3. 숙소 저장
        accommodationMapper.insertAccommodation(accommodation);
        Long accommodationsId = accommodation.getAccommodationsId();

        // 4. 연관 테이블 저장
        // 이미지가 없거나 비어있지 않다면 실행
        if (createRequestDto.getImages() != null && !createRequestDto.getImages().isEmpty()) {
            accommodationMapper.insertAccommodationImages(accommodationsId, createRequestDto.getImages());
        }

        // 편의시설
        if (createRequestDto.getAmenityIds() != null && !createRequestDto.getAmenityIds().isEmpty()) {
            accommodationMapper.insertAccommodationAmenities(accommodationsId, createRequestDto.getAmenityIds());
        }

        // 테마
        if (createRequestDto.getThemeIds() != null && !createRequestDto.getThemeIds().isEmpty()) {
            accommodationMapper.insertAccommodationThemes(accommodationsId, createRequestDto.getThemeIds());
        }

        // 객실
        if (createRequestDto.getRooms() != null && !createRequestDto.getRooms().isEmpty()) {
            roomMapper.insertRooms(accommodationsId, createRequestDto.getRooms());
        }

        return accommodationsId;
    }


    // 숙소 상세조회
    @Override
    public AccommodationResponseDto getAccommodation(Long accommodationsId) {
        return accommodationMapper.selectAccommodationById(accommodationsId);
    }


    // 숙소 수정
    @Override
    @Transactional
    public void updateAccommodation(Long accommodationsId, AccommodationUpdateRequestDto updateRequestDto) {
        Accommodation accommodation = Accommodation.builder()
                .accommodationsId(accommodationsId)
                .accommodationsDescription(updateRequestDto.getAccommodationsDescription())
                .shortDescription(updateRequestDto.getShortDescription())
                .transportInfo(updateRequestDto.getTransportInfo())
                .accommodationStatus(updateRequestDto.getAccommodationStatus())
                .parkingInfo(updateRequestDto.getParkingInfo())
                .sns(updateRequestDto.getSns())
                .phone(updateRequestDto.getPhone())
                .checkInTime(updateRequestDto.getCheckInTime())
                .checkOutTime(updateRequestDto.getCheckOutTime())
                .build();

        accommodationMapper.updateAccommodation(accommodationsId, accommodation);

        // 편의시설 업데이트 (기존 삭제 후 재등록)
        if (updateRequestDto.getAmenityIds() != null) {
            accommodationMapper.deleteAccommodationAmenities(accommodationsId);
            if (!updateRequestDto.getAmenityIds().isEmpty()) {
                accommodationMapper.insertAccommodationAmenities(accommodationsId, updateRequestDto.getAmenityIds());
            }
        }

        // 테마 업데이트 (기존 삭제 후 재등록)
        if (updateRequestDto.getThemeIds() != null) {
            accommodationMapper.deleteAccommodationThemes(accommodationsId);
            if (!updateRequestDto.getThemeIds().isEmpty()) {
                accommodationMapper.insertAccommodationThemes(accommodationsId, updateRequestDto.getThemeIds());
            }
        }
    }


    // 숙소 삭제
    @Override
    @Transactional
    public void deleteAccommodation(Long accommodationsId) {
        accommodationMapper.deleteAccommodation(accommodationsId);

        // 객실 예약이 있다면 삭제 불가 코드
    }
}
