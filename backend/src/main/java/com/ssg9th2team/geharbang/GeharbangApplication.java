package com.ssg9th2team.geharbang;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GeharbangApplication {

	public static void main(String[] args) {
		SpringApplication.run(GeharbangApplication.class, args);
	}

}
