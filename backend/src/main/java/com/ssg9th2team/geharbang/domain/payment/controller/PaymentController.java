package com.ssg9th2team.geharbang.domain.payment.controller;

import com.ssg9th2team.geharbang.domain.payment.dto.PaymentConfirmRequestDto;
import com.ssg9th2team.geharbang.domain.payment.dto.PaymentResponseDto;
import com.ssg9th2team.geharbang.domain.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 결제 승인
     */
    @PostMapping("/confirm")
    public ResponseEntity<PaymentResponseDto> confirmPayment(
            @RequestBody PaymentConfirmRequestDto requestDto) {
        PaymentResponseDto response = paymentService.confirmPayment(requestDto);
        return ResponseEntity.ok(response);
    }

    /**
     * 주문번호로 결제 조회
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponseDto> getPaymentByOrderId(
            @PathVariable String orderId) {
        PaymentResponseDto response = paymentService.getPaymentByOrderId(orderId);
        return ResponseEntity.ok(response);
    }

    /**
     * 토스페이먼츠 클라이언트 키 조회 (프론트엔드용)
     */
    @GetMapping("/client-key")
    public ResponseEntity<String> getClientKey() {
        // 실제로는 환경변수에서 가져오는게 좋음
        return ResponseEntity.ok("test_ck_D5GePWvyJnrK0W0k6q8gLzN97Eoq");
    }
}
