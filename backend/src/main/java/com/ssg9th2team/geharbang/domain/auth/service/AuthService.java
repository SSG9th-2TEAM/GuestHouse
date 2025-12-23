package com.ssg9th2team.geharbang.domain.auth.service;

import com.ssg9th2team.geharbang.domain.auth.dto.LoginRequest;
import com.ssg9th2team.geharbang.domain.auth.dto.SignupRequest;
import com.ssg9th2team.geharbang.domain.auth.dto.TokenResponse;
import com.ssg9th2team.geharbang.domain.auth.dto.UserResponse;
import com.ssg9th2team.geharbang.domain.auth.dto.VerifyCodeRequest;

public interface AuthService {

    // 회원가입
    UserResponse signup(SignupRequest signupRequest);

    // 로그인
    TokenResponse login(LoginRequest loginRequest);

    // 이메일 중복 확인
    boolean checkEmailDuplicate(String email);

    // 이메일 인증 코드 전송
    void sendVerificationCode(String email);

    // 이메일 인증 코드 확인
    boolean verifyEmailCode(VerifyCodeRequest verifyCodeRequest);

    // 리프레시 토큰으로 액세스 토큰 재발급
    TokenResponse refresh(String refreshToken);
}
