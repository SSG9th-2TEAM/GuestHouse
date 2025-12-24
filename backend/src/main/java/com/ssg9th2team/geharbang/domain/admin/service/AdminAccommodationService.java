package com.ssg9th2team.geharbang.domain.admin.service;

import com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation;
import com.ssg9th2team.geharbang.domain.accommodation.entity.ApprovalStatus;
import com.ssg9th2team.geharbang.domain.accommodation.repository.jpa.AccommodationJpaRepository;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminAccommodationDetail;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminAccommodationSummary;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminPageResponse;
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
public class AdminAccommodationService {

    private final AccommodationJpaRepository accommodationRepository;

    public AdminPageResponse<AdminAccommodationSummary> getAccommodations(
            String status,
            String keyword,
            int page,
            int size,
            String sort
    ) {
        Specification<Accommodation> spec = buildSpecification(status, keyword);
        Sort sorting = "oldest".equalsIgnoreCase(sort)
                ? Sort.by(Sort.Direction.ASC, "createdAt")
                : Sort.by(Sort.Direction.DESC, "createdAt");

        Page<Accommodation> result = accommodationRepository.findAll(spec, PageRequest.of(page, size, sorting));
        List<AdminAccommodationSummary> items = result.stream()
                .map(this::toSummary)
                .toList();

        return AdminPageResponse.of(items, page, size, result.getTotalElements(), result.getTotalPages());
    }

    public AdminAccommodationDetail getAccommodationDetail(Long accommodationId) {
        Accommodation accommodation = accommodationRepository.findById(accommodationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Accommodation not found"));
        return toDetail(accommodation);
    }

    public AdminAccommodationDetail approveAccommodation(Long accommodationId) {
        Accommodation accommodation = accommodationRepository.findById(accommodationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Accommodation not found"));
        accommodation.approve();
        return toDetail(accommodationRepository.save(accommodation));
    }

    public AdminAccommodationDetail rejectAccommodation(Long accommodationId, String reason) {
        Accommodation accommodation = accommodationRepository.findById(accommodationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Accommodation not found"));
        accommodation.reject(reason);
        return toDetail(accommodationRepository.save(accommodation));
    }

    private Specification<Accommodation> buildSpecification(String status, String query) {
        return (root, q, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            ApprovalStatus approvalStatus = parseStatus(status);
            if (approvalStatus != null) {
                predicates.add(cb.equal(root.get("approvalStatus"), approvalStatus));
            }
            if (StringUtils.hasText(query)) {
                String likeQuery = "%" + query.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("accommodationsName")), likeQuery),
                        cb.like(cb.lower(root.get("city")), likeQuery),
                        cb.like(cb.lower(root.get("district")), likeQuery)
                ));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private ApprovalStatus parseStatus(String status) {
        if (!StringUtils.hasText(status) || "all".equalsIgnoreCase(status)) {
            return null;
        }
        String normalized = status.trim().toUpperCase();
        return switch (normalized) {
            case "PENDING", "WAIT", "REQUEST" -> ApprovalStatus.PENDING;
            case "APPROVED", "APPROVE" -> ApprovalStatus.APPROVED;
            case "REJECTED", "REJECT", "DENIED" -> ApprovalStatus.REJECTED;
            default -> null;
        };
    }

    private AdminAccommodationSummary toSummary(Accommodation accommodation) {
        return new AdminAccommodationSummary(
                accommodation.getAccommodationsId(),
                accommodation.getUserId(),
                accommodation.getAccommodationsName(),
                accommodation.getAccommodationsCategory() != null ? accommodation.getAccommodationsCategory().name() : null,
                accommodation.getCity(),
                accommodation.getDistrict(),
                accommodation.getApprovalStatus() != null ? accommodation.getApprovalStatus().name() : null,
                accommodation.getRejectionReason(),
                accommodation.getCreatedAt()
        );
    }

    private AdminAccommodationDetail toDetail(Accommodation accommodation) {
        return new AdminAccommodationDetail(
                accommodation.getAccommodationsId(),
                accommodation.getUserId(),
                accommodation.getAccommodationsName(),
                accommodation.getAccommodationsCategory() != null ? accommodation.getAccommodationsCategory().name() : null,
                accommodation.getCity(),
                accommodation.getDistrict(),
                accommodation.getApprovalStatus() != null ? accommodation.getApprovalStatus().name() : null,
                accommodation.getRejectionReason(),
                accommodation.getCreatedAt()
        );
    }
}
