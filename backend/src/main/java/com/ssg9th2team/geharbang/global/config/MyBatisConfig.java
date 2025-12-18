package com.ssg9th2team.geharbang.global.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan(
        basePackages = "com.ssg9th2team.geharbang.domain.**.repository.mybatis"
)
public class MyBatisConfig {
}
