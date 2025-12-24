package com.ssg9th2team.geharbang.domain.payment.repository.jpa;

import com.ssg9th2team.geharbang.domain.payment.entity.PaymentRefund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRefundJpaRepository extends JpaRepository<PaymentRefund, Long> {

    List<PaymentRefund> findByPaymentId(Long paymentId);

    Optional<PaymentRefund> findByPgRefundKey(String pgRefundKey);

    long countByRefundStatusAndCreatedAtBetween(Integer refundStatus, LocalDateTime start, LocalDateTime end);

    @Query("select coalesce(sum(r.refundAmount), 0) from PaymentRefund r " +
            "where r.refundStatus = 1 and r.createdAt >= :start and r.createdAt < :end")
    Long sumRefundAmount(LocalDateTime start, LocalDateTime end);
}
