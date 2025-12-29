package com.ssg9th2team.geharbang.global.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/// 이메일 서비스 관련 기능
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender javaMailSender;


    /// 사용자에게 이메일 인증 코드 전송
    @Override
    public void sendVerificationEmail(String to, String verificationCode) {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject("게스트하우스 플랫폼 회원가입 이메일 인증 코드");

            // 이메일 내용 (HTML 형식)
            String htmlContent = "<html>" +
                                 "<body style='font-family: Arial, sans-serif;'>" +
                                 "<h2 style='color: #004d40;'>이메일 인증 코드 안내</h2>" +
                                 "<p>안녕하세요!</p>" +
                                 "<p><지금 이곳> 플랫폼 회원가입을 위한 인증 코드를 안내해 드립니다.</p>" +
                                 "<p style='font-size: 24px; font-weight: bold; color: #00796b;'>" + verificationCode + "</p>" +
                                 "<p>위 코드를 회원가입 페이지에 입력하여 이메일 인증을 완료해 주세요.</p>" +
                                 "<p>본 코드는 발송 시점으로부터 3분간 유효합니다.</p>" +
                                 "<p>감사합니다.</p>" +
                                 "</body>" +
                                 "</html>";
            helper.setText(htmlContent, true); // true 설정 시 HTML 형식으로 전송

            javaMailSender.send(mimeMessage);
            log.info("인증 이메일 전송 완료: {} (코드: {})", to, verificationCode);
        } catch (MessagingException e) {
            log.error("인증 이메일 전송 실패: {} (에러: {})", to, e.getMessage());
            throw new RuntimeException("이메일 전송에 실패했습니다.", e);
        }
    }
}
