package com.ssg9th2team.geharbang.domain.admin.service;

import com.ssg9th2team.geharbang.domain.admin.repository.mybatis.AdminLogMapper;
import com.ssg9th2team.geharbang.domain.auth.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminLogService {

    private final AdminLogMapper adminLogMapper;
    private final AdminRepository adminRepository;

    public void writeLog(String targetType, Long targetId, String actionType, String reason) {
        try {
            Long adminId = resolveAdminId();
            if (adminId == null || targetId == null) {
                return;
            }
            adminLogMapper.insertAdminLog(adminId, targetType, targetId, actionType, reason);
        } catch (Exception e) {
            log.warn("Failed to insert admin_log: targetType={}, targetId={}, actionType={}",
                    targetType, targetId, actionType, e);
        }
    }

    private Long resolveAdminId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        String username = authentication.getName();
        if (!StringUtils.hasText(username)) {
            return null;
        }
        return adminRepository.findByUsername(username)
                .map(admin -> admin.getId())
                .orElse(null);
    }
}
