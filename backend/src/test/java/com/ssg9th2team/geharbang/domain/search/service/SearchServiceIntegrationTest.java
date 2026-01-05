package com.ssg9th2team.geharbang.domain.search.service;

import com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation;
import com.ssg9th2team.geharbang.domain.accommodation.entity.AccommodationsCategory;
import com.ssg9th2team.geharbang.domain.accommodation.entity.ApprovalStatus;
import com.ssg9th2team.geharbang.domain.main.dto.PublicListResponse;
import com.ssg9th2team.geharbang.domain.reservation.entity.Reservation;
import com.ssg9th2team.geharbang.domain.room.entity.Room;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(SearchServiceImpl.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:searchtest;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.type.preferred_boolean_jdbc_type=TINYINT",
        "spring.flyway.enabled=false"
})
@Transactional
class SearchServiceIntegrationTest {

    @Autowired
    private SearchService searchService;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUpSchema() {
        entityManager.createNativeQuery(
                "CREATE TABLE IF NOT EXISTS accommodation_image (" +
                        "image_id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                        "accommodations_id BIGINT NOT NULL, " +
                        "image_url VARCHAR(255), " +
                        "image_type VARCHAR(20), " +
                        "sort_order INT" +
                        ")"
        ).executeUpdate();
    }

    @Test
    @DisplayName("Search returns accommodations for Seogwipo keyword")
    void searchBySeogwipoKeywordReturnsAccommodation() {
        Accommodation accommodation = persistAccommodation("\uC11C\uADC0\uD3EC \uD14C\uC2A4\uD2B8 \uC219\uC18C", "\uC11C\uADC0\uD3EC\uC2DC");
        persistRoom(accommodation.getAccommodationsId(), 4);
        entityManager.clear();

        PublicListResponse response = searchService.searchPublicList(
                Collections.emptyList(),
                "\uC11C\uADC0\uD3EC",
                0,
                10,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).getAccomodationsName())
                .isEqualTo("\uC11C\uADC0\uD3EC \uD14C\uC2A4\uD2B8 \uC219\uC18C");
    }

    @Test
    @DisplayName("Guest count filter excludes accommodations with insufficient capacity")
    void searchFiltersByGuestCount() {
        Accommodation small = persistAccommodation("\uC11C\uADC0\uD3EC \uC18C\uD615 \uC219\uC18C", "\uC11C\uADC0\uD3EC\uC2DC");
        persistRoom(small.getAccommodationsId(), 2);

        Accommodation large = persistAccommodation("\uC11C\uADC0\uD3EC \uB300\uD615 \uC219\uC18C", "\uC11C\uADC0\uD3EC\uC2DC");
        persistRoom(large.getAccommodationsId(), 6);
        entityManager.clear();

        PublicListResponse response = searchService.searchPublicList(
                Collections.emptyList(),
                "\uC11C\uADC0\uD3EC",
                0,
                10,
                null,
                null,
                null,
                null,
                null,
                null,
                4
        );

        List<String> names = response.items().stream()
                .map(item -> item.getAccomodationsName())
                .toList();
        assertThat(names).containsExactly("\uC11C\uADC0\uD3EC \uB300\uD615 \uC219\uC18C");
    }

    @Test
    @DisplayName("Date range filter excludes accommodations with fully booked rooms")
    void searchFiltersOutUnavailableRoomsForDateRange() {
        LocalDateTime checkin = LocalDateTime.of(2026, 2, 10, 15, 0);
        LocalDateTime checkout = LocalDateTime.of(2026, 2, 12, 11, 0);

        Accommodation blocked = persistAccommodation("\uC11C\uADC0\uD3EC \uC608\uC57D\uBD88\uAC00", "\uC11C\uADC0\uD3EC\uC2DC");
        Room blockedRoom = persistRoom(blocked.getAccommodationsId(), 4);
        persistReservation(blocked.getAccommodationsId(), blockedRoom.getRoomId(), checkin, checkout);

        Accommodation available = persistAccommodation("\uC11C\uADC0\uD3EC \uC608\uC57D\uAC00\uB2A5", "\uC11C\uADC0\uD3EC\uC2DC");
        persistRoom(available.getAccommodationsId(), 4);
        entityManager.clear();

        PublicListResponse response = searchService.searchPublicList(
                Collections.emptyList(),
                "\uC11C\uADC0\uD3EC",
                0,
                10,
                null,
                null,
                null,
                null,
                checkin,
                checkout,
                2
        );

        List<String> names = response.items().stream()
                .map(item -> item.getAccomodationsName())
                .toList();
        assertThat(names).containsExactly("\uC11C\uADC0\uD3EC \uC608\uC57D\uAC00\uB2A5");
    }

    private Accommodation persistAccommodation(String name, String district) {
        Accommodation accommodation = Accommodation.builder()
                .accountNumberId(1L)
                .userId(1L)
                .accommodationsName(name)
                .accommodationsCategory(AccommodationsCategory.GUESTHOUSE)
                .accommodationsDescription("test description")
                .shortDescription("test short")
                .city("\uC81C\uC8FC")
                .district(district)
                .township("test-township")
                .addressDetail("test-address")
                .latitude(BigDecimal.valueOf(33.25))
                .longitude(BigDecimal.valueOf(126.55))
                .transportInfo("test transport")
                .phone("010-0000-0000")
                .businessRegistrationNumber("1234567890")
                .parkingInfo("test parking")
                .checkInTime("15:00")
                .checkOutTime("11:00")
                .minPrice(10000)
                .build();
        entityManager.persist(accommodation);
        entityManager.flush();

        accommodation.updateApprovalStatus(ApprovalStatus.APPROVED, null);
        entityManager.flush();
        return accommodation;
    }

    private Room persistRoom(Long accommodationsId, int maxGuests) {
        Room room = Room.builder()
                .accommodationsId(accommodationsId)
                .roomName("test room")
                .price(10000)
                .minGuests(1)
                .maxGuests(maxGuests)
                .roomStatus(1)
                .build();
        entityManager.persist(room);
        entityManager.flush();
        return room;
    }

    private void persistReservation(Long accommodationsId, Long roomId, LocalDateTime checkin, LocalDateTime checkout) {
        int nights = (int) ChronoUnit.DAYS.between(checkin.toLocalDate(), checkout.toLocalDate());
        Reservation reservation = Reservation.builder()
                .accommodationsId(accommodationsId)
                .roomId(roomId)
                .userId(1L)
                .checkin(checkin)
                .checkout(checkout)
                .stayNights(nights)
                .guestCount(2)
                .reservationStatus(2)
                .totalAmountBeforeDc(10000)
                .couponDiscountAmount(0)
                .finalPaymentAmount(10000)
                .paymentStatus(1)
                .reserverName("\uD14C\uC2A4\uD130")
                .reserverPhone("010-0000-0000")
                .build();
        entityManager.persist(reservation);
        entityManager.flush();
    }
}
