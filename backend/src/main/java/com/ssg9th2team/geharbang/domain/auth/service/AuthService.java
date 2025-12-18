package com.ssg9th2team.geharbang.domain.auth.service;

import com.ssg9th2team.geharbang.domain.auth.dto.LoginRequest;
import com.ssg9th2team.geharbang.domain.auth.dto.SignupRequest;
import com.ssg9th2team.geharbang.domain.auth.dto.TokenResponse;
import com.ssg9th2team.geharbang.domain.auth.dto.UserResponse;

public interface AuthService {

    // 회원가입
    UserResponse signup(SignupRequest signupRequest);

    // 로그인
    TokenResponse login(LoginRequest loginRequest);

    // 이메일 중복 확인
    boolean checkEmailDuplicate(String email);

    // 리프레시 토큰으로 액세스 토큰 재발급
    TokenResponse refresh(String refreshToken);
}
