package com.ssg9th2team.geharbang.domain.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssg9th2team.geharbang.domain.payment.dto.PaymentConfirmRequestDto;
import com.ssg9th2team.geharbang.domain.payment.dto.PaymentConfirmResponseDto;
import com.ssg9th2team.geharbang.domain.payment.dto.PaymentResponseDto;
import com.ssg9th2team.geharbang.domain.payment.dto.RefundPolicyResult;
import com.ssg9th2team.geharbang.domain.payment.entity.Payment;
import com.ssg9th2team.geharbang.domain.payment.entity.PaymentRefund;
import com.ssg9th2team.geharbang.domain.payment.repository.jpa.PaymentJpaRepository;
import com.ssg9th2team.geharbang.domain.payment.repository.jpa.PaymentRefundJpaRepository;
import com.ssg9th2team.geharbang.domain.reservation.entity.Reservation;
import com.ssg9th2team.geharbang.domain.reservation.repository.jpa.ReservationJpaRepository;
import com.ssg9th2team.geharbang.domain.reservation.service.WaitlistService;...

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PaymentServiceImpl implements PaymentService {

    private final PaymentJpaRepository paymentRepository;
    private final PaymentRefundJpaRepository paymentRefundRepository;
    private final RefundPolicyService refundPolicyService;
    private final ReservationJpaRepository reservationRepository;
    private final UserCouponService userCouponService;
    private final ObjectMapper objectMapper;
    private final WaitlistService waitlistService;
...
    // 공통 환불 처리 로직
    private PaymentResponseDto processRefund(Payment payment, Reservation reservation, Integer actualRefundAmount, String reason, Integer approvedAmount) {
        // 환불 기록 생성 (요청 상태)
        PaymentRefund paymentRefund = PaymentRefund.builder()
                .paymentId(payment.getId())
                .refundAmount(actualRefundAmount)
                .refundStatus(0) // 0: 요청
                .reasonMessage(reason)
                .requestedBy("SYSTEM") // or ADMIN/USER
                .build();

        if (actualRefundAmount == 0) {
            paymentRefund.updateRefundSuccess(payment.getPgPaymentKey(), LocalDateTime.now());
            paymentRefundRepository.save(paymentRefund);
            reservation.updateRefunded();
            reservationRepository.save(reservation);
            
            // 환불 금액이 0이어도 쿠폰은 복구
            if (reservation.getUserCouponId() != null) {
                userCouponService.restoreCoupon(reservation.getUserId(), reservation.getUserCouponId());
                log.info("쿠폰 복구 완료 (환불 불가 정책/0원 환불): userCouponId={}", reservation.getUserCouponId());
            }
            
            log.info("0원 환불 처리 완료: reservationId={}", reservation.getId());
        } else {
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
                body.put("cancelReason", reason);
                if (actualRefundAmount < approvedAmount) {
                    body.put("cancelAmount", actualRefundAmount); // 부분 취소
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

                // 쿠폰 복구 처리
                if (reservation.getUserCouponId() != null) {
                    userCouponService.restoreCoupon(reservation.getUserId(), reservation.getUserCouponId());
                    log.info("쿠폰 복구 완료: userCouponId={}", reservation.getUserCouponId());
                }

                log.info("환불 처리 완료: reservationId={}, refundAmount={}", reservation.getId(), actualRefundAmount);

            } catch (Exception e) {
                log.error("결제 취소 실패", e);

                // 환불 실패 - payment_refund 테이블 업데이트
                paymentRefund.updateRefundFailed("CANCEL_FAILED", e.getMessage());
                paymentRefundRepository.save(paymentRefund);

                throw new RuntimeException("환불 처리에 실패했습니다: " + e.getMessage());
            }
        }
        
        // 첫 예약 쿠폰 회수 (예약 취소로 인해 첫 예약이 아니게 됨) - 공통 처리
        userCouponService.revokeFirstReservationCoupon(reservation.getUserId());
        
        // 대기자에게 알림 발송 (빈자리 발생)
        try {
            waitlistService.notifyWaitingUsers(reservation.getRoomId(), reservation.getCheckin(), reservation.getCheckout());
            log.info("취소/환불로 인한 대기자 알림 발송 요청 완료: roomId={}", reservation.getRoomId());
        } catch (Exception e) {
            log.warn("대기자 알림 발송 중 오류 (결제 취소는 성공): {}", e.getMessage());
        }

        return PaymentResponseDto.from(payment);
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

    @Override
    public java.util.List<com.ssg9th2team.geharbang.domain.payment.dto.RefundResponseDto> getAllRefunds() {
        return paymentRefundRepository.findAll().stream()
                .map(com.ssg9th2team.geharbang.domain.payment.dto.RefundResponseDto::from)
                .toList();
    }

    @Override
    @Transactional
    public void deleteAllPaymentDataByReservationId(Long reservationId) {
        log.info("예약 ID로 결제 정보 삭제 시도: {}", reservationId);
        // 결제 정보 조회 (없을 수도 있음 - 대기 상태에서 취소 등)
        paymentRepository.findByReservationId(reservationId).ifPresent(payment -> {
            // 환불 내역이 있으면 먼저 삭제
            paymentRefundRepository.deleteByPaymentId(payment.getId());
            // 결제 내역 삭제 (reservationId로 삭제)
            // paymentRepository.delete(payment); 로 해도 되지만, 명시적으로 deleteByReservationId 사용
            paymentRepository.deleteByReservationId(reservationId);
            log.info("결제 정보 및 환불 내역 삭제 완료: paymentId={}", payment.getId());
        });
    }
}
