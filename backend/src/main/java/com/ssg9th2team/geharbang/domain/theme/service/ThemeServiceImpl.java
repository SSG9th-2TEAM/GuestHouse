package com.ssg9th2team.geharbang.domain.theme.service;

import com.ssg9th2team.geharbang.domain.theme.entity.Theme;
import com.ssg9th2team.geharbang.domain.theme.repository.ThemeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ThemeServiceImpl implements ThemeService {

    private final ThemeRepository themeRepository;

    @Override
    public List<Theme> findAllThemes() {
        return themeRepository.findAll();
    }
}
