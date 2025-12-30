package com.ssg9th2team.geharbang.domain.review.host.controller;

import com.ssg9th2team.geharbang.domain.auth.repository.UserRepository;
import com.ssg9th2team.geharbang.domain.review.host.dto.HostReviewResponse;
import com.ssg9th2team.geharbang.domain.review.host.dto.HostReviewSummaryResponse;
import com.ssg9th2team.geharbang.domain.review.host.service.HostReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/host/reviews")
@RequiredArgsConstructor
public class HostReviewController {

    private final HostReviewService hostReviewService;
    private final UserRepository userRepository;

    @GetMapping("/summary")
    public HostReviewSummaryResponse summary(Authentication authentication) {
        Long hostId = resolveHostId(authentication);
        return hostReviewService.getSummary(hostId);
    }

    @GetMapping
    public List<HostReviewResponse> list(
            @RequestParam(required = false) Long accommodationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "latest") String sort,
            Authentication authentication
    ) {
        Long hostId = resolveHostId(authentication);
        return hostReviewService.getReviews(hostId, accommodationId, page, size, sort);
    }

    private Long resolveHostId(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"))
                .getId();
    }
}
