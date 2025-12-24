package com.ssg9th2team.geharbang.domain.accommodation.service;

import com.ssg9th2team.geharbang.domain.accommodation.dto.AccountNumberDto;
import com.ssg9th2team.geharbang.domain.accommodation.dto.AccommodationCreateRequestDto;
import com.ssg9th2team.geharbang.domain.accommodation.dto.AccommodationResponseDto;
import com.ssg9th2team.geharbang.domain.accommodation.dto.AccommodationUpdateRequestDto;
import com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation;
import com.ssg9th2team.geharbang.domain.accommodation.entity.AccommodationsCategory;
import com.ssg9th2team.geharbang.domain.accommodation.entity.ApprovalStatus;
import com.ssg9th2team.geharbang.domain.accommodation.repository.mybatis.AccommodationMapper;
import com.ssg9th2team.geharbang.domain.reservation.dto.ReservationResponseDto;
import com.ssg9th2team.geharbang.domain.reservation.entity.Reservation;
import com.ssg9th2team.geharbang.domain.reservation.repository.jpa.ReservationJpaRepository;
import com.ssg9th2team.geharbang.domain.reservation.service.ReservationService;
import com.ssg9th2team.geharbang.domain.room.dto.RoomResponseListDto;
import com.ssg9th2team.geharbang.domain.room.entity.Room;
import com.ssg9th2team.geharbang.domain.room.repository.mybatis.RoomMapper;
import com.ssg9th2team.geharbang.global.storage.ObjectStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccommodationServiceImpl implements AccommodationService {

    private final AccommodationMapper accommodationMapper;
    private final RoomMapper roomMapper;
    private final ObjectStorageService objectStorageService;
    private final ReservationJpaRepository reservationJpaRepository;;



    // 숙소 등록
    @Override
    @Transactional
    public Long createAccommodation(Long userId, AccommodationCreateRequestDto createRequestDto) {
        
        // Base64 이미지 처리 - 네이버 클라우드 Object Storage에 업로드
        try {
            // 1. 사업자 등록증 이미지
            if (createRequestDto.getBusinessRegistrationImage() != null) {
                String savedUrl = objectStorageService.uploadBase64Image(
                        createRequestDto.getBusinessRegistrationImage(), "business");
                createRequestDto.setBusinessRegistrationImage(savedUrl);
            }

            // 2. 숙소 이미지 리스트
            if (createRequestDto.getImages() != null) {
                for (com.ssg9th2team.geharbang.domain.accommodation.dto.AccommodationImageDto img : createRequestDto.getImages()) {
                    if (img.getImageUrl() != null) {
                        String savedUrl = objectStorageService.uploadBase64Image(
                                img.getImageUrl(), "accommodations");
                        img.setImageUrl(savedUrl);
                    }
                }
            }

            // 3. 객실 대표 이미지
            if (createRequestDto.getRooms() != null) {
                for (com.ssg9th2team.geharbang.domain.room.dto.RoomCreateDto room : createRequestDto.getRooms()) {
                    if (room.getMainImageUrl() != null) {
                        String savedUrl = objectStorageService.uploadBase64Image(
                                room.getMainImageUrl(), "rooms");
                        room.setMainImageUrl(savedUrl);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("이미지 업로드 중 오류가 발생했습니다: " + e.getMessage());
        }


        // 정산계좌 먼저 등록
        // 계좌테이블이 부모이므로 계좌를 먼저 등록하고 계좌 아이디를 가지고 숙소를 등록
        AccountNumberDto accountNumberDto = new AccountNumberDto();
        accountNumberDto.setBankName(createRequestDto.getBankName());
        accountNumberDto.setAccountNumber(createRequestDto.getAccountNumber());
        accountNumberDto.setAccountHolder(createRequestDto.getAccountHolder());

        accommodationMapper.insertAccountNumber(accountNumberDto);

        Long accountNumberId = accountNumberDto.getAccountNumberId(); // 정산계좌생성 -> 정산 아이디 생성 -> accountNumberId에 저장

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

        // 숙소 저장 -> 숙소 아이디 생성 -> 숙소 아이디로 연관 테이블 저장
        Long accommodationsId = accommodation.getAccommodationsId();



        // ===================== 4. 연관 테이블 저장 ==========================

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
            System.out.println("DEBUG: Inserting rooms for accommodationId: " + accommodationsId);
            System.out.println("DEBUG: Number of rooms to insert: " + createRequestDto.getRooms().size());
            roomMapper.insertRooms(accommodationsId, createRequestDto.getRooms());

            // 객실 등록 후 숙소의 최소 가격 업데이트
            accommodationMapper.updateMinPrice(accommodationsId);
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
        // 1. 숙소 기본 정보 업데이트
        Accommodation accommodation = Accommodation.builder()
                .accommodationsId(accommodationsId)
                .accommodationsName(updateRequestDto.getAccommodationsName())
                .accommodationsDescription(updateRequestDto.getAccommodationsDescription())
                .shortDescription(updateRequestDto.getShortDescription())
                .transportInfo(updateRequestDto.getTransportInfo())
                .accommodationStatus(updateRequestDto.getAccommodationStatus())
                .parkingInfo(updateRequestDto.getParkingInfo())
                .sns(updateRequestDto.getSns())
                .phone(updateRequestDto.getPhone())
                .checkInTime(updateRequestDto.getCheckInTime())
                .checkOutTime(updateRequestDto.getCheckOutTime())
                .latitude(updateRequestDto.getLatitude())
                .longitude(updateRequestDto.getLongitude())
                .build();

        accommodationMapper.updateAccommodation(accommodationsId, accommodation);

        // 2. 연관 데이터 업데이트 (삭제 후 재등록)
        
        // 편의시설
        if (updateRequestDto.getAmenityIds() != null) {
            accommodationMapper.deleteAccommodationAmenities(accommodationsId);
            if (!updateRequestDto.getAmenityIds().isEmpty()) {
                accommodationMapper.insertAccommodationAmenities(accommodationsId, updateRequestDto.getAmenityIds());
            }
        }

        // 테마
        if (updateRequestDto.getThemeIds() != null) {
            accommodationMapper.deleteAccommodationThemes(accommodationsId);
            if (!updateRequestDto.getThemeIds().isEmpty()) {
                accommodationMapper.insertAccommodationThemes(accommodationsId, updateRequestDto.getThemeIds());
            }
        }

        // 이미지
        if (updateRequestDto.getImages() != null) {
            // 이미지 업로드 로직 추가
            try {
                for (com.ssg9th2team.geharbang.domain.accommodation.dto.AccommodationImageDto img : updateRequestDto.getImages()) {
                    if (img.getImageUrl() != null) {
                        String savedUrl = objectStorageService.uploadBase64Image(
                                img.getImageUrl(), "accommodations");
                        img.setImageUrl(savedUrl);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("숙소 이미지 수정 중 오류가 발생했습니다: " + e.getMessage());
            }

            accommodationMapper.deleteAccommodationImages(accommodationsId);
            if (!updateRequestDto.getImages().isEmpty()) {
                accommodationMapper.insertAccommodationImages(accommodationsId, updateRequestDto.getImages());
            }
        }

        // 먼저 현재 숙소 정보를 조회하여 연결된 계좌 ID(accountNumberId)를 가져옵니다.
        AccommodationResponseDto accountNumber = accommodationMapper.selectAccommodationById(accommodationsId);
        Long accountNumberId = accountNumber.getAccountNumberId();

        AccountNumberDto accountNumberDto = new AccountNumberDto();
        accountNumberDto.setBankName(updateRequestDto.getBankName());
        accountNumberDto.setAccountNumber(updateRequestDto.getAccountNumber());
        accountNumberDto.setAccountHolder(updateRequestDto.getAccountHolder());

        // 그 ID를 사용하여 정산 계좌 테이블을 업데이트합니다.
        accommodationMapper.updateAccountNumber(accountNumberId, accountNumberDto);

        // 3. 객실 동기화 (삭제 및 추가/수정)
        
        // 3-1. 현재 DB에 저장된 객실 목록 조회 (삭제 대상 식별용)
        List<RoomResponseListDto> currentRooms = accommodationMapper.selectRoomsByAccommodationId(accommodationsId);
        
        // 3-2. 요청된 객실 ID 목록 추출
        List<Long> requestedRoomIds = new java.util.ArrayList<>();
        if (updateRequestDto.getRooms() != null) {
            for (AccommodationUpdateRequestDto.RoomData r : updateRequestDto.getRooms()) {
                if (r.getRoomId() != null) {
                    requestedRoomIds.add(r.getRoomId());
                }
            }
        }

        // 3-3. DB에는 있는데 요청에는 없는 ID 삭제
        for (RoomResponseListDto currentRoom : currentRooms) {
            if (!requestedRoomIds.contains(currentRoom.getRoomId())) {
                roomMapper.deleteRoom(accommodationsId, currentRoom.getRoomId());
            }
        }

        // 3-4. 객실 추가/수정
        if (updateRequestDto.getRooms() != null) {
            for (AccommodationUpdateRequestDto.RoomData roomDto : updateRequestDto.getRooms()) {
                // 객실 이미지 업로드 로직 추가
                try {
                    if (roomDto.getMainImageUrl() != null) {
                        String savedUrl = objectStorageService.uploadBase64Image(
                                roomDto.getMainImageUrl(), "rooms");
                        roomDto.setMainImageUrl(savedUrl);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException("객실 이미지 수정 중 오류가 발생했습니다: " + e.getMessage());
                }

                Room room = Room.builder()
                        .accommodationsId(accommodationsId)
                        .roomName(roomDto.getRoomName())
                        .price(roomDto.getPrice())
                        .weekendPrice(roomDto.getWeekendPrice())
                        .minGuests(roomDto.getMinGuests())
                        .maxGuests(roomDto.getMaxGuests())
                        .roomDescription(roomDto.getRoomDescription())
                        .mainImageUrl(roomDto.getMainImageUrl())
                        .bathroomCount(roomDto.getBathroomCount())
                        .roomType(roomDto.getRoomType())
                        .bedCount(roomDto.getBedCount())
                        .roomStatus(roomDto.getRoomStatus() != null ? roomDto.getRoomStatus() : 1)
                        .build();

                if (roomDto.getRoomId() != null) {
                    roomMapper.updateRoom(accommodationsId, roomDto.getRoomId(), room);
                } else {
                    roomMapper.insertRoom(room);
                }
            }
            // 최저가 갱신
            accommodationMapper.updateMinPrice(accommodationsId);
        }
    }



    // 숙소 삭제
    @Override
    @Transactional
    public void deleteAccommodation(Long accommodationsId) {
        // 예약 확인
        List<Reservation> reservations = reservationJpaRepository.findByAccommodationsId(accommodationsId);
        boolean hasActiveReservation = reservations.stream().anyMatch(r -> r.getReservationStatus() == 2);

        if(hasActiveReservation) {
            throw new IllegalStateException("예약된 정보가 있어 삭제할 수 없습니다.");
        }

        accommodationMapper.deleteAccommodation(accommodationsId);

    }
}
