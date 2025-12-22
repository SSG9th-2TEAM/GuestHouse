-- 테스트 데이터 초기화 스크립트
-- 1. 예약 내역 삭제
DELETE FROM reservation;

-- 2. Room의 maxGuests 복구 (예: 10명으로 설정)
UPDATE room SET max_guests = 10 WHERE room_id = 1;

-- 확인
SELECT room_id, room_name, max_guests FROM room WHERE room_id = 1;
