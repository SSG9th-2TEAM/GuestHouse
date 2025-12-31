package com.ssg9th2team.geharbang.domain.main.controller;

import com.ssg9th2team.geharbang.domain.accommodation.service.AccommodationService;
import com.ssg9th2team.geharbang.domain.auth.repository.UserRepository;
import com.ssg9th2team.geharbang.domain.main.dto.AccommodationDetailDto;
import com.ssg9th2team.geharbang.domain.main.dto.MainAccommodationListResponse;
import com.ssg9th2team.geharbang.domain.main.dto.PublicListResponse;
import com.ssg9th2team.geharbang.domain.main.service.MainService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class MainController {

    private final MainService mainService;
    private final AccommodationService accommodationService;
    private final UserRepository userRepository; // Inject UserRepository

    @GetMapping("/list")
    public MainAccommodationListResponse list(
            Authentication authentication, // Inject Authentication object
            @RequestParam(name = "themeIds", required = false) List<Long> themeIds,
            @RequestParam(name = "keyword", required = false) String keyword) {

        Long userId = null;
        if (authentication != null && authentication.isAuthenticated()) {
            String userEmail = authentication.getName();
            userId = userRepository.findByEmail(userEmail)
                    .map(com.ssg9th2team.geharbang.domain.auth.entity.User::getId)
                    .orElse(null); // If user not found, userId remains null
        }
        return mainService.getMainAccommodationList(userId, themeIds, keyword);
    }

    @GetMapping("/detail/{accommodationsId}")
    public AccommodationDetailDto accommodationDetail(@PathVariable Long accommodationsId) {
        return AccommodationDetailDto.from(accommodationService.getAccommodation(accommodationsId));
    }

    @GetMapping("/search")
    public PublicListResponse search(
            @RequestParam(name = "themeIds", required = false) List<Long> themeIds,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "24") int size
    ) {
        return mainService.searchPublicList(themeIds, keyword, page, size);
    }
}
