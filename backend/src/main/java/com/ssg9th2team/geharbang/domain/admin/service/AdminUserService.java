package com.ssg9th2team.geharbang.domain.admin.service;

import com.ssg9th2team.geharbang.domain.admin.dto.AdminPageResponse;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminUserSummary;
import com.ssg9th2team.geharbang.domain.auth.entity.User;
import com.ssg9th2team.geharbang.domain.auth.entity.UserRole;
import com.ssg9th2team.geharbang.domain.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
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
        Specification<User> spec = buildSpecification(role, query);
        Sort sorting = "oldest".equalsIgnoreCase(sort)
                ? Sort.by(Sort.Direction.ASC, "createdAt")
                : Sort.by(Sort.Direction.DESC, "createdAt");

        Page<User> result = userRepository.findAll(spec, PageRequest.of(page, size, sorting));
        List<AdminUserSummary> items = result.stream()
                .map(this::toSummary)
                .toList();

        return AdminPageResponse.of(items, page, size, result.getTotalElements(), result.getTotalPages());
    }

    private Specification<User> buildSpecification(String role, String query) {
        return (root, q, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            UserRole userRole = parseRole(role);
            if (userRole != null) {
                predicates.add(cb.equal(root.get("role"), userRole));
            }
            if (StringUtils.hasText(query)) {
                String likeQuery = "%" + query.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("email")), likeQuery),
                        cb.like(cb.lower(root.get("phone")), likeQuery)
                ));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
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
