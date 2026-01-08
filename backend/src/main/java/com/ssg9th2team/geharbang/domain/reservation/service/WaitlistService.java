package com.ssg9th2team.geharbang.domain.reservation.service;

import com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation;
import com.ssg9th2team.geharbang.domain.accommodation.repository.jpa.AccommodationJpaRepository;
import com.ssg9th2team.geharbang.domain.auth.entity.User;
import com.ssg9th2team.geharbang.domain.auth.repository.UserRepository;
import com.ssg9th2team.geharbang.domain.reservation.entity.Waitlist;
import com.ssg9th2team.geharbang.domain.reservation.repository.jpa.WaitlistJpaRepository;
import com.ssg9th2team.geharbang.domain.room.entity.Room;
import com.ssg9th2team.geharbang.domain.room.repository.jpa.RoomJpaRepository;
import com.ssg9th2team.geharbang.global.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class WaitlistService {

    private final WaitlistJpaRepository waitlistRepository;
    private final UserRepository userRepository;
    private final RoomJpaRepository roomRepository;
    private final AccommodationJpaRepository accommodationRepository;
    private final EmailService emailService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    /**
     * 대기 목록 등록
     */
    @Transactional
    public Long registerWaitlist(Long roomId, Long accommodationId, LocalDateTime checkin,
            LocalDateTime checkout, Integer guestCount) {
        // 현재 로그인 사용자 정보 추출
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("인증된 사용자를 찾을 수 없습니다: " + email));

        // 중복 대기 등록 확인
        if (waitlistRepository.existsByUserIdAndRoomIdAndCheckinAndCheckoutAndIsNotifiedFalse(
                user.getId(), roomId, checkin, checkout)) {
            throw new IllegalStateException("이미 대기 등록되어 있습니다.");
        }

        Waitlist waitlist = Waitlist.builder()
                .userId(user.getId())
                .roomId(roomId)
                .accommodationsId(accommodationId)
                .checkin(checkin)
                .checkout(checkout)
                .guestCount(guestCount)
                .build();

        Waitlist saved = waitlistRepository.save(waitlist);
        log.info("대기 목록 등록: userId={}, roomId={}, date={} ~ {}",
                user.getId(), roomId, checkin, checkout);
        return saved.getId();
    }

    /**
     * 대기 목록 취소
     */
    @Transactional
    public void cancelWaitlist(Long waitlistId) {
        Waitlist waitlist = waitlistRepository.findById(waitlistId)
                .orElseThrow(() -> new IllegalArgumentException("대기 정보를 찾을 수 없습니다: " + waitlistId));

        // 본인 확인
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("인증된 사용자를 찾을 수 없습니다: " + email));

        if (!waitlist.getUserId().equals(user.getId())) {
            throw new IllegalStateException("본인의 대기 목록만 취소할 수 있습니다.");
        }

        waitlistRepository.delete(waitlist);
        log.info("대기 목록 취소: waitlistId={}", waitlistId);
    }

    /**
     * 빈자리 발생 시 대기자에게 알림 발송
     * (미결제 예약 정리 후 호출)
     */
    @Transactional
    public void notifyWaitingUsers(Long roomId, LocalDateTime checkin, LocalDateTime checkout) {
        List<Waitlist> waitingList = waitlistRepository.findWaitingByRoomAndDateRange(
                roomId, checkin, checkout);

        if (waitingList.isEmpty()) {
            return;
        }

        // 대기자의 이메일 정보 일괄 조회 (N+1 방지)
        java.util.Set<Long> userIds = waitingList.stream()
                .map(Waitlist::getUserId)
                .collect(java.util.stream.Collectors.toSet());

        java.util.Map<Long, String> userEmails = userRepository.findAllById(userIds).stream()
                .collect(java.util.stream.Collectors.toMap(User::getId, User::getEmail));

        // 객실 및 숙소 정보 조회
        Room room = roomRepository.findById(roomId).orElse(null);
        if (room == null) {
            log.warn("객실 정보를 찾을 수 없습니다: roomId={}", roomId);
            return;
        }

        Accommodation accommodation = accommodationRepository.findById(room.getAccommodationsId()).orElse(null);
        String accommodationName = accommodation != null ? accommodation.getAccommodationsName() : "숙소";
        String roomName = room.getRoomName() != null ? room.getRoomName() : "객실";

        // 대기자들에게 알림 발송
        for (Waitlist waitlist : waitingList) {
            String email = userEmails.get(waitlist.getUserId());
            if (email == null) {
                log.warn("대기자 사용자 정보를 찾을 수 없음: userId={}", waitlist.getUserId());
                continue;
            }
            try {
                emailService.sendWaitlistNotificationEmail(
                        email,
                        accommodationName,
                        roomName,
                        checkin.format(DATE_FORMATTER),
                        checkout.format(DATE_FORMATTER));
                waitlist.markAsNotified();
                log.info("대기자 알림 발송 완료: email={}, roomId={}", email, roomId);
            } catch (Exception e) {
                log.error("대기자 알림 발송 실패: email={}, error={}", email, e.getMessage());
            }
        }
    }
}
