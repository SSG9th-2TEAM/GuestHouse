package com.ssg9th2team.geharbang.domain.search.repository;

import com.ssg9th2team.geharbang.domain.main.dto.ListDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class SearchDebugIntegrationTest {

    @Autowired
    private SearchRepository searchRepository;

    @Test
    public void debugSearchAewol() {
        System.out.println(">>> START SEARCH DEBUG");
        String keyword = "애월";
        Page<ListDto> result = searchRepository.searchDynamic(
                null, // themeIds
                keyword,
                null, null, null, null, // bounds
                null, null, // dates
                1, // guestCount (default)
                null, null, // price
                false, // includeUnavailable
                PageRequest.of(0, 100));

        System.out.println("Total Elements Found: " + result.getTotalElements());
        List<ListDto> items = result.getContent();
        for (ListDto item : items) {
            System.out.println("Found: " + item.getAccommodationsName() + " | Loc: " + item.getCity() + " "
                    + item.getDistrict() + " " + item.getTownship());
        }
        System.out.println(">>> END SEARCH DEBUG");
    }
}
