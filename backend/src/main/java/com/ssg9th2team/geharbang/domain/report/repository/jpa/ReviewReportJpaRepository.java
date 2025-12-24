package com.ssg9th2team.geharbang.domain.report.repository.jpa;

import com.ssg9th2team.geharbang.domain.report.entity.ReviewReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewReportJpaRepository extends JpaRepository<ReviewReport, Long>, JpaSpecificationExecutor<ReviewReport> {
}
