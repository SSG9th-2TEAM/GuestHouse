package com.ssg9th2team.geharbang.domain.user.service;

import com.ssg9th2team.geharbang.domain.auth.entity.User;
import com.ssg9th2team.geharbang.domain.auth.repository.UserRepository;
import com.ssg9th2team.geharbang.domain.reservation.entity.Reservation;
import com.ssg9th2team.geharbang.domain.reservation.repository.jpa.ReservationJpaRepository;
import com.ssg9th2team.geharbang.domain.user.dto.DeleteAccountRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ReservationJpaRepository reservationRepository;

    @Override
    @Transactional
    public void deleteUser(String email, DeleteAccountRequest deleteAccountRequest) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // Check for active reservations
        List<Reservation> reservations = reservationRepository.findByUserId(user.getId());
        boolean hasActiveReservations = reservations.stream()
                .anyMatch(r -> r.getReservationStatus() != 3); // 3: Cancelled

        if (hasActiveReservations) {
            throw new IllegalStateException("진행 중이거나 완료된 예약이 있어 탈퇴할 수 없습니다. 모든 예약을 취소한 후 다시 시도해주세요.");
        }

        // Log the reasons for deletion
        log.info("사용자 {} 탈퇴. 사유: {}, 기타: {}", email, deleteAccountRequest.getReasons(), deleteAccountRequest.getOtherReason());

        userRepository.delete(user);
    }
}
