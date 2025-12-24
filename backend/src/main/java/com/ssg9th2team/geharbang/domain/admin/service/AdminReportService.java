package com.ssg9th2team.geharbang.domain.admin.service;

import com.ssg9th2team.geharbang.domain.admin.dto.AdminPageResponse;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminReportDetail;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminReportSummary;
import com.ssg9th2team.geharbang.domain.report.entity.ReviewReport;
import com.ssg9th2team.geharbang.domain.report.repository.jpa.ReviewReportJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminReportService {

    private final ReviewReportJpaRepository reportRepository;

    public AdminPageResponse<AdminReportSummary> getReports(String status, String type, String query, int page, int size, String sort) {
        Specification<ReviewReport> spec = buildSpecification(status, type, query);
        Sort sorting = "oldest".equalsIgnoreCase(sort)
                ? Sort.by(Sort.Direction.ASC, "createdAt")
                : Sort.by(Sort.Direction.DESC, "createdAt");
        Page<ReviewReport> result = reportRepository.findAll(spec, PageRequest.of(page, size, sorting));
        List<AdminReportSummary> items = result.stream()
                .map(this::toSummary)
                .toList();
        return AdminPageResponse.of(items, page, size, result.getTotalElements(), result.getTotalPages());
    }

    public AdminReportDetail getReportDetail(Long reportId) {
        ReviewReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));
        return toDetail(report);
    }

    public AdminReportDetail resolveReport(Long reportId, String action) {
        ReviewReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));
        if (StringUtils.hasText(action)) {
            report.updateState(action.trim().toUpperCase());
        }
        return toDetail(reportRepository.save(report));
    }

    private Specification<ReviewReport> buildSpecification(String status, String type, String query) {
        return (root, q, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(status) && !"all".equalsIgnoreCase(status)) {
                predicates.add(cb.equal(root.get("state"), status.toUpperCase()));
            }
            if (StringUtils.hasText(type) && !"all".equalsIgnoreCase(type)) {
                if (!"review".equalsIgnoreCase(type)) {
                    predicates.add(cb.disjunction());
                }
            }
            if (StringUtils.hasText(query)) {
                String likeQuery = "%" + query.toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("reason")), likeQuery));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private AdminReportSummary toSummary(ReviewReport report) {
        return new AdminReportSummary(
                report.getReportId(),
                "REVIEW",
                report.getState(),
                report.getReason(),
                report.getCreatedAt()
        );
    }

    private AdminReportDetail toDetail(ReviewReport report) {
        return new AdminReportDetail(
                report.getReportId(),
                report.getReviewId(),
                report.getUserId(),
                "REVIEW",
                report.getState(),
                report.getReason(),
                report.getCreatedAt(),
                report.getUpdatedAt()
        );
    }
}
