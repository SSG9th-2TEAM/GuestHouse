package com.ssg9th2team.geharbang.domain.coupon.service;

import com.ssg9th2team.geharbang.domain.coupon.dto.UserCouponResponseDto;
import com.ssg9th2team.geharbang.domain.coupon.entity.Coupon;
import com.ssg9th2team.geharbang.domain.coupon.entity.CouponIssueResult;
import com.ssg9th2team.geharbang.domain.coupon.entity.CouponTriggerType;
import com.ssg9th2team.geharbang.domain.coupon.entity.UserCoupon;
import com.ssg9th2team.geharbang.domain.coupon.entity.UserCouponStatus;
import com.ssg9th2team.geharbang.domain.coupon.repository.jpa.CouponInventoryRepository;
import com.ssg9th2team.geharbang.domain.coupon.repository.jpa.CouponJpaRepository;
import com.ssg9th2team.geharbang.domain.coupon.repository.jpa.UserCouponJpaRepository;
import com.ssg9th2team.geharbang.domain.coupon.repository.mybatis.UserCouponMapper;
import com.ssg9th2team.geharbang.domain.review.repository.jpa.ReviewJpaRepository;
import com.ssg9th2team.geharbang.domain.reservation.repository.jpa.ReservationJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserCouponServiceImpl implements UserCouponService {

    private final UserCouponJpaRepository userCouponJpaRepository;
    private final CouponJpaRepository couponJpaRepository;
    private final CouponInventoryService couponInventoryService;
    private final CouponInventoryRepository couponInventoryRepository;
    private final UserCouponMapper userCouponMapper;
    private final ReservationJpaRepository reservationJpaRepository;
    private final ReviewJpaRepository reviewJpaRepository;
    private final StringRedisTemplate redisTemplate;
    private final CacheManager cacheManager;
    
    private static final String COUPON_ISSUED_KEY_PREFIX = "coupon:issued:";

    // 쿠폰 발급 (수동 - 숙소 상세페이지에서 쿠폰 받기 등)
    @Override
    @Transactional
    public void issueCoupon(Long userId, Long couponId) {
        Coupon coupon = couponJpaRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다."));

        // 이 쿠폰이 '다운로드용'인지 확인!
        if (coupon.getTriggerType() != CouponTriggerType.DOWNLOAD && coupon.getTriggerType() != CouponTriggerType.EVENT) {
             throw new IllegalArgumentException("직접 다운로드할 수 없는 쿠폰입니다.");
        }

        // 활성화 체크
        if (!Boolean.TRUE.equals(coupon.getIsActive())) {
            throw new IllegalArgumentException("현재 발급 불가능한 쿠폰입니다.");
        }

        // 공통 발급 메서드 호출
        CouponIssueResult result = issueToUser(userId, coupon);
        if (result == CouponIssueResult.DUPLICATED) {
            throw new IllegalArgumentException("이미 발급 받은 쿠폰입니다.");
        } else if (result == CouponIssueResult.SOLD_OUT) {
            throw new IllegalStateException("오늘 선착순 수량이 모두 소진되었습니다.");
        }
    }


    // 첫 예약 완료 시 쿠폰 자동 발급 (PaymentService에서 호출)
    @Override
    @Transactional
    public boolean issueFirstReservationCoupon(Long userId) {
        // 내 계정에서 완료된 예약 수 조회 ( reservationStatus가 2인게 몇개인지 조회해라 )
        Long completedCount = reservationJpaRepository.countByUserIdAndReservationStatus(userId, 2);

        // 첫 예약인지 체크
        if(completedCount != 1) {   // 여기서 1은 예약 확정 된게 1개이면
            return false;
        }

        return issueByTrigger(userId, CouponTriggerType.FIRST_RESERVATION);
    }



    // 리뷰 달성 시 쿠폰 자동 발급 (ReviewService에서 호출)
    // 3, 6, 9회 → REVIEW_3 쿠폰 / 10회 → REVIEW_10 쿠폰 / 11회 이후 → 없음
    @Override
    @Transactional
    public boolean issueReviewRewardCoupon(Long userId) {
        long reviewCount = reviewJpaRepository.countByUserIdAndIsDeletedFalse(userId);

        // 10회 달성 시 REVIEW_10 쿠폰 발급
        if (reviewCount == 10) {
            return issueByTrigger(userId, CouponTriggerType.REVIEW_10);
        }

        // 3, 6, 9회일 때만 REVIEW_3 쿠폰 발급 (10회 미만)
        if (reviewCount < 10 && reviewCount % 3 == 0 && reviewCount > 0) {
            return issueByTrigger(userId, CouponTriggerType.REVIEW_3);
        }

        return false;
    }



    // 자동 발급 (트리거 타입 기반) -> 회원가입, 리뷰3번 등등
    @Override
    @Transactional
    public boolean issueByTrigger(Long userId, CouponTriggerType trigger) {
        Coupon coupon = couponJpaRepository.findByTriggerTypeAndIsActiveTrue(trigger)
                .orElse(null);

        if (coupon == null) {
            return false;  // 해당 트리거 타입의 활성화된 쿠폰이 없음
        }

        return issueToUser(userId, coupon) == CouponIssueResult.SUCCESS;
    }


    // 공통 발급 로직 (만료일 자동 계산)
    @Override
    @Transactional
    public CouponIssueResult issueToUser(Long userId, Coupon coupon) {
        // 1. Redis Set으로 중복 체크 (O(1))
        String redisKey = COUPON_ISSUED_KEY_PREFIX + coupon.getCouponId();
        Long addCount = redisTemplate.opsForSet().add(redisKey, userId.toString());
        
        if (addCount != null && addCount == 0) {
            // Redis에 이미 존재 → 중복 발급
            log.debug("쿠폰 {} 중복 발급 차단 - userId: {}", coupon.getCouponId(), userId);
            return CouponIssueResult.DUPLICATED;
        }
        
        // DB에도 확인 (Redis 장애 대비 이중 체크)
        if (userCouponJpaRepository.existsByUserIdAndCouponId(userId, coupon.getCouponId())) {
            // Redis에는 없었지만 DB에 있음 → Redis 동기화 필요
            log.warn("쿠폰 {} Redis-DB 불일치 감지 - userId: {}", coupon.getCouponId(), userId);
            // Redis에서 제거 (동기화)
            redisTemplate.opsForSet().remove(redisKey, userId.toString());
            return CouponIssueResult.DUPLICATED;
        }

        // 2. 선착순 제한 확인
        boolean slotAvailable = couponInventoryService.consumeSlotIfLimited(coupon.getCouponId());
        if (!slotAvailable) {
            return CouponIssueResult.SOLD_OUT;
        }

        // 2. 만료일 계산 (Coupon 엔티티의 calculateExpiryDate 메서드 사용)
        LocalDateTime expiresAt = coupon.calculateExpiryDate();

        // 3. UserCoupon 생성 및 저장
        UserCoupon userCoupon = UserCoupon.issue(userId, coupon.getCouponId(), expiresAt);
        userCouponJpaRepository.save(userCoupon);
        evictUserCouponCache(userId, "ISSUED");

        return CouponIssueResult.SUCCESS;
    }



    // 사용 가능 쿠폰, 만료 쿠폰, 사용 완료한 쿠폰 조회
    @Override
    @Cacheable(value = "userCoupons", key = "#userId + '_' + #status")
    @Transactional(readOnly = true)
    public List<UserCouponResponseDto> getMyCouponsByStatus(Long userId, String status) {
        //  DTO 리스트 반환
        return userCouponMapper.selectMyCouponsByStatus(userId, status);
    }




    // 쿠폰 사용 처리
    @Override
    @Transactional
    public void useCoupon(Long userId, Long userCouponId) {
        // 쿠폰 조회
        UserCoupon userCoupon = userCouponJpaRepository.findById(userCouponId)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다."));

        // 본인 쿠폰인지 확인
        if (!userCoupon.getUserId().equals(userId)) {
            throw new IllegalArgumentException("본인의 쿠폰만 사용할 수 있습니다.");
        }

        // 사용 가능 상태인지 확인
        if (userCoupon.getStatus() != UserCouponStatus.ISSUED) {
            throw new IllegalArgumentException("사용할 수 없는 쿠폰입니다.");
        }

        // 만료 여부 확인
        if (userCoupon.getExpiredAt() != null && userCoupon.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("만료된 쿠폰입니다.");
        }

        userCoupon.use();
        evictUserCouponCache(userId, "ISSUED");
        evictUserCouponCache(userId, "USED");
    }


    // 쿠폰 복구 처리 (예약 취소 시 호출)
    @Override
    @Transactional
    public void restoreCoupon(Long userId, Long userCouponId) {
        // 쿠폰 조회
        UserCoupon userCoupon = userCouponJpaRepository.findById(userCouponId)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다."));

        // 본인 쿠폰인지 확인
        if (!userCoupon.getUserId().equals(userId)) {
            throw new IllegalArgumentException("본인의 쿠폰만 복구할 수 있습니다.");
        }

        // 사용 완료 상태인지 확인
        if (userCoupon.getStatus() != UserCouponStatus.USED) {
            return;  // 이미 복구되었거나 다른 상태면 무시
        }

        // 만료일 체크 후 복구
        if (userCoupon.isRestorable()) {
            userCoupon.restore();
        }
        // 만료일이 지났으면 복구하지 않음 (만료 상태로 변경)
        else {
            userCoupon.expire();
        }

        evictUserCouponCache(userId, "ISSUED");
        evictUserCouponCache(userId, "USED");
        evictUserCouponCache(userId, "EXPIRED");
    }


    // 새로운 쿠폰 발급시 캐시 무효화 후 새로운 캐시
    private void evictUserCouponCache(Long userId, String status) {
        Cache cache = cacheManager.getCache("userCoupons");
        if (cache == null) {
            return;
        }
        cache.evict(userId + "_" + status);
    }


    // 만료된 쿠폰 상태 변경 (스케줄러에서 호출)
    @Override
    @Transactional
    public int expireOverdueCoupons() {
        List<UserCoupon> expiredCoupons = userCouponJpaRepository
                .findByStatusAndExpiredAtBefore(UserCouponStatus.ISSUED, LocalDateTime.now());

        Set<Long> affectedUserIds = new HashSet<>();
        for (UserCoupon coupon : expiredCoupons) {
            coupon.expire();
            affectedUserIds.add(coupon.getUserId());
        }

        for (Long userId : affectedUserIds) {
            evictUserCouponCache(userId, "ISSUED");
            evictUserCouponCache(userId, "EXPIRED");
        }

        return expiredCoupons.size();
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> getMyCouponIds(Long userId) {
        return userCouponJpaRepository.findCouponIdsByUserId(userId);
    }


    /**
     * Redis Set 초기화 - DB의 발급 이력을 Redis에 동기화
     * 애플리케이션 시작 시 또는 스케줄러에서 호출
     * [HIGH] OOM 방지: Stream 사용 + ReadOnly 트랜잭션
     */
    @Transactional(readOnly = true)
    public void initializeRedisIssuedCoupons() {
        try (Stream<UserCoupon> stream = userCouponJpaRepository.streamAll()) {
            // Stream은 한 번만 순회 가능하므로 forEach 내부에서 카운팅이 어려움
            // 여기서는 단순 반복 처리
            stream.forEach(userCoupon -> {
                String redisKey = COUPON_ISSUED_KEY_PREFIX + userCoupon.getCouponId();
                redisTemplate.opsForSet().add(redisKey, userCoupon.getUserId().toString());
            });
        }
        
        // 정확한 카운트는 별도 쿼리로 조회하거나 AtomicInteger 사용 가능하나, 로그용이므로 생략하거나 대략적 처리
        log.info("Redis 쿠폰 발급 이력 초기화 완료 (Stream 처리)");
    }

    /**
     * 특정 쿠폰의 발급 이력을 Redis에 동기화
     */
    public void syncRedisIssuedCoupon(Long couponId) {
        // [HIGH] 성능 최적화: couponId로 직접 조회 (전체 스캔 방지)
        List<UserCoupon> issuedCoupons = userCouponJpaRepository.findAllByCouponId(couponId);
        
        String redisKey = COUPON_ISSUED_KEY_PREFIX + couponId;
        // 기존 Set 삭제
        redisTemplate.delete(redisKey);
        
        // 재생성
        for (UserCoupon userCoupon : issuedCoupons) {
            redisTemplate.opsForSet().add(redisKey, userCoupon.getUserId().toString());
        }
        
        log.info("쿠폰 {} Redis 발급 이력 동기화: {}건", couponId, issuedCoupons.size());
    }
    
    /**
     * 일일 선착순 쿠폰의 발급 이력을 초기화한다.
     * 매일 자정에 실행되어 사용자가 다시 쿠폰을 발급받을 수 있도록 함.
     */
    @Override
    public int resetDailyCouponIssuedTracking() {
        // 1. CouponInventory에서 재고 관리되는 쿠폰들의 ID 조회
        // (선착순 쿠폰만 CouponInventory에 존재)
        List<Long> limitedCouponIds = couponInventoryRepository.findAllCouponIds();

        if (limitedCouponIds.isEmpty()) {
            log.info("초기화할 일일 쿠폰 발급 이력이 없습니다.");
            return 0;
        }

        // 2. 각 쿠폰의 발급 이력 Redis Set 삭제 (bulk)
        List<String> keysToDelete = limitedCouponIds.stream()
                .map(couponId -> COUPON_ISSUED_KEY_PREFIX + couponId)
                .toList();
        Long deletedCount = redisTemplate.delete(keysToDelete);
        int cleared = deletedCount != null ? deletedCount.intValue() : 0;

        log.info("일일 쿠폰 발급 이력 초기화: {}건", cleared);
        return cleared;
    }
}
