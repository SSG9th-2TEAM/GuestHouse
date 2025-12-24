package com.ssg9th2team.geharbang.domain.auth.controller;

import com.ssg9th2team.geharbang.domain.auth.dto.EmailRequest;
import com.ssg9th2team.geharbang.domain.auth.dto.FindEmailRequest;
import com.ssg9th2team.geharbang.domain.auth.dto.FindEmailResponse;
import com.ssg9th2team.geharbang.domain.auth.dto.FindPasswordRequest;
import com.ssg9th2team.geharbang.domain.auth.dto.LoginRequest;
import com.ssg9th2team.geharbang.domain.auth.dto.ResetPasswordRequest;
import com.ssg9th2team.geharbang.domain.auth.dto.SignupRequest;
import com.ssg9th2team.geharbang.domain.auth.dto.TokenResponse;
import com.ssg9th2team.geharbang.domain.auth.dto.UserResponse;
import com.ssg9th2team.geharbang.domain.auth.dto.VerifyCodeRequest;
import com.ssg9th2team.geharbang.domain.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;


    //회원가입
    @PostMapping("/signup")
    public ResponseEntity<UserResponse> signup(@Valid @RequestBody SignupRequest signupRequest) {
        log.info("회원가입 요청: {}", signupRequest.getEmail());
        UserResponse userResponse = authService.signup(signupRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(userResponse);
    }

    //로그인
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("로그인 요청: {}", loginRequest.getEmail());
        TokenResponse tokenResponse = authService.login(loginRequest);
        return ResponseEntity.ok(tokenResponse);
    }

    // 이메일 찾기
    @PostMapping("/find-email")
    public ResponseEntity<FindEmailResponse> findEmail(@Valid @RequestBody FindEmailRequest findEmailRequest) {
        log.info("이메일 찾기 요청: {} / {}", findEmailRequest.getName(), findEmailRequest.getPhone());
        FindEmailResponse response = authService.findEmail(findEmailRequest);
        return ResponseEntity.ok(response);
    }

    // 비밀번호 찾기 - 사용자 확인 및 인증 코드 전송
    @PostMapping("/find-password")
    public ResponseEntity<Void> findPassword(@Valid @RequestBody FindPasswordRequest findPasswordRequest) {
        log.info("비밀번호 찾기 요청: {} / {}", findPasswordRequest.getEmail(), findPasswordRequest.getPhone());
        authService.findPassword(findPasswordRequest);
        return ResponseEntity.ok().build();
    }

    // 비밀번호 재설정
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest resetPasswordRequest) {
        log.info("비밀번호 재설정 요청: {}", resetPasswordRequest.getEmail());
        authService.resetPassword(resetPasswordRequest);
        return ResponseEntity.ok().build();
    }

    //이메일 중복  확인
    @GetMapping("/check-email")
    public ResponseEntity<Boolean> checkEmailDuplicate(@RequestParam String email) {
        log.info("이메일 중복 확인 요청: {}", email);
        boolean isDuplicate = authService.checkEmailDuplicate(email);
        return ResponseEntity.ok(isDuplicate);
    }

    // 닉네임 중복 확인
    @GetMapping("/check-nickname")
    public ResponseEntity<Boolean> checkNicknameDuplicate(@RequestParam String nickname) {
        log.info("닉네임 중복 확인 요청: {}", nickname);
        boolean isDuplicate = authService.checkNicknameDuplicate(nickname);
        return ResponseEntity.ok(isDuplicate);
    }


    //이메일 인증 코드 전송
    @PostMapping("/send-verification")
    public ResponseEntity<Void> sendVerificationCode(@Valid @RequestBody EmailRequest emailRequest) {
        log.info("이메일 인증 코드 전송 요청: {}", emailRequest.getEmail());
        authService.sendVerificationCode(emailRequest.getEmail());
        return ResponseEntity.ok().build();
    }


    //이메일 인증 코드 확인
    @PostMapping("/verify-code")
    public ResponseEntity<Boolean> verifyCode(@Valid @RequestBody VerifyCodeRequest verifyCodeRequest) {
        log.info("이메일 인증 코드 확인 요청: {} / 코드: {}", verifyCodeRequest.getEmail(), verifyCodeRequest.getCode());
        boolean isVerified = authService.verifyEmailCode(verifyCodeRequest);
        return ResponseEntity.ok(isVerified);
    }

    //이메일 인증 코드 확인만 (삭제하지 않음) - 비밀번호 찾기용
    @PostMapping("/verify-code-only")
    public ResponseEntity<Boolean> verifyCodeOnly(@Valid @RequestBody VerifyCodeRequest verifyCodeRequest) {
        log.info("이메일 인증 코드 확인만 요청: {} / 코드: {}", verifyCodeRequest.getEmail(), verifyCodeRequest.getCode());
        boolean isVerified = authService.verifyEmailCodeOnly(verifyCodeRequest);
        return ResponseEntity.ok(isVerified);
    }


    //토큰 갱신
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestHeader("Authorization") String refreshToken) {
        log.info("토큰 갱신 요청");
        // Bearer 제거
        String token = refreshToken.replace("Bearer ", "");
        TokenResponse tokenResponse = authService.refresh(token);
        return ResponseEntity.ok(tokenResponse);
    }
}
