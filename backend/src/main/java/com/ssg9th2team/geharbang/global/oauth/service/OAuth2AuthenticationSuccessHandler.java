package com.ssg9th2team.geharbang.global.oauth.service;

import com.ssg9th2team.geharbang.domain.auth.entity.User;
import com.ssg9th2team.geharbang.global.security.JwtTokenProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;

    // 상대 경로 사용 (프론트/백엔드 같은 도메인)
    private String redirectUri = "/oauth2/redirect";

    // 상대 경로 사용
    private String signupRedirectUri = "/social-signup";

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        String targetUrl = determineTargetUrl(request, response, authentication);

        if (response.isCommitted()) {
            log.debug("Response has already been committed. Unable to redirect to " + targetUrl);
            return;
        }

        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) {
        // JWT 토큰 생성
        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

        log.info("OAuth2 Login Success - User: {}", authentication.getName());

        // CustomOAuth2User에서 User 정보 가져오기
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        User user = oAuth2User.getUser();

        // 소셜 회원가입 완료 여부 확인
        Boolean socialSignupCompleted = user.getSocialSignupCompleted();

        if (socialSignupCompleted == null || !socialSignupCompleted) {
            // 신규 소셜 사용자 - 회원가입 페이지로 리다이렉트
            log.info("New social user - Redirecting to signup page: {}", user.getEmail());
            return UriComponentsBuilder.fromUriString(signupRedirectUri)
                    .queryParam("accessToken", accessToken)
                    .queryParam("refreshToken", refreshToken)
                    .build()
                    .toUriString();
        } else {
            // 기존 소셜 사용자 - 메인 페이지로 리다이렉트
            log.info("Existing social user - Redirecting to main page: {}", user.getEmail());
            return UriComponentsBuilder.fromUriString(redirectUri)
                    .queryParam("accessToken", accessToken)
                    .queryParam("refreshToken", refreshToken)
                    .build()
                    .toUriString();
        }
    }
}
