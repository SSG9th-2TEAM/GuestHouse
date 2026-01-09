# 쿠폰 발급 트러블슈팅 (DB 락 → Redis 전환)

## 문제 사례 1: 선착순 & 동시 클릭 시 중복 발급

- 초기 구현은 **DB(JPA) 트랜잭션 + 비관적 락** 기반이었고, `CouponInventoryService.consumeSlotIfLimited()`가 재고 차감을 담당했습니다.
- 문제가 된 시나리오
  1. 네트워크 지연 중 버튼 연타 → 동시에 여러 INSERT 요청 발생.
  2. 발급 조건이 충족되는 순간 다수의 사용자가 동시에 요청.
  3. 선착순 쿠폰/대규모 이벤트에서 초당 수백 건 이상 트래픽.
- 결과: PESSIMISTIC_WRITE 락 대기에 따른 지연, DB 부하 증가, 발급 API 응답이 느려져 UX 저하.

## 1차 해결 – DB 중심 제어 고도화

- 트랜잭션 범위 안에서 `UserCoupon` 중복 여부와 만료 상태를 철저히 검증.
- `CouponInventory.resetIfNeeded()`로 일자별 재고 리셋을 수행하고, `CouponInventoryRepository.resetAllInventories()` 배치 업데이트를 도입해 자정 스케줄러 부하를 줄임.
- 프론트에서는 `getMyCouponIds()` 경량 API로 이미 발급된 쿠폰을 체크하고, 선착순 타이머 갱신 시 `setInterval`을 종료해 API 폭주를 방지함.

이 단계에서 **중복 발급/만료 문제**는 안정화되었지만, 여전히 모든 요청이 DB Lock 경합을 일으키기 때문에 대규모 트래픽 환경에서는 확장성 한계가 존재했습니다.

## 2차 개선 방안 – Redis 기반 구조 설계

향후 선착순/이벤트 트래픽을 위해 다음 구조로 전환할 계획입니다.

1. **Redis Set/HyperLogLog**로 사용자별 발급 여부를 즉시 판단 → DB 조회 없이 중복 방지.
2. **Redis Atomic Counter (INCR/DECR)**로 남은 수량을 관리 → 초당 발급량을 Lock 없이 제어.
3. **Lua Script 혹은 Redis Transactions**로 “중복 체크 + 재고 차감”을 하나의 원자 연산으로 묶어 경쟁 조건 제거.
4. 발급 성공 시 비동기 Queue(Kafka/SQS 등)로 UserCoupon INSERT를 처리 → DB를 최종 기록 저장소로 유지.
5. TTL을 활용해 쿠폰 기간/자정 리셋을 자동화, 자정 배치 없이도 재고 초기화 가능.

## 적용 현황 체크리스트

- [x] `CouponInventoryService` 메서드별 주석으로 Lock 방식과 리셋 흐름 문서화.
- [x] `CouponController#getMyCouponIds` API로 프론트 발급 상태 확인 최적화.
- [x] `EventView` 자정 이후 `clearInterval` 처리로 API 폭주 차단.
- [ ] Redis 기반 발급 파이프라인 설계/구현.
- [ ] Kafka/비동기 큐 도입 여부 결정.

## 레퍼런스
- `backend/src/main/java/.../CouponInventoryService.java`
- `frontend/src/views/home/EventView.vue`
- `docs/ai-auto-naming.md` (이미지 AI 추천 가이드)

Redis 전환 이후에는 DB는 최종 정합성만 담당하고, 실시간 발급은 캐시 계층에서 처리하여 성능과 안정성을 동시에 확보할 예정입니다.
