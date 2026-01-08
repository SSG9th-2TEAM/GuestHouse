package com.ssg9th2team.geharbang.domain.search.repository;

import com.ssg9th2team.geharbang.config.IntegrationTestConfig;
import com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation;
import com.ssg9th2team.geharbang.domain.accommodation.entity.AccommodationsCategory;
import com.ssg9th2team.geharbang.domain.accommodation.entity.ApprovalStatus;
import com.ssg9th2team.geharbang.domain.main.repository.ListDtoProjection;
import com.ssg9th2team.geharbang.domain.room.entity.Room;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@org.junit.jupiter.api.Disabled("CI 환경 부하로 인한 임시 비활성화")
class SearchRepositoryPerformanceTest extends IntegrationTestConfig {

    @Autowired
    private SearchRepository searchRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Performance test for searchPublicListByBoundsNoDates with large dataset")
    void measureSearchPerformance() {
        // Given: Seed 1000 accommodations
        int dataSize = 1000;
        seedData(dataSize);
        entityManager.flush();
        entityManager.clear();

        // When: Execute search with bounds and price filter (no dates to avoid H2
        // INTERVAL issues)
        // Define bounds that cover most of generated data (Jeju area approx)
        Double minLat = 33.0;
        Double maxLat = 34.0;
        Double minLng = 126.0;
        Double maxLng = 127.0;

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        var result = searchRepository.searchPublicListByBoundsNoDates(
                "GuestHouse",
                minLat,
                maxLat,
                minLng,
                maxLng,
                2, // Guest count
                10000, // Min price
                200000, // Max price
                PageRequest.of(0, 20));

        stopWatch.stop();

        // Then
        System.out.println("==========================================");
        System.out.println("Performance Test Results:");
        System.out.println("DataSet Size: " + dataSize + " accommodations");
        System.out.println("Execution Time: " + stopWatch.getTotalTimeMillis() + " ms");
        System.out.println("Found Elements: " + result.getTotalElements());
        System.out.println("==========================================");

        assertThat(result.getContent()).isNotEmpty();
        // Assert that it returns within a reasonable time (e.g., 2 seconds for this
        // small in-memory set)
        // Note: Real DB performance will differ, but this checks complexity.
        assertThat(stopWatch.getTotalTimeMillis()).isLessThan(2000);
    }

    private void seedData(int count) {
        Random random = new Random();
        for (int i = 0; i < count; i++) {
            Accommodation accommodation = Accommodation.builder()
                    .accountNumberId(1L)
                    .userId(1L)
                    .accommodationsName("GuestHouse " + i)
                    .accommodationsCategory(AccommodationsCategory.GUESTHOUSE)
                    .accommodationsDescription("Description " + i)
                    .shortDescription("Short " + i)
                    .city("Jeju")
                    .district("Seogwipo")
                    .township("Town")
                    .addressDetail("Detail " + i)
                    .latitude(BigDecimal.valueOf(33.2 + (random.nextDouble() * 0.5))) // 33.2 ~ 33.7
                    .longitude(BigDecimal.valueOf(126.3 + (random.nextDouble() * 0.5))) // 126.3 ~ 126.8
                    .transportInfo("Transport")
                    .phone("010-0000-0000")
                    .businessRegistrationNumber("12345" + i)
                    .parkingInfo("Parking")
                    .checkInTime("15:00")
                    .checkOutTime("11:00")
                    .minPrice(10000 + random.nextInt(50000)) // 10000 ~ 60000
                    .build();

            entityManager.persist(accommodation);
            accommodation.updateApprovalStatus(ApprovalStatus.APPROVED, null);

            // Add room
            Room room = Room.builder()
                    .accommodationsId(accommodation.getAccommodationsId())
                    .roomName("Room " + i)
                    .price(accommodation.getMinPrice())
                    .minGuests(1)
                    .maxGuests(4)
                    .roomStatus(1)
                    .build();
            entityManager.persist(room);
        }
    }
}
