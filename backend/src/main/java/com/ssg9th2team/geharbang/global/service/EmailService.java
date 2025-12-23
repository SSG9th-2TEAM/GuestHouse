package com.ssg9th2team.geharbang.global.service;

/// 이메일 관련 인터페이스
public interface EmailService {


    ///  to = 이메일을 받을 주소 verivicationCode 전송할 인증 코드,
    void sendVerificationEmail(String to, String verificationCode);
}
