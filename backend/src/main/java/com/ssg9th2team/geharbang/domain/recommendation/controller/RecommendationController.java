package com.ssg9th2team.geharbang.domain.recommendation.controller;

import com.ssg9th2team.geharbang.domain.recommendation.dto.RecommendationResponse;
import com.ssg9th2team.geharbang.domain.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    /**
     * 사용자 맞춤 숙소 추천 API
     * GET /api/recommendations?userId={userId}&limit={limit}
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getRecommendations(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "10") int limit) {

        List<RecommendationResponse> recommendations = recommendationService.getRecommendations(userId, limit);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "count", recommendations.size(),
                "recommendations", recommendations));
    }
}
