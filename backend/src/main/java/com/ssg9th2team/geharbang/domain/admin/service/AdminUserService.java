package com.ssg9th2team.geharbang.domain.admin.service;

import com.ssg9th2team.geharbang.domain.admin.dto.AdminPageResponse;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminUserSummary;
import com.ssg9th2team.geharbang.domain.auth.entity.User;
import com.ssg9th2team.geharbang.domain.auth.entity.UserRole;
import com.ssg9th2team.geharbang.domain.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;

    public AdminPageResponse<AdminUserSummary> getUsers(
            String role,
            String query,
            int page,
            int size,
            String sort
    ) {
        Sort sorting = "oldest".equalsIgnoreCase(sort)
                ? Sort.by(Sort.Direction.ASC, "createdAt")
                : Sort.by(Sort.Direction.DESC, "createdAt");

        List<User> filtered = userRepository.findAll(sorting).stream()
                .filter(user -> matchesRole(user, role))
                .filter(user -> matchesQuery(user, query))
                .toList();

        int totalElements = filtered.size();
        int totalPages = size > 0 ? (int) Math.ceil(totalElements / (double) size) : 1;
        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<AdminUserSummary> items = filtered.subList(fromIndex, toIndex).stream()
                .map(this::toSummary)
                .toList();

        return AdminPageResponse.of(items, page, size, totalElements, totalPages);
    }

    private UserRole parseRole(String role) {
        if (!StringUtils.hasText(role) || "all".equalsIgnoreCase(role)) {
            return null;
        }
        String normalized = role.trim().toUpperCase();
        return switch (normalized) {
            case "HOST", "ROLE_HOST" -> UserRole.HOST;
            case "USER", "ROLE_USER", "GUEST" -> UserRole.USER;
            default -> null;
        };
    }

    private boolean matchesRole(User user, String role) {
        UserRole userRole = parseRole(role);
        return userRole == null || userRole == user.getRole();
    }

    private boolean matchesQuery(User user, String query) {
        if (!StringUtils.hasText(query)) {
            return true;
        }
        String normalized = query.toLowerCase();
        String email = user.getEmail() != null ? user.getEmail().toLowerCase() : "";
        String phone = user.getPhone() != null ? user.getPhone().toLowerCase() : "";
        return email.contains(normalized) || phone.contains(normalized);
    }

    private AdminUserSummary toSummary(User user) {
        return new AdminUserSummary(
                user.getId(),
                user.getEmail(),
                user.getRole() != null ? user.getRole().name() : null,
                user.getPhone(),
                user.getHostApproved(),
                user.getCreatedAt()
        );
    }
}
