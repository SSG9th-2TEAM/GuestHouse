package com.ssg9th2team.geharbang.global.oauth.service;

import com.ssg9th2team.geharbang.domain.auth.entity.SocialProvider;
import com.ssg9th2team.geharbang.domain.auth.entity.User;
import com.ssg9th2team.geharbang.domain.auth.entity.UserRole;
import com.ssg9th2team.geharbang.domain.auth.entity.UserSocial;
import com.ssg9th2team.geharbang.domain.auth.repository.UserRepository;
import com.ssg9th2team.geharbang.domain.auth.repository.UserSocialRepository;
import com.ssg9th2team.geharbang.domain.user.entity.Gender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final UserSocialRepository userSocialRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        log.info("OAuth2 Login - Provider: {}", registrationId);

        OAuth2UserInfo oAuth2UserInfo = null;

        if ("naver".equals(registrationId)) {
            oAuth2UserInfo = new NaverOAuth2UserInfo(oAuth2User.getAttributes());
        } else if ("google".equals(registrationId)) {
            oAuth2UserInfo = new GoogleOAuth2UserInfo(oAuth2User.getAttributes());
        } else {
            throw new OAuth2AuthenticationException("지원하지 않는 소셜 로그인입니다: " + registrationId);
        }

        String providerId = oAuth2UserInfo.getProviderId();
        String provider = oAuth2UserInfo.getProvider();
        String email = oAuth2UserInfo.getEmail();
        String name = oAuth2UserInfo.getName();
        String genderStr = oAuth2UserInfo.getGender();
        String mobile = oAuth2UserInfo.getMobile();

        log.info("OAuth2 User Info - Provider: {}, ProviderId: {}, Email: {}, Name: {}, Gender: {}, Mobile: {}",
                provider, providerId, email, name, genderStr, mobile);

        SocialProvider socialProvider = SocialProvider.valueOf(provider.toUpperCase());

        // 1. user_social 테이블에서 기존 소셜 로그인 정보 조회
        Optional<UserSocial> userSocialOptional = userSocialRepository.findByProviderAndProviderUid(
                socialProvider,
                providerId
        );

        User user;
        if (userSocialOptional.isEmpty()) {
            // 신규 소셜 로그인 사용자
            log.info("신규 소셜 로그인 사용자 생성: {}", email);

            // 이메일 중복 체크 (일반 회원가입으로 이미 가입된 경우)
            Optional<User> existingUser = userRepository.findByEmail(email);
            
            if (existingUser.isPresent()) {
                // 이메일은 이미 있지만 소셜 로그인은 처음인 경우
                // 기존 User에 소셜 계정 연결
                user = existingUser.get();
                log.info("기존 사용자에 소셜 계정 연결: {}", email);
            } else {
                // 완전히 새로운 사용자 생성
                Gender gender = convertGender(genderStr);

                user = User.builder()
                        .name(name)
                        .nickname(generateUniqueNickname(name))
                        .gender(gender)
                        .email(email)
                        .password(null) // 소셜 로그인은 비밀번호 없음
                        .phone(mobile)
                        .role(UserRole.USER)
                        .marketingAgreed(false)
                        .hostApproved(null)
                        .socialSignupCompleted(false) // 소셜 회원가입 미완료 상태
                        .socialProvider(null) // User 테이블의 socialProvider는 사용하지 않음
                        .socialId(null) // User 테이블의 socialId는 사용하지 않음
                        .build();

                user = userRepository.save(user);
                log.info("신규 사용자 생성 완료: {}", user.getEmail());
            }

            // user_social 테이블에 소셜 로그인 정보 저장
            UserSocial userSocial = UserSocial.builder()
                    .user(user)
                    .provider(socialProvider)
                    .providerUid(providerId)
                    .email(email)
                    .profileImage(null) // 네이버 API에서 프로필 이미지를 받아올 수 있다면 여기에 설정
                    .build();

            userSocialRepository.save(userSocial);
            log.info("소셜 로그인 정보 저장 완료: provider={}, uid={}", socialProvider, providerId);
        } else {
            // 기존 소셜 로그인 사용자
            UserSocial userSocial = userSocialOptional.get();
            user = userSocial.getUser();
            log.info("기존 소셜 로그인 사용자 로그인: {}", user.getEmail());
        }

        return new CustomOAuth2User(user, oAuth2User.getAttributes());
    }

    private String generateUniqueNickname(String baseName) {
        String nickname = baseName;
        int count = 1;
        while (userRepository.existsByNickname(nickname)) {
            nickname = baseName + count;
            count++;
        }
        return nickname;
    }

    /**
     * 네이버 성별 정보를 Gender enum으로 변환
     * @param genderStr 네이버 성별 ("M", "F", "U" 또는 null)
     * @return Gender enum (MALE, FEMALE, 또는 null)
     */
    private Gender convertGender(String genderStr) {
        if (genderStr == null) {
            return null;
        }
        
        return switch (genderStr) {
            case "M" -> Gender.MALE;
            case "F" -> Gender.FEMALE;
            default -> null; // "U" (미확인) 또는 기타 값은 null
        };
    }
}
