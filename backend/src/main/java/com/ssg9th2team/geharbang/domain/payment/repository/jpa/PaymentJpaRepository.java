package com.ssg9th2team.geharbang.domain.payment.repository.jpa;

import com.ssg9th2team.geharbang.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentJpaRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(String orderId);

    Optional<Payment> findByReservationId(Long reservationId);

    Optional<Payment> findByPgPaymentKey(String pgPaymentKey);

    List<Payment> findByReservationIdIn(List<Long> reservationIds);

    void deleteByReservationIdIn(List<Long> reservationIds);

    void deleteByReservationId(Long reservationId);
}
