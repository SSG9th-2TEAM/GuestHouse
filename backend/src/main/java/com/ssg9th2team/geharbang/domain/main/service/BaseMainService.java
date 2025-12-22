package com.ssg9th2team.geharbang.domain.main.service;

import com.ssg9th2team.geharbang.domain.main.dto.ListDto;

import java.util.List;

public class BaseMainService implements MainService {
    @Override
    public List<ListDto> findByTheme() {
        return List.of();
    }
}
