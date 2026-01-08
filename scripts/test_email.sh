#!/bin/bash
# 이메일 알림 테스트 스크립트
# 사용법: ./test_email.sh [JWT_TOKEN] [RECEIVER_EMAIL]

# 설정
API_BASE="http://10.0.2.6:8080/api"
JWT_TOKEN="${1:-YOUR_JWT_TOKEN}"
EMAIL="${2:-test@example.com}"
ROOM_ID=1
ACCOMMODATION_ID=1
CHECKIN="2026-02-01T15:00:00Z"
CHECKOUT="2026-02-02T11:00:00Z"

echo "=========================================="
echo "이메일 알림 테스트 시작"
echo "수신자: $EMAIL"
echo "=========================================="

# 1. 대기 목록 등록
echo "[1] 대기 목록 등록 중..."
curl -s -X POST "$API_BASE/waitlist" \
  -H "Content-Type: application/json" \
  -H "Authorization: $JWT_TOKEN" \
  -d "{
    \"roomId\": $ROOM_ID,
    \"accommodationId\": $ACCOMMODATION_ID,
    \"checkin\": \"$CHECKIN\",
    \"checkout\": \"$CHECKOUT\",
    \"guestCount\": 1
  }"
echo " 완료"

# 2. 강제로 알림 트리거 (스케줄러 동작 시뮬레이션 힘듦)
# 대신 간단한 API를 하나 만들거나, 기존 로직을 타게 해야 함
# 여기서는 '미결제 예약 삭제' 로직을 타야 하는데, 실제 미결제 예약이 있어야 함.

echo ""
echo "[2] 테스트를 위한 가짜 미결제 예약 생성..."
# 룸 ID 1에 미결제 예약 생성 (DB에 직접 넣거나 API 사용)
# 여기서는 예약 API 사용 -> 결제 안 함 -> 스케줄러가 돌아야 함
curl -s -X POST "$API_BASE/reservations" \
  -H "Content-Type: application/json" \
  -H "Authorization: $JWT_TOKEN" \
  -d "{
    \"accommodationsId\": $ACCOMMODATION_ID,
    \"roomId\": $ROOM_ID,
    \"checkin\": \"$CHECKIN\",
    \"checkout\": \"$CHECKOUT\",
    \"guestCount\": 1,
    \"totalAmount\": 10000,
    \"reserverName\": \"테스트\",
    \"reserverPhone\": \"010-1234-5678\"
  }" > /dev/null
echo " 완료"

echo ""
echo "=========================================="
echo "이제 서버에서 스케줄러가 실행되기를 기다리거나,"
echo "수동으로 cleanup 메소드를 호출해야 합니다."
echo "서버 로그를 확인하세요: tail -f logs/application.log"
echo "=========================================="
