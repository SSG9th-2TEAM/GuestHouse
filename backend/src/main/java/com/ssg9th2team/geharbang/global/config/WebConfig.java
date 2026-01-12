package com.ssg9th2team.geharbang.global.config;

import com.ssg9th2team.geharbang.domain.admin.support.AdminIdArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AdminIdArgumentResolver adminIdArgumentResolver;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        "http://localhost:3000",      // 로컬 프론트엔드 (React 기본)
                        "http://localhost:5173",      // 로컬 프론트엔드 (Vite 기본)
                        "http://localhost:8080",      // 로컬 백엔드
                        "http://127.0.0.1:5173",
                        "http://127.0.0.1:8080",
                        // ▼▼▼ [여기부터 추가된 부분] ▼▼▼
                        "http://49.50.138.206",       // 배포된 프론트엔드 (포트 80)
                        "http://49.50.138.206:8081",  // 관리자 서버 자기 자신
                        "http://49.50.138.206:3000",  // 배포된 프론트엔드 (혹시 3000번 쓴다면)
                        "http://49.50.138.206:5173"   // 배포된 프론트엔드 (혹시 5173번 쓴다면)
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowCredentials(true); // 쿠키/인증정보 포함 허용
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(adminIdArgumentResolver);
    }
}