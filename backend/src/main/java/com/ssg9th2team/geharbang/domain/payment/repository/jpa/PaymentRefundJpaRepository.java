package com.ssg9th2team.geharbang.domain.payment.repository.jpa;

import com.ssg9th2team.geharbang.domain.payment.entity.PaymentRefund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRefundJpaRepository extends JpaRepository<PaymentRefund, Long> {

    List<PaymentRefund> findByPaymentId(Long paymentId);

    Optional<PaymentRefund> findByPgRefundKey(String pgRefundKey);

    @Modifying
    void deleteByPaymentIdIn(List<Long> paymentIds);

    void deleteByPaymentId(Long paymentId);

    List<PaymentRefund> findByRequestedAtBetween(LocalDateTime start, LocalDateTime end);

    List<PaymentRefund> findByPaymentIdIn(List<Long> paymentIds);
}
