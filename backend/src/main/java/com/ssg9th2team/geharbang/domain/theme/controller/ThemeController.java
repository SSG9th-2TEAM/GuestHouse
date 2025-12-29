package com.ssg9th2team.geharbang.domain.theme.controller;

import com.ssg9th2team.geharbang.domain.theme.dto.ThemeResponseDto;
import com.ssg9th2team.geharbang.domain.theme.entity.Theme;
import com.ssg9th2team.geharbang.domain.theme.service.ThemeService;
import com.ssg9th2team.geharbang.domain.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/themes")
@RequiredArgsConstructor
public class ThemeController {

    private final ThemeService themeService;
    private final UserRepository userRepository;

    @GetMapping
    public List<ThemeResponseDto> findAll() {
        return themeService.findAllThemes()
                .stream()
                .map(ThemeResponseDto::from)
                .collect(Collectors.toList());
    }

    @GetMapping("/me")
    @Transactional(readOnly = true)
    public List<ThemeResponseDto> findMyThemes(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"))
                .getThemes()
                .stream()
                .sorted(Comparator.comparing(Theme::getId))
                .limit(3)
                .map(ThemeResponseDto::from)
                .collect(Collectors.toList());
    }
}
