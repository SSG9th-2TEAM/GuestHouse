package com.ssg9th2team.geharbang.domain.main.service;

import com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation;
import com.ssg9th2team.geharbang.domain.accommodation.entity.ApprovalStatus;
import com.ssg9th2team.geharbang.domain.accommodation_theme.entity.AccommodationTheme;
import com.ssg9th2team.geharbang.domain.accommodation_theme.repository.AccommodationThemeRepository;
import com.ssg9th2team.geharbang.domain.auth.entity.User;
import com.ssg9th2team.geharbang.domain.auth.repository.UserRepository;
import com.ssg9th2team.geharbang.domain.main.dto.ListDto;
import com.ssg9th2team.geharbang.domain.main.dto.MainAccommodationListResponse;
import com.ssg9th2team.geharbang.domain.main.dto.PublicListResponse;
import com.ssg9th2team.geharbang.domain.main.repository.AccommodationImageProjection;
import com.ssg9th2team.geharbang.domain.main.repository.ListDtoProjection;
import com.ssg9th2team.geharbang.domain.main.repository.MainRepository;
import com.ssg9th2team.geharbang.domain.room.repository.jpa.RoomJpaRepository;
import com.ssg9th2team.geharbang.domain.theme.entity.Theme;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class MainServiceTest {

    @InjectMocks
    private BaseMainService mainService;

    @Mock
    private MainRepository mainRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AccommodationThemeRepository accommodationThemeRepository;
    @Mock
    private RoomJpaRepository roomJpaRepository;

    private User userWithThemes;
    private User userWithoutThemes;
    private Theme theme1, theme2, theme3;
    private Accommodation acc1, acc2, acc3;
    private List<Accommodation> allAccommodations;

    @BeforeEach
    void setUp() {
        // Create real entity objects but set IDs manually for testing
        theme1 = Theme.builder().themeCategory("여행스타일").themeName("오션뷰").build();
        ReflectionTestUtils.setField(theme1, "id", 1L);
        theme2 = Theme.builder().themeCategory("여행스타일").themeName("파티").build();
        ReflectionTestUtils.setField(theme2, "id", 2L);
        theme3 = Theme.builder().themeCategory("여행스타일").themeName("조용한").build();
        ReflectionTestUtils.setField(theme3, "id", 3L);

        userWithThemes = User.builder().themes(new HashSet<>(Set.of(theme1, theme2))).build();
        userWithoutThemes = User.builder().themes(new HashSet<>()).build();

        acc1 = Accommodation.builder()
                .accommodationsId(1L)
                .accommodationsName("오션뷰 파티하우스")
                .city("부산")
                .district("해운대")
                .township("우동")
                .rating(4.8)
                .reviewCount(150)
                .build();
        acc2 = Accommodation.builder()
                .accommodationsId(2L)
                .accommodationsName("조용한 오션뷰 숙소")
                .city("서울")
                .district("마포")
                .township("연남")
                .rating(4.5)
                .reviewCount(120)
                .build();
        acc3 = Accommodation.builder()
                .accommodationsId(3L)
                .accommodationsName("숲속의 조용한 집")
                .city("부산")
                .district("수영")
                .township("광안")
                .rating(4.9)
                .reviewCount(200)
                .build();
        allAccommodations = List.of(acc1, acc2, acc3);

        AccommodationTheme at1_1 = new AccommodationTheme(acc1, theme1);
        AccommodationTheme at1_2 = new AccommodationTheme(acc1, theme2);
        AccommodationTheme at2_1 = new AccommodationTheme(acc2, theme1);
        AccommodationTheme at2_3 = new AccommodationTheme(acc2, theme3);
        AccommodationTheme at3_3 = new AccommodationTheme(acc3, theme3);

        lenient().when(mainRepository.findByAccommodationStatusAndApprovalStatus(1, ApprovalStatus.APPROVED)).thenReturn(allAccommodations);
        lenient().when(mainRepository.findApprovedByKeyword("오션뷰")).thenReturn(List.of(acc1, acc2));
        lenient().when(mainRepository.findApprovedByKeyword("부산")).thenReturn(List.of(acc1, acc3));
        lenient().when(mainRepository.findApprovedByKeyword("파티")).thenReturn(List.of(acc1));
        lenient().when(userRepository.findById(1L)).thenReturn(Optional.of(userWithThemes));
        lenient().when(userRepository.findById(2L)).thenReturn(Optional.of(userWithoutThemes));
        lenient().when(accommodationThemeRepository.findByAccommodationIds(anyList())).thenReturn(List.of(at1_1, at1_2, at2_1, at2_3, at3_3));
        lenient().when(mainRepository.findRepresentativeImages(anyList())).thenReturn(Collections.emptyList());
        lenient().when(roomJpaRepository.findMaxGuestsByAccommodationIds(anyList())).thenReturn(Collections.emptyList());
    }

    @Test
    @DisplayName("로그인한 사용자(테마 선택O)에게 맞춤 숙소를 추천한다")
    void testRecommendationForUserWithThemes() {
        MainAccommodationListResponse response = mainService.getMainAccommodationList(1L, null, null);
        assertThat(response.getRecommendedAccommodations()).hasSize(2);
        assertThat(response.getRecommendedAccommodations().get(0).getAccomodationsName()).isEqualTo("오션뷰 파티하우스");
        assertThat(response.getRecommendedAccommodations().get(1).getAccomodationsName()).isEqualTo("조용한 오션뷰 숙소");
        assertThat(response.getGeneralAccommodations()).hasSize(1);
        assertThat(response.getGeneralAccommodations().get(0).getAccomodationsName()).isEqualTo("숲속의 조용한 집");
    }

    @Test
    @DisplayName("로그인한 사용자(테마 선택X)에게는 추천 숙소가 없다")
    void testRecommendationForUserWithoutThemes() {
        MainAccommodationListResponse response = mainService.getMainAccommodationList(2L, null, null);
        assertThat(response.getRecommendedAccommodations()).isEmpty();
        assertThat(response.getGeneralAccommodations()).hasSize(3);
    }

    @Test
    @DisplayName("비로그인 사용자에게는 추천 숙소가 없다")
    void testRecommendationForUnauthenticatedUser() {
        MainAccommodationListResponse response = mainService.getMainAccommodationList(null, null, null);
        assertThat(response.getRecommendedAccommodations()).isEmpty();
        assertThat(response.getGeneralAccommodations()).hasSize(3);
    }

    @Test
    @DisplayName("테마 필터 사용 시 추천 로직은 무시되고 필터링된 결과만 반환된다")
    void testFilteringWithThemeIds() {
        when(mainRepository.findByThemeIds(anyList())).thenReturn(List.of(acc2, acc3));

        MainAccommodationListResponse response = mainService.getMainAccommodationList(1L, List.of(3L), null);

        assertThat(response.getRecommendedAccommodations()).isEmpty();
        assertThat(response.getGeneralAccommodations()).hasSize(2);
        List<String> names = response.getGeneralAccommodations().stream().map(ListDto::getAccomodationsName).collect(Collectors.toList());
        assertThat(names).containsExactlyInAnyOrder("조용한 오션뷰 숙소", "숲속의 조용한 집");
    }

    @Test
    @DisplayName("검색어가 있으면 숙소명 기준으로 필터링된다")
    void testFilteringWithKeyword() {
        MainAccommodationListResponse response = mainService.getMainAccommodationList(2L, null, "오션뷰");

        assertThat(response.getRecommendedAccommodations()).isEmpty();
        List<String> names = response.getGeneralAccommodations().stream().map(ListDto::getAccomodationsName).collect(Collectors.toList());
        assertThat(names).containsExactlyInAnyOrder("오션뷰 파티하우스", "조용한 오션뷰 숙소");
    }

    @Test
    @DisplayName("검색어가 있으면 지역명으로도 필터링된다")
    void testFilteringWithKeywordMatchesLocation() {
        MainAccommodationListResponse response = mainService.getMainAccommodationList(2L, null, "부산");

        assertThat(response.getRecommendedAccommodations()).isEmpty();
        List<String> names = response.getGeneralAccommodations().stream().map(ListDto::getAccomodationsName).collect(Collectors.toList());
        assertThat(names).containsExactlyInAnyOrder("오션뷰 파티하우스", "숲속의 조용한 집");
    }

    @Test
    @DisplayName("검색어와 테마 필터를 함께 사용하면 교집합만 반환된다")
    void testFilteringWithKeywordAndThemeIds() {
        when(mainRepository.findByThemeIdsAndKeyword(eq(List.of(3L)), eq("오션뷰"))).thenReturn(List.of(acc2));

        MainAccommodationListResponse response = mainService.getMainAccommodationList(1L, List.of(3L), "오션뷰");

        assertThat(response.getRecommendedAccommodations()).isEmpty();
        assertThat(response.getGeneralAccommodations()).hasSize(1);
        assertThat(response.getGeneralAccommodations().get(0).getAccomodationsName()).isEqualTo("조용한 오션뷰 숙소");
    }

    @Test
    @DisplayName("검색어가 있으면 추천 숙소도 필터링된다")
    void testFilteringWithKeywordForRecommendations() {
        MainAccommodationListResponse response = mainService.getMainAccommodationList(1L, null, "파티");

        assertThat(response.getRecommendedAccommodations()).hasSize(1);
        assertThat(response.getRecommendedAccommodations().get(0).getAccomodationsName()).isEqualTo("오션뷰 파티하우스");
        assertThat(response.getGeneralAccommodations()).isEmpty();
    }

    @Test
    @DisplayName("공개 검색은 페이지 메타와 아이템을 매핑한다")
    void testSearchPublicListMapsPage() {
        ListDtoProjection projection = new ListProjectionStub(
                10L,
                "테스트 숙소",
                "설명",
                "부산",
                "해운대",
                "우동",
                35.1,
                129.1,
                120000L,
                4.7,
                12,
                4,
                "https://example.com/image.jpg"
        );
        PageImpl<ListDtoProjection> page = new PageImpl<>(List.of(projection), PageRequest.of(0, 24), 1);
        when(mainRepository.searchPublicList(eq("부산"), isNull(), isNull(), isNull(), any(PageRequest.class))).thenReturn(page);

        PublicListResponse response = mainService.searchPublicList(
                Collections.emptyList(),
                " 부산 ",
                0,
                24,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).getAccomodationsName()).isEqualTo("테스트 숙소");
        assertThat(response.page().totalElements()).isEqualTo(1);
        assertThat(response.page().hasNext()).isFalse();
    }

    @Test
    @DisplayName("테마 검색은 테마 전용 쿼리를 호출한다")
    void testSearchPublicListUsesThemeQuery() {
        ListDtoProjection projection = new ListProjectionStub(
                11L,
                "테마 숙소",
                "설명",
                "서울",
                "마포",
                "연남",
                37.5,
                126.9,
                90000L,
                4.2,
                5,
                3,
                "https://example.com/theme.jpg"
        );
        PageImpl<ListDtoProjection> page = new PageImpl<>(List.of(projection), PageRequest.of(1, 10), 11);
        when(mainRepository.searchPublicListByTheme(eq(List.of(2L)), eq("오션뷰"), isNull(), isNull(), isNull(), any(PageRequest.class)))
                .thenReturn(page);

        PublicListResponse response = mainService.searchPublicList(
                List.of(2L),
                "오션뷰",
                1,
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
        verify(mainRepository).searchPublicListByTheme(
                eq(List.of(2L)),
                eq("오션뷰"),
                isNull(),
                isNull(),
                isNull(),
                any(PageRequest.class)
        );
    }

    @Test
    @DisplayName("뷰포트 검색은 좌표 조건 쿼리를 호출한다")
    void testSearchPublicListUsesBoundsQuery() {
        ListDtoProjection projection = new ListProjectionStub(
                12L,
                "지도 숙소",
                "설명",
                "부산",
                "해운대",
                "우동",
                35.2,
                129.1,
                130000L,
                4.1,
                8,
                2,
                "https://example.com/map.jpg"
        );
        PageImpl<ListDtoProjection> page = new PageImpl<>(List.of(projection), PageRequest.of(0, 50), 1);
        when(mainRepository.searchPublicListByBounds(
                eq("부산"),
                eq(35.0),
                eq(36.0),
                eq(126.0),
                eq(128.0),
                isNull(),
                isNull(),
                isNull(),
                any(PageRequest.class)
        ))
                .thenReturn(page);

        PublicListResponse response = mainService.searchPublicList(
                Collections.emptyList(),
                "부산",
                0,
                50,
                35.0,
                36.0,
                126.0,
                128.0,
                null,
                null,
                null
        );

        assertThat(response.items()).hasSize(1);
        verify(mainRepository).searchPublicListByBounds(
                eq("부산"),
                eq(35.0),
                eq(36.0),
                eq(126.0),
                eq(128.0),
                isNull(),
                isNull(),
                isNull(),
                any(PageRequest.class)
        );
    }

    @Test
    @DisplayName("날짜/인원 조건이 있으면 검색 쿼리에 전달된다")
    void testSearchPublicListPassesAvailabilityParams() {
        ListDtoProjection projection = new ListProjectionStub(
                20L,
                "예약 가능 숙소",
                "설명",
                "서울",
                "강남",
                "역삼",
                37.5,
                127.0,
                150000L,
                4.6,
                9,
                4,
                "https://example.com/available.jpg"
        );
        PageImpl<ListDtoProjection> page = new PageImpl<>(List.of(projection), PageRequest.of(0, 10), 1);
        LocalDateTime checkin = LocalDateTime.of(2026, 1, 10, 15, 0);
        LocalDateTime checkout = LocalDateTime.of(2026, 1, 12, 11, 0);
        when(mainRepository.searchPublicList(isNull(), eq(checkin), eq(checkout), eq(4), any(PageRequest.class)))
                .thenReturn(page);

        PublicListResponse response = mainService.searchPublicList(
                Collections.emptyList(),
                null,
                0,
                10,
                null,
                null,
                null,
                null,
                checkin,
                checkout,
                4
        );

        assertThat(response.items()).hasSize(1);
        verify(mainRepository).searchPublicList(isNull(), eq(checkin), eq(checkout), eq(4), any(PageRequest.class));
    }

    private static final class ListProjectionStub implements ListDtoProjection {
        private final Long accomodationsId;
        private final String accomodationsName;
        private final String shortDescription;
        private final String city;
        private final String district;
        private final String township;
        private final Double latitude;
        private final Double longitude;
        private final Long minPrice;
        private final Double rating;
        private final Integer reviewCount;
        private final Integer maxGuests;
        private final String imageUrl;

        private ListProjectionStub(Long accomodationsId, String accomodationsName, String shortDescription, String city,
                                   String district, String township, Double latitude, Double longitude, Long minPrice,
                                   Double rating, Integer reviewCount, Integer maxGuests, String imageUrl) {
            this.accomodationsId = accomodationsId;
            this.accomodationsName = accomodationsName;
            this.shortDescription = shortDescription;
            this.city = city;
            this.district = district;
            this.township = township;
            this.latitude = latitude;
            this.longitude = longitude;
            this.minPrice = minPrice;
            this.rating = rating;
            this.reviewCount = reviewCount;
            this.maxGuests = maxGuests;
            this.imageUrl = imageUrl;
        }

        @Override
        public Long getAccomodationsId() {
            return accomodationsId;
        }

        @Override
        public String getAccomodationsName() {
            return accomodationsName;
        }

        @Override
        public String getShortDescription() {
            return shortDescription;
        }

        @Override
        public String getCity() {
            return city;
        }

        @Override
        public String getDistrict() {
            return district;
        }

        @Override
        public String getTownship() {
            return township;
        }

        @Override
        public Double getLatitude() {
            return latitude;
        }

        @Override
        public Double getLongitude() {
            return longitude;
        }

        @Override
        public Long getMinPrice() {
            return minPrice;
        }

        @Override
        public Double getRating() {
            return rating;
        }

        @Override
        public Integer getReviewCount() {
            return reviewCount;
        }

        @Override
        public Integer getMaxGuests() {
            return maxGuests;
        }

        @Override
        public String getImageUrl() {
            return imageUrl;
        }
    }
}
