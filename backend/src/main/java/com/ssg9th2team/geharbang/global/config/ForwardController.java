package com.ssg9th2team.geharbang.global.config;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class ForwardController implements ErrorController {

    // SPA 라우팅: 정적 리소스나 API가 아닌 모든 경로를 index.html로 포워딩
    @GetMapping(value = "/**")
    public String forward(HttpServletRequest request) {
        String uri = request.getRequestURI();

        // 제외할 경로들 (정적 리소스, API 등)
        if (uri.startsWith("/api/") ||
                uri.startsWith("/assets/") ||
                uri.startsWith("/uploads/") ||
                uri.startsWith("/swagger-ui/") ||
                uri.startsWith("/v3/") ||
                uri.startsWith("/oauth2/") ||
                uri.startsWith("/login/oauth2/") ||
                uri.equals("/favicon.ico") ||
                uri.equals("/logo.png") ||
                uri.equals("/icon.png") ||
                uri.equals("/index.html") ||
                uri.contains(".")) { // 확장자가 있는 파일
            return null; // Spring 기본 처리
        }

        return "forward:/index.html";
    }

    @GetMapping("/error")
    public String handleError() {
        return "forward:/index.html";
    }
}
