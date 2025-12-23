package com.ssg9th2team.geharbang.domain.auth.controller;

import com.ssg9th2team.geharbang.domain.auth.dto.EmailRequest;
import com.ssg9th2team.geharbang.domain.auth.dto.LoginRequest;
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


    //이메일 중복  확인
    @GetMapping("/check-email")
    public ResponseEntity<Boolean> checkEmailDuplicate(@RequestParam String email) {
        log.info("이메일 중복 확인 요청: {}", email);
        boolean isDuplicate = authService.checkEmailDuplicate(email);
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
