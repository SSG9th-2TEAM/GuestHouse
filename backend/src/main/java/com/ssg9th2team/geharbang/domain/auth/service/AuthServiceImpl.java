package com.ssg9th2team.geharbang.domain.auth.service;

import com.ssg9th2team.geharbang.domain.auth.dto.LoginRequest;
import com.ssg9th2team.geharbang.domain.auth.dto.SignupRequest;
import com.ssg9th2team.geharbang.domain.auth.dto.TokenResponse;
import com.ssg9th2team.geharbang.domain.auth.dto.UserResponse;
import com.ssg9th2team.geharbang.domain.auth.entity.User;
import com.ssg9th2team.geharbang.domain.auth.entity.UserRole;
import com.ssg9th2team.geharbang.domain.auth.repository.UserRepository;
import com.ssg9th2team.geharbang.domain.theme.entity.Theme;
import com.ssg9th2team.geharbang.domain.theme.repository.ThemeRepository;
import com.ssg9th2team.geharbang.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.ssg9th2team.geharbang.domain.auth.entity.Admin;
import com.ssg9th2team.geharbang.domain.auth.repository.AdminRepository;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final ThemeRepository themeRepository;

    @Override
    @Transactional
    public UserResponse signup(SignupRequest signupRequest) {
        // 1. 이메일 중복 체크
        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // 2. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(signupRequest.getPassword());

        // 3. 관심 테마 조회
        Set<Theme> themes = new HashSet<>();
        if (!CollectionUtils.isEmpty(signupRequest.getThemeIds())) {
            List<Theme> foundThemes = themeRepository.findAllById(signupRequest.getThemeIds());
            themes.addAll(foundThemes);
        }

        // 4. User 엔티티 생성
        User user = User.builder()
                .email(signupRequest.getEmail())
                .password(encodedPassword)
                .phone(signupRequest.getPhone())
                .role(UserRole.USER)
                .marketingAgreed(
                        signupRequest.getMarketingAgreed() != null ? signupRequest.getMarketingAgreed() : false)
                .hostApproved(null)
                .themes(themes)
                .build();

        User savedUser = userRepository.save(user);

        log.info("회원가입 완료: {}", savedUser.getEmail());
        return UserResponse.from(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest loginRequest) {
        // 관리자 우선으로 확인
        Optional<Admin> adminOptional = adminRepository.findByUsername(loginRequest.getEmail());
        if (adminOptional.isPresent()) {
            Admin admin = adminOptional.get();
            if (passwordEncoder.matches(loginRequest.getPassword(), admin.getPassword())) {
                return createAdminToken(admin);
            }
        }

        // 사용자로 확인
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다."));

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다.");
        }

        return createUserToken(user);
    }

    private TokenResponse createAdminToken(Admin admin) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                admin.getUsername(),
                null,
                Collections.singleton(new SimpleGrantedAuthority("ROLE_ADMIN")));

        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

        log.info("관리자 로그인 성공: {}", admin.getUsername());

        return TokenResponse.builder()
                .grantType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessTokenExpiresIn(jwtTokenProvider.getAccessTokenExpiration())
                .role("ADMIN")
                .build();
    }

    private TokenResponse createUserToken(User user) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                null,
                Collections.singleton(new SimpleGrantedAuthority(user.getRole().getKey())));

        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

        log.info("로그인 성공: {}", user.getEmail());

        return TokenResponse.builder()
                .grantType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessTokenExpiresIn(jwtTokenProvider.getAccessTokenExpiration())
                .role(user.getRole().name())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean checkEmailDuplicate(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public TokenResponse refresh(String refreshToken) {
        // 1. Refresh Token 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 Refresh Token입니다.");
        }

        // 2. Refresh Token에서 사용자 정보 추출
        Authentication authentication = jwtTokenProvider.getAuthentication(refreshToken);

        // 3. 새로운 Access Token 생성
        String newAccessToken = jwtTokenProvider.generateAccessToken(authentication);

        log.info("토큰 갱신 성공: {}", authentication.getName());

        return TokenResponse.builder()
                .grantType("Bearer")
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .accessTokenExpiresIn(jwtTokenProvider.getAccessTokenExpiration())
                .build();
    }
}
