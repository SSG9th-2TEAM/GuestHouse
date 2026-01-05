package com.ssg9th2team.geharbang.domain.payment.repository.jpa;

import com.ssg9th2team.geharbang.domain.payment.entity.PaymentRefund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRefundJpaRepository extends JpaRepository<PaymentRefund, Long> {

    List<PaymentRefund> findByPaymentId(Long paymentId);

    Optional<PaymentRefund> findByPgRefundKey(String pgRefundKey);

    void deleteByPaymentIdIn(List<Long> paymentIds);

    void deleteByPaymentId(Long paymentId);
}
