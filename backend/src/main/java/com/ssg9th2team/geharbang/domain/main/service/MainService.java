package com.ssg9th2team.geharbang.domain.main.service;

import com.ssg9th2team.geharbang.domain.main.dto.ListDto;

import java.util.List;

public interface MainService {

    List<ListDto> findByTheme(List<Long> themeIds);
}
