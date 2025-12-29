package com.ssg9th2team.geharbang.domain.admin.service;

import com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation;
import com.ssg9th2team.geharbang.domain.accommodation.entity.ApprovalStatus;
import com.ssg9th2team.geharbang.domain.accommodation.repository.jpa.AccommodationJpaRepository;
import com.ssg9th2team.geharbang.domain.auth.entity.User;
import com.ssg9th2team.geharbang.domain.auth.entity.UserRole;
import com.ssg9th2team.geharbang.domain.auth.repository.UserRepository;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminAccommodationDetail;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminAccommodationSummary;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminAccommodationService {

    private final AccommodationJpaRepository accommodationRepository;
    private final UserRepository userRepository;

    public AdminPageResponse<AdminAccommodationSummary> getAccommodations(
            String status,
            String keyword,
            int page,
            int size,
            String sort
    ) {
        Sort sorting = "oldest".equalsIgnoreCase(sort)
                ? Sort.by(Sort.Direction.ASC, "createdAt")
                : Sort.by(Sort.Direction.DESC, "createdAt");

        List<Accommodation> filtered = accommodationRepository.findAll(sorting).stream()
                .filter(accommodation -> matchesStatus(accommodation, status))
                .filter(accommodation -> matchesKeyword(accommodation, keyword))
                .toList();

        int totalElements = filtered.size();
        int totalPages = size > 0 ? (int) Math.ceil(totalElements / (double) size) : 1;
        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<AdminAccommodationSummary> items = filtered.subList(fromIndex, toIndex).stream()
                .map(this::toSummary)
                .toList();

        return AdminPageResponse.of(items, page, size, totalElements, totalPages);
    }

    public AdminAccommodationDetail getAccommodationDetail(Long accommodationId) {
        Accommodation accommodation = accommodationRepository.findById(accommodationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Accommodation not found"));
        return toDetail(accommodation);
    }

    public AdminAccommodationDetail approveAccommodation(Long accommodationId) {
        Accommodation accommodation = accommodationRepository.findById(accommodationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Accommodation not found"));
        accommodation.updateApprovalStatus(ApprovalStatus.APPROVED, null);
        promoteUserToHost(accommodation.getUserId());
        return toDetail(accommodationRepository.save(accommodation));
    }

    public AdminAccommodationDetail rejectAccommodation(Long accommodationId, String reason) {
        Accommodation accommodation = accommodationRepository.findById(accommodationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Accommodation not found"));
        accommodation.reject(reason);
        return toDetail(accommodationRepository.save(accommodation));
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

    private boolean matchesStatus(Accommodation accommodation, String status) {
        ApprovalStatus approvalStatus = parseStatus(status);
        return approvalStatus == null || approvalStatus == accommodation.getApprovalStatus();
    }

    private boolean matchesKeyword(Accommodation accommodation, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String normalized = keyword.toLowerCase();
        String name = accommodation.getAccommodationsName() != null
                ? accommodation.getAccommodationsName().toLowerCase()
                : "";
        String city = accommodation.getCity() != null ? accommodation.getCity().toLowerCase() : "";
        String district = accommodation.getDistrict() != null ? accommodation.getDistrict().toLowerCase() : "";
        return name.contains(normalized) || city.contains(normalized) || district.contains(normalized);
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

    private void promoteUserToHost(Long userId) {
        if (userId == null) {
            return;
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getRole() != UserRole.HOST) {
            user.updateRole(UserRole.HOST);
        }
        user.updateHostApproved(true);
        userRepository.save(user);
    }
}
