package com.ssg9th2team.geharbang.domain.main.service;

import com.ssg9th2team.geharbang.domain.main.dto.MainAccommodationListResponse;
import com.ssg9th2team.geharbang.domain.main.dto.PublicListResponse;

import java.util.List;

public interface MainService {

    MainAccommodationListResponse getMainAccommodationList(Long userId, List<Long> filterThemeIds, String keyword);

    PublicListResponse searchPublicList(List<Long> themeIds, String keyword, int page, int size);
}
