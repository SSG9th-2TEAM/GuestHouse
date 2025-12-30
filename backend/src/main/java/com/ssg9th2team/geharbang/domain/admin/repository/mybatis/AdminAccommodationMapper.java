package com.ssg9th2team.geharbang.domain.admin.repository.mybatis;

import com.ssg9th2team.geharbang.domain.admin.dto.AdminAccommodationMetrics;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AdminAccommodationMapper {
    List<AdminAccommodationMetrics> selectAccommodationMetrics(@Param("ids") List<Long> ids);
}
