package com.ssg9th2team.geharbang.domain.review.host.service;

import com.ssg9th2team.geharbang.domain.review.host.dto.HostReviewResponse;
import com.ssg9th2team.geharbang.domain.review.host.dto.HostReviewSummaryResponse;
import com.ssg9th2team.geharbang.domain.review.host.repository.mybatis.HostReviewMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HostReviewServiceImpl implements HostReviewService {

    private final HostReviewMapper hostReviewMapper;

    @Override
    public HostReviewSummaryResponse getSummary(Long hostId) {
        HostReviewSummaryResponse summary = hostReviewMapper.selectSummary(hostId);
        if (summary == null) {
            HostReviewSummaryResponse empty = new HostReviewSummaryResponse();
            empty.setAvgRating(0.0);
            empty.setReviewCount(0);
            return empty;
        }
        if (summary.getAvgRating() == null) summary.setAvgRating(0.0);
        if (summary.getReviewCount() == null) summary.setReviewCount(0);
        return summary;
    }

    @Override
    public List<HostReviewResponse> getReviews(Long hostId, Long accommodationId, int page, int size, String sort) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        int offset = safePage * safeSize;
        String normalizedSort = "oldest".equalsIgnoreCase(sort) ? "oldest" : "latest";
        return hostReviewMapper.selectReviews(hostId, accommodationId, offset, safeSize, normalizedSort);
    }
}
