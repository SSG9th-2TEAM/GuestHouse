package com.ssg9th2team.geharbang.domain.reservation.service;

import com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation;
import com.ssg9th2team.geharbang.domain.accommodation.repository.jpa.AccommodationJpaRepository;
import com.ssg9th2team.geharbang.domain.reservation.dto.ReservationRequestDto;
import com.ssg9th2team.geharbang.domain.room.entity.Room;
import com.ssg9th2team.geharbang.domain.room.repository.jpa.RoomJpaRepository;
import com.ssg9th2team.geharbang.global.storage.ObjectStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ConcurrencyTest {

    @Autowired
    private ReservationService reservationService;

    @MockBean
    private ObjectStorageService objectStorageService;

    @Autowired
    private com.ssg9th2team.geharbang.domain.reservation.repository.jpa.ReservationJpaRepository reservationRepository;

    @Autowired
    private RoomJpaRepository roomRepository;

    @Autowired
    private AccommodationJpaRepository accommodationRepository;

    private Long roomId;
    private Long accommodationId;

    @BeforeEach
    void setUp() {
        // 1. Accommodation 생성
        Accommodation accommodation = Accommodation.builder()
                .accommodationsName("테스트 숙소")
                .city("서울").district("강남구").township("역삼동").addressDetail("123-45")
                .latitude(java.math.BigDecimal.valueOf(37.5))
                .longitude(java.math.BigDecimal.valueOf(127.0))
                .accommodationsCategory(
                        com.ssg9th2team.geharbang.domain.accommodation.entity.AccommodationsCategory.HOTEL)
                .accommodationsDescription("테스트 설명")
                .shortDescription("짧은 설명")
                .transportInfo("교통")
                .phone("010-0000-0000")
                .businessRegistrationNumber("1234567890")
                .parkingInfo("주차가능")
                .checkInTime("15:00")
                .checkOutTime("11:00")
                .accountNumberId(1L) // 임의
                .userId(1L) // 임의
                .build();
        accommodation = accommodationRepository.save(accommodation);
        accommodationId = accommodation.getAccommodationsId();

        // 2. Room 생성 (maxGuests = 10으로 설정)
        // 100명이 동시에 요청하지만 10명만 성공해야 함
        Room room = Room.builder()
                .accommodationsId(accommodationId)
                .roomName("테스트 객실")
                .maxGuests(10) // 중요: 재고 10개
                .minGuests(1)
                .price(10000)
                .roomStatus(1)
                .build();
        room = roomRepository.save(room);
        roomId = room.getRoomId();
    }

    @Test
    @DisplayName("객실 정원이 10명일 때 100명이 동시에 1명씩 예약하면 10명만 성공해야 한다.")
    void concurrencyReservationTest() throws InterruptedException {
        int numberOfThreads = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        ReservationRequestDto requestDto = new ReservationRequestDto(
                accommodationId,
                roomId,
                1L, // userId
                Instant.now().plus(1, ChronoUnit.DAYS),
                Instant.now().plus(2, ChronoUnit.DAYS),
                1, // guestCount (1명씩 예약)
                10000,
                0, // couponDiscount
                "테스터",
                "010-1234-5678");

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                try {
                    reservationService.createReservation(requestDto);
                    successCount.getAndIncrement();
                } catch (Exception e) {
                    failCount.getAndIncrement();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        System.out.println("성공한 예약 수: " + successCount.get());
        System.out.println("실패한 예약 수: " + failCount.get());

        // 검증: 성공 횟수는 정확히 초기 maxGuests(10)와 같아야 함
        assertThat(successCount.get()).isEqualTo(10);

        // 검증: DB에 남은 maxGuests는 0이어야 함
        Room updatedRoom = roomRepository.findById(roomId).orElseThrow();
        assertThat(updatedRoom.getMaxGuests()).isEqualTo(0);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        if (roomId != null) {
            // 해당 숙소의 예약 모두 삭제
            // (실제로는 예약 ID를 추적하거나, accommodationId로 조회해서 삭제)
            reservationRepository.deleteAll(reservationRepository.findByAccommodationsId(accommodationId));

            // Room 삭제
            roomRepository.deleteById(roomId);
        }

        if (accommodationId != null) {
            // Accommodation 삭제
            accommodationRepository.deleteById(accommodationId);
        }
    }
}
