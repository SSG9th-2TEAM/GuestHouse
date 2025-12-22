package com.ssg9th2team.geharbang.domain.accommodation.service;

import com.ssg9th2team.geharbang.domain.accommodation.dto.AccommodationCreateRequestDto;
import com.ssg9th2team.geharbang.domain.accommodation.dto.AccommodationImageDto;
import com.ssg9th2team.geharbang.domain.room.dto.RoomCreateDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional // 테스트 후 DB 롤백 (데이터 오염 방지)
public class AccommodationServiceTest {

    @Autowired
    private AccommodationServiceImpl accommodationService;

    @Test
    @DisplayName("숙소 등록 통합 테스트 - 긴 이미지 문자열과 다양한 Enum 값 검증")
    void createAccommodationTest() {
        // given
        Long userId = 1L; // 더미 유저 ID (존재해야 함)

        // 아주 긴 Base64 문자열 생성 (600자 이상)
        String longBase64Image = "data:image/jpeg;base64," + "A".repeat(1000);

        AccommodationCreateRequestDto requestDto = new AccommodationCreateRequestDto();
        requestDto.setAccommodationsName("TDD 숙소");
        requestDto.setAccommodationsCategory("HOTEL"); // ENUM 테스트 (기존에 없던 값)
        requestDto.setAccommodationsDescription("테스트 설명");
        requestDto.setShortDescription("짧은 설명");
        requestDto.setCity("제주");
        requestDto.setDistrict("제주시");
        requestDto.setTownship("애월읍");
        requestDto.setAddressDetail("상세 주소 123");
        requestDto.setLatitude(new BigDecimal("33.1234567"));
        requestDto.setLongitude(new BigDecimal("126.1234567"));
        requestDto.setTransportInfo("셔틀 버스");
        requestDto.setPhone("010-1234-5678");
        requestDto.setBusinessRegistrationNumber("123-45-67890");
        requestDto.setBusinessRegistrationImage(longBase64Image); // 핵심: 긴 문자열 테스트
        requestDto.setParkingInfo("주차 가능");
        requestDto.setSns("instagram.com/test");
        requestDto.setCheckInTime("15:00");
        requestDto.setCheckOutTime("11:00");

        // 계좌 정보
        requestDto.setBankName("테스트은행");
        requestDto.setAccountNumber("123-456-789");
        requestDto.setAccountHolder("테스트예금주");

        // 이미지 (긴 문자열)
        AccommodationImageDto imageDto = new AccommodationImageDto();
        imageDto.setImageUrl(longBase64Image);
        imageDto.setImageType("banner");
        imageDto.setSortOrder(1);
        requestDto.setImages(Collections.singletonList(imageDto));

        // 객실 (긴 문자열)
        RoomCreateDto roomDto = new RoomCreateDto();
        roomDto.setRoomName("디럭스 룸");
        roomDto.setPrice(100000);
        roomDto.setWeekendPrice(120000);
        roomDto.setMinGuests(2);
        roomDto.setMaxGuests(4);
        roomDto.setRoomDescription("객실 설명");
        roomDto.setMainImageUrl(longBase64Image);
        roomDto.setBathroomCount(1);
        roomDto.setRoomType("ONDOL");
        roomDto.setBedCount(2);
        requestDto.setRooms(Collections.singletonList(roomDto));

        // 편의시설/테마 (ID는 DB에 존재하는 값이어야 함)
        // 없을 경우 null 포인터나 FK 에러가 날 수 있으니 빈 리스트로 먼저 테스트
        requestDto.setAmenityIds(new ArrayList<>());
        requestDto.setThemeIds(new ArrayList<>());

        // when
        Long savedId = accommodationService.createAccommodation(userId, requestDto);

        // then
        assertThat(savedId).isNotNull();
        System.out.println("숙소 등록 성공 ID: " + savedId);
    }
}
