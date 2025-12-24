package com.ssg9th2team.geharbang.domain.payment.repository.jpa;

import com.ssg9th2team.geharbang.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PaymentJpaRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {

    Optional<Payment> findByOrderId(String orderId);

    Optional<Payment> findByReservationId(Long reservationId);

    Optional<Payment> findByPgPaymentKey(String pgPaymentKey);

    long countByPaymentStatusAndCreatedAtBetween(Integer paymentStatus, LocalDateTime start, LocalDateTime end);

    @Query("select coalesce(sum(p.approvedAmount), 0) from Payment p " +
            "where p.paymentStatus = 1 and p.createdAt >= :start and p.createdAt < :end")
    Long sumApprovedAmount(LocalDateTime start, LocalDateTime end);
}
