package com.ssg9th2team.geharbang.domain.admin.controller;

import com.ssg9th2team.geharbang.domain.admin.support.AdminId;
import com.ssg9th2team.geharbang.domain.coupon.service.CouponIssueQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/coupons/async")
@RequiredArgsConstructor
public class AdminCouponAsyncController {

    private final CouponIssueQueueService couponIssueQueueService;

    @Value("${coupon.issue.async-enabled:true}")
    private boolean asyncEnabled;

    @Value("${coupon.issue.async-processor.batch-size:200}")
    private int batchSize;

    @Value("${coupon.issue.async-processor.delay-ms:200}")
    private long delayMs;

    @GetMapping("/queues")
    public Map<String, Object> getQueueStatus(@AdminId Long adminId) {
        Map<String, Object> response = new HashMap<>();
        response.put("asyncEnabled", asyncEnabled);
        response.put("batchSize", batchSize);
        response.put("delayMs", delayMs);
        response.put("queueSize", couponIssueQueueService.getQueueSize());
        response.put("retrySize", couponIssueQueueService.getRetrySize());
        return response;
    }

    @PostMapping("/retry/requeue")
    public Map<String, Object> requeueRetry(
            @AdminId Long adminId,
            @RequestParam(defaultValue = "1000") int limit
    ) {
        long moved = couponIssueQueueService.requeueRetry(limit);
        Map<String, Object> response = new HashMap<>();
        response.put("moved", moved);
        response.put("queueSize", couponIssueQueueService.getQueueSize());
        response.put("retrySize", couponIssueQueueService.getRetrySize());
        return response;
    }

    @DeleteMapping("/retry")
    public Map<String, Object> clearRetry(@AdminId Long adminId) {
        couponIssueQueueService.clearRetryQueue();
        Map<String, Object> response = new HashMap<>();
        response.put("queueSize", couponIssueQueueService.getQueueSize());
        response.put("retrySize", couponIssueQueueService.getRetrySize());
        return response;
    }
}
