package com.ssg9th2team.geharbang.domain.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssg9th2team.geharbang.domain.payment.dto.PaymentConfirmRequestDto;
import com.ssg9th2team.geharbang.domain.payment.dto.PaymentResponseDto;
import com.ssg9th2team.geharbang.domain.payment.entity.Payment;
import com.ssg9th2team.geharbang.domain.payment.entity.PaymentRefund;
import com.ssg9th2team.geharbang.domain.payment.repository.jpa.PaymentJpaRepository;
import com.ssg9th2team.geharbang.domain.payment.repository.jpa.PaymentRefundJpaRepository;
import com.ssg9th2team.geharbang.domain.reservation.entity.Reservation;
import com.ssg9th2team.geharbang.domain.reservation.repository.jpa.ReservationJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PaymentServiceImpl implements PaymentService {

    private final PaymentJpaRepository paymentRepository;
    private final PaymentRefundJpaRepository paymentRefundRepository;
    private final ReservationJpaRepository reservationRepository;
    private final ObjectMapper objectMapper;

    @Value("${tosspayments.secret-key}")
    private String secretKey;

    private static final String TOSS_CONFIRM_URL = "https://api.tosspayments.com/v1/payments/confirm";

    @Override
    @Transactional
    public PaymentResponseDto confirmPayment(PaymentConfirmRequestDto requestDto) {
        log.info("결제 승인 요청: orderId={}, amount={}", requestDto.orderId(), requestDto.amount());

        // orderId에서 reservationId 추출 (형식: ORDER_예약ID_타임스탬프)
        Long reservationId = extractReservationId(requestDto.orderId());

        // 예약 조회 및 검증
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다: " + reservationId));

        // 금액 검증
        if (!reservation.getFinalPaymentAmount().equals(requestDto.amount())) {
            throw new IllegalArgumentException("결제 금액이 일치하지 않습니다");
        }

        // 토스페이먼츠 결제 승인 API 호출
        try {
            RestTemplate restTemplate = new RestTemplate();

            // UTF-8 인코딩 설정
            restTemplate.getMessageConverters()
                    .add(0, new org.springframework.http.converter.StringHttpMessageConverter(StandardCharsets.UTF_8));

            // Basic 인증 헤더 생성
            String encodedSecretKey = Base64.getEncoder()
                    .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));
            headers.setAcceptCharset(java.util.Collections.singletonList(StandardCharsets.UTF_8));
            headers.set("Authorization", "Basic " + encodedSecretKey);

            // 요청 바디
            Map<String, Object> body = new HashMap<>();
            body.put("paymentKey", requestDto.paymentKey());
            body.put("orderId", requestDto.orderId());
            body.put("amount", requestDto.amount());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    TOSS_CONFIRM_URL,
                    HttpMethod.POST,
                    entity,
                    String.class);

            log.info("토스페이먼츠 응답: status={}", response.getStatusCode());

            // 응답 파싱
            JsonNode responseJson = objectMapper.readTree(response.getBody());
            String paymentMethod = responseJson.has("method") ? responseJson.get("method").asText() : "CARD";
            String approvedAtStr = responseJson.has("approvedAt") ? responseJson.get("approvedAt").asText() : null;
            LocalDateTime approvedAt = approvedAtStr != null ? LocalDateTime.parse(approvedAtStr.substring(0, 19))
                    : LocalDateTime.now();

            // Payment 저장
            Payment payment = Payment.builder()
                    .reservationId(reservationId)
                    .pgProviderCode("TOSS")
                    .paymentMethod(paymentMethod)
                    .orderId(requestDto.orderId())
                    .pgPaymentKey(requestDto.paymentKey())
                    .requestAmount(requestDto.amount())
                    .approvedAmount(requestDto.amount())
                    .paymentStatus(1) // 성공
                    .approvedAt(approvedAt)
                    .build();

            Payment savedPayment = paymentRepository.save(payment);

            // 예약 상태 업데이트 (예약확정, 결제완료)
            reservation.updatePaymentCompleted();
            reservationRepository.save(reservation);

            log.info("결제 승인 완료: paymentId={}, reservationId={}", savedPayment.getId(), reservationId);

            return PaymentResponseDto.from(savedPayment);

        } catch (Exception e) {
            log.error("결제 승인 실패", e);

            // 실패 Payment 저장
            Payment failedPayment = Payment.builder()
                    .reservationId(reservationId)
                    .pgProviderCode("TOSS")
                    .paymentMethod("UNKNOWN")
                    .orderId(requestDto.orderId())
                    .requestAmount(requestDto.amount())
                    .paymentStatus(2) // 실패
                    .failureMessage(e.getMessage())
                    .build();

            paymentRepository.save(failedPayment);

            // 예약 결제 상태를 실패로 업데이트
            reservation.updatePaymentFailed();
            reservationRepository.save(reservation);

            throw new RuntimeException("결제 승인에 실패했습니다: " + e.getMessage());
        }
    }

    @Override
    public PaymentResponseDto getPaymentByOrderId(String orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("결제 정보를 찾을 수 없습니다: " + orderId));
        return PaymentResponseDto.from(payment);
    }

    @Override
    public PaymentResponseDto getPaymentByReservationId(Long reservationId) {
        Payment payment = paymentRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("결제 정보를 찾을 수 없습니다: reservationId=" + reservationId));
        return PaymentResponseDto.from(payment);
    }

    @Override
    @Transactional
    public PaymentResponseDto cancelPayment(Long reservationId, String cancelReason, Integer refundAmount) {
        log.info("결제 취소 요청: reservationId={}, refundAmount={}", reservationId, refundAmount);

        // 결제 정보 조회
        Payment payment = paymentRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("결제 정보를 찾을 수 없습니다: reservationId=" + reservationId));

        // 예약 조회
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다: " + reservationId));

        // 환불 금액 결정
        Integer actualRefundAmount = (refundAmount != null) ? refundAmount : payment.getApprovedAmount();

        // 환불 기록 생성 (요청 상태)
        PaymentRefund paymentRefund = PaymentRefund.builder()
                .paymentId(payment.getId())
                .refundAmount(actualRefundAmount)
                .refundStatus(0) // 0: 요청
                .reasonMessage(cancelReason)
                .requestedBy("USER")
                .build();

        // 토스페이먼츠 결제 취소 API 호출
        try {
            RestTemplate restTemplate = new RestTemplate();
            restTemplate.getMessageConverters()
                    .add(0, new org.springframework.http.converter.StringHttpMessageConverter(StandardCharsets.UTF_8));

            String encodedSecretKey = Base64.getEncoder()
                    .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));
            headers.set("Authorization", "Basic " + encodedSecretKey);

            Map<String, Object> body = new HashMap<>();
            body.put("cancelReason", cancelReason);
            if (refundAmount != null && refundAmount < payment.getApprovedAmount()) {
                body.put("cancelAmount", refundAmount); // 부분 취소
            }

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            String cancelUrl = "https://api.tosspayments.com/v1/payments/" + payment.getPgPaymentKey() + "/cancel";

            ResponseEntity<String> response = restTemplate.exchange(
                    cancelUrl,
                    HttpMethod.POST,
                    entity,
                    String.class);

            log.info("토스페이먼츠 취소 응답: status={}", response.getStatusCode());

            // 환불 성공 - payment_refund 테이블 업데이트
            paymentRefund.updateRefundSuccess(payment.getPgPaymentKey(), LocalDateTime.now());
            paymentRefundRepository.save(paymentRefund);

            // 예약 상태 업데이트 (취소/환불)
            reservation.updateRefunded();
            reservationRepository.save(reservation);

            log.info("환불 처리 완료: reservationId={}, refundAmount={}", reservationId, actualRefundAmount);

            return PaymentResponseDto.from(payment);

        } catch (Exception e) {
            log.error("결제 취소 실패", e);

            // 환불 실패 - payment_refund 테이블 업데이트
            paymentRefund.updateRefundFailed("CANCEL_FAILED", e.getMessage());
            paymentRefundRepository.save(paymentRefund);

            throw new RuntimeException("환불 처리에 실패했습니다: " + e.getMessage());
        }
    }

    private Long extractReservationId(String orderId) {
        // 형식: ORDER_예약ID_타임스탬프
        try {
            String[] parts = orderId.split("_");
            if (parts.length >= 2) {
                return Long.parseLong(parts[1]);
            }
            throw new IllegalArgumentException("잘못된 주문번호 형식입니다: " + orderId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("주문번호에서 예약 ID를 추출할 수 없습니다: " + orderId);
        }
    }
}
