package com.ssg9th2team.geharbang;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan(basePackages = "com.ssg9th2team.geharbang.domain")
public class GeharbangApplication {

	public static void main(String[] args) {
		SpringApplication.run(GeharbangApplication.class, args);
	}

}
