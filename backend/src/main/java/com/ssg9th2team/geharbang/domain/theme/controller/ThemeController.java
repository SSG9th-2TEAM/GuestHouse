package com.ssg9th2team.geharbang.domain.theme.controller;

import com.ssg9th2team.geharbang.domain.theme.dto.ThemeResponseDto;
import com.ssg9th2team.geharbang.domain.theme.service.ThemeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/themes")
@RequiredArgsConstructor
public class ThemeController {

    private final ThemeService themeService;

    @GetMapping
    public List<ThemeResponseDto> findAll() {
        return themeService.findAllThemes()
                .stream()
                .map(ThemeResponseDto::from)
                .collect(Collectors.toList());
    }
}
