package com.ssg9th2team.geharbang.domain.main.repository;

import com.ssg9th2team.geharbang.domain.main.dto.ListDto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MainRepository extends JpaRepository<ListDto, Long> {
    List<ListDto> findAllByTheme(List<Integer> themeIds);
}
