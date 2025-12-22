package com.ssg9th2team.geharbang.domain.payment.service;

import com.ssg9th2team.geharbang.domain.payment.dto.PaymentConfirmRequestDto;
import com.ssg9th2team.geharbang.domain.payment.dto.PaymentResponseDto;

public interface PaymentService {

    /**
     * 결제 승인 요청 (토스페이먼츠 API 호출)
     */
    PaymentResponseDto confirmPayment(PaymentConfirmRequestDto requestDto);

    /**
     * 결제 조회
     */
    PaymentResponseDto getPaymentByOrderId(String orderId);
}
