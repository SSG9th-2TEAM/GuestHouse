package com.ssg9th2team.geharbang.domain.user.service;

import com.ssg9th2team.geharbang.domain.auth.entity.User;
import com.ssg9th2team.geharbang.domain.auth.entity.UserSocial;
import com.ssg9th2team.geharbang.domain.auth.repository.UserRepository;
import com.ssg9th2team.geharbang.domain.auth.repository.UserSocialRepository;
import com.ssg9th2team.geharbang.domain.reservation.entity.Reservation;
import com.ssg9th2team.geharbang.domain.reservation.repository.jpa.ReservationJpaRepository;
import com.ssg9th2team.geharbang.domain.user.dto.DeleteAccountRequest;
import com.ssg9th2team.geharbang.domain.user.dto.UpdateProfileRequest;
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
    private final UserSocialRepository userSocialRepository;

    @Override
    @Transactional
    public void deleteUser(String email, DeleteAccountRequest deleteAccountRequest) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));


        List<Reservation> reservations = reservationRepository.findByUserId(user.getId());
        boolean hasActiveReservations = reservations.stream()
                .anyMatch(r -> r.getReservationStatus() != 3); // 3: Cancelled

        if (hasActiveReservations) {
            throw new IllegalStateException("진행 중이거나 완료된 예약이 있어 탈퇴할 수 없습니다. 모든 예약을 취소한 후 다시 시도해주세요.");
        }

        // 연결된 소셜 로그인 정보 삭제
        List<UserSocial> userSocials = userSocialRepository.findByUser(user);
        if (!userSocials.isEmpty()) {
            userSocialRepository.deleteAll(userSocials);
            log.info("사용자 {}의 연결된 소셜 로그인 정보 {}개 삭제", email, userSocials.size());
        }

        log.info("사용자 {} 탈퇴. 사유: {}, 기타: {}", email, deleteAccountRequest.getReasons(),
                deleteAccountRequest.getOtherReason());

        userRepository.delete(user);
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    @Override
    @Transactional
    public void updateUserProfile(String email, UpdateProfileRequest request) {
        User user = getUserByEmail(email);

        // 닉네임 중복 확인 (자기 자신은 제외)
        if (userRepository.existsByNicknameAndIdNot(request.getNickname(), user.getId())) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        user.updateProfile(request.getNickname(), request.getPhone());
    }
}
