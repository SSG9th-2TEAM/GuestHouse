package com.ssg9th2team.geharbang.domain.admin.service;

import com.ssg9th2team.geharbang.domain.admin.dto.AdminPageResponse;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminUserSummary;
import com.ssg9th2team.geharbang.domain.auth.entity.User;
import com.ssg9th2team.geharbang.domain.auth.entity.UserRole;
import com.ssg9th2team.geharbang.domain.auth.repository.UserRepository;
import com.ssg9th2team.geharbang.domain.auth.spec.UserSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

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
        // 체크: role/query 필터 + page/size가 DB 페이징으로 정상 동작하는지 확인.
        int safePage = Math.max(0, page);
        int safeSize = size > 0 ? Math.min(size, 50) : 20;
        Sort sorting = resolveUserSort(sort);

        UserRole targetRole = parseRole(role);
        Page<User> pageResult = userRepository.findAll(
                UserSpecifications.hasRole(targetRole)
                        .and(UserSpecifications.keywordContains(query)),
                PageRequest.of(safePage, safeSize, sorting)
        );

        return AdminPageResponse.of(
                pageResult.getContent().stream().map(this::toSummary).toList(),
                safePage,
                safeSize,
                (int) pageResult.getTotalElements(),
                pageResult.getTotalPages()
        );
    }

    private Sort resolveUserSort(String sort) {
        if (!StringUtils.hasText(sort) || "latest".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        if ("oldest".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.ASC, "createdAt");
        }
        if ("idAsc".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.ASC, "id");
        }
        if ("idDesc".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.DESC, "id");
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sort value");
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
