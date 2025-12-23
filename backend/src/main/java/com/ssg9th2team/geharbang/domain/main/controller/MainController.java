package com.ssg9th2team.geharbang.domain.main.controller;

import com.ssg9th2team.geharbang.domain.main.dto.ListDto;
import com.ssg9th2team.geharbang.domain.main.service.MainService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class MainController {

    private final MainService mainService;

    @GetMapping("/list")
    public List<ListDto> list(@RequestParam(name = "themeIds", required = false) List<Long> themeIds) {
        return mainService.findByTheme(themeIds);
    }
}
