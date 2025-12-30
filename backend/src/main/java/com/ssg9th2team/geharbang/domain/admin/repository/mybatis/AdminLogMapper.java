package com.ssg9th2team.geharbang.domain.admin.repository.mybatis;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AdminLogMapper {
    int insertAdminLog(
            @Param("adminId") Long adminId,
            @Param("targetType") String targetType,
            @Param("targetId") Long targetId,
            @Param("actionType") String actionType,
            @Param("reason") String reason
    );
}
