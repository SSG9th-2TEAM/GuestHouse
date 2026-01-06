package com.ssg9th2team.geharbang.domain.admin.service;

import com.ssg9th2team.geharbang.domain.admin.dto.AdminLogRow;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminPageResponse;
import com.ssg9th2team.geharbang.domain.admin.repository.mybatis.AdminLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminLogService {

    private final AdminLogMapper adminLogMapper;

    public void writeLog(Long adminUserId, String targetType, Long targetId, String actionType, String reason) {
        writeLog(adminUserId, targetType, targetId, actionType, reason, null);
    }

    public void writeLog(Long adminUserId, String targetType, Long targetId, String actionType, String reason, String metadataJson) {
        try {
            if (adminUserId == null || targetId == null) {
                return;
            }
            String normalizedReason = StringUtils.hasText(reason) ? reason.trim() : null;
            String normalizedMetadata = StringUtils.hasText(metadataJson) ? metadataJson.trim() : null;
            int inserted = adminLogMapper.insertAdminLog(adminUserId, targetType, targetId, actionType, normalizedReason, normalizedMetadata);
            if (inserted > 0) {
                log.info("AdminLog inserted: adminId={}, target={}#{}, action={}", adminUserId, targetType, targetId, actionType);
            }
        } catch (Exception e) {
            log.warn("Failed to insert admin_log: targetType={}, targetId={}, actionType={}",
                    targetType, targetId, actionType, e);
        }
    }

    public AdminPageResponse<AdminLogRow> getLogs(
            LocalDate startDate,
            LocalDate endDate,
            String actionType,
            String keyword,
            int page,
            int size,
            int maxSize
    ) {
        int safePage = Math.max(0, page);
        int safeSize = size > 0 ? Math.min(size, maxSize) : maxSize;
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end = endDate != null ? endDate.plusDays(1).atStartOfDay() : null;
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        Long keywordTargetId = resolveKeywordTargetId(normalizedKeyword);
        if (keywordTargetId != null) {
            normalizedKeyword = null;
        }
        int offset = safePage * safeSize;
        List<AdminLogRow> items = adminLogMapper.selectAdminLogs(
                start, end, actionType, normalizedKeyword, keywordTargetId, safeSize, offset);
        long total = adminLogMapper.countAdminLogs(start, end, actionType, normalizedKeyword, keywordTargetId);
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / safeSize);
        return AdminPageResponse.of(items, safePage, safeSize, total, totalPages);
    }

    private Long resolveKeywordTargetId(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        String normalized = keyword.trim();
        for (int i = 0; i < normalized.length(); i++) {
            if (!Character.isDigit(normalized.charAt(i))) {
                return null;
            }
        }
        try {
            return Long.parseLong(normalized);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
