-- 목데이터 생성 스크립트
-- 사용자 20명 + 예약 50건 (확정) + 결제 데이터

-- ===== 1. 사용자 20명 생성 =====
-- 비밀번호: 1234 (BCrypt 암호화)

INSERT INTO users (name, nickname, email, password, phone, role, gender, marketing_agree, created_at, updated_at) VALUES
('김철수', 'mockuser01', 'mocktest01@test.com', '$2a$10$abcdefghijklmnopqrstuv', '010-1234-0001', 'USER', 'MALE', 1, NOW(), NOW()),
('이영희', 'mockuser02', 'mocktest02@test.com', '$2a$10$abcdefghijklmnopqrstuv', '010-1234-0002', 'USER', 'FEMALE', 0, NOW(), NOW()),
('박민준', 'mockuser03', 'mocktest03@test.com', '$2a$10$abcdefghijklmnopqrstuv', '010-1234-0003', 'USER', 'MALE', 1, NOW(), NOW()),
('최수연', 'mockuser04', 'mocktest04@test.com', '$2a$10$abcdefghijklmnopqrstuv', '010-1234-0004', 'USER', 'FEMALE', 0, NOW(), NOW()),
('정하늘', 'mockuser05', 'mocktest05@test.com', '$2a$10$abcdefghijklmnopqrstuv', '010-1234-0005', 'USER', 'MALE', 1, NOW(), NOW()),
('강서연', 'mockuser06', 'mocktest06@test.com', '$2a$10$abcdefghijklmnopqrstuv', '010-1234-0006', 'USER', 'FEMALE', 0, NOW(), NOW()),
('조현우', 'mockuser07', 'mocktest07@test.com', '$2a$10$abcdefghijklmnopqrstuv', '010-1234-0007', 'USER', 'MALE', 1, NOW(), NOW()),
('윤지아', 'mockuser08', 'mocktest08@test.com', '$2a$10$abcdefghijklmnopqrstuv', '010-1234-0008', 'USER', 'FEMALE', 0, NOW(), NOW()),
('장도윤', 'mockuser09', 'mocktest09@test.com', '$2a$10$abcdefghijklmnopqrstuv', '010-1234-0009', 'USER', 'MALE', 1, NOW(), NOW()),
('임소율', 'mockuser10', 'mocktest10@test.com', '$2a$10$abcdefghijklmnopqrstuv', '010-1234-0010', 'USER', 'FEMALE', 0, NOW(), NOW()),
('한시우', 'mockuser11', 'mocktest11@test.com', '$2a$10$abcdefghijklmnopqrstuv', '010-1234-0011', 'USER', 'MALE', 1, NOW(), NOW()),
('오예서', 'mockuser12', 'mocktest12@test.com', '$2a$10$abcdefghijklmnopqrstuv', '010-1234-0012', 'USER', 'FEMALE', 0, NOW(), NOW()),
('서준호', 'mockuser13', 'mocktest13@test.com', '$2a$10$abcdefghijklmnopqrstuv', '010-1234-0013', 'USER', 'MALE', 1, NOW(), NOW()),
('신다은', 'mockuser14', 'mocktest14@test.com', '$2a$10$abcdefghijklmnopqrstuv', '010-1234-0014', 'USER', 'FEMALE', 0, NOW(), NOW()),
('권태민', 'mockuser15', 'mocktest15@test.com', '$2a$10$abcdefghijklmnopqrstuv', '010-1234-0015', 'USER', 'MALE', 1, NOW(), NOW()),
('황유나', 'mockuser16', 'mocktest16@test.com', '$2a$10$abcdefghijklmnopqrstuv', '010-1234-0016', 'USER', 'FEMALE', 0, NOW(), NOW()),
('송지호', 'mockuser17', 'mocktest17@test.com', '$2a$10$abcdefghijklmnopqrstuv', '010-1234-0017', 'USER', 'MALE', 1, NOW(), NOW()),
('배하린', 'mockuser18', 'mocktest18@test.com', '$2a$10$abcdefghijklmnopqrstuv', '010-1234-0018', 'USER', 'FEMALE', 0, NOW(), NOW()),
('류성민', 'mockuser19', 'mocktest19@test.com', '$2a$10$abcdefghijklmnopqrstuv', '010-1234-0019', 'USER', 'MALE', 1, NOW(), NOW()),
('구민서', 'mockuser20', 'mocktest20@test.com', '$2a$10$abcdefghijklmnopqrstuv', '010-1234-0020', 'USER', 'FEMALE', 0, NOW(), NOW());

-- 사용자 ID 범위 설정
SET @user_start_id = (SELECT MIN(user_id) FROM users WHERE email LIKE 'mocktest%@test.com');
SET @user_end_id = (SELECT MAX(user_id) FROM users WHERE email LIKE 'mocktest%@test.com');

-- ===== 2. 예약 50건 생성 (확정 상태) =====
INSERT INTO reservation (accommodations_id, room_id, user_id, checkin, checkout, stay_nights, guest_count, reservation_status, total_amount_before_dc, coupon_discount_amount, final_payment_amount, payment_status, reserver_name, reserver_phone, created_at, updated_at)
SELECT 
    r.accommodations_id,
    r.room_id,
    @user_start_id + FLOOR(RAND() * 20),
    DATE_ADD(CURDATE(), INTERVAL FLOOR(RAND() * 30) DAY),
    DATE_ADD(CURDATE(), INTERVAL (FLOOR(RAND() * 30) + FLOOR(RAND() * 3) + 1) DAY),
    FLOOR(RAND() * 3) + 1,
    FLOOR(RAND() * 3) + 1,
    2,
    r.price * (FLOOR(RAND() * 2) + 1),
    0,
    r.price * (FLOOR(RAND() * 2) + 1),
    1,
    '예약자',
    CONCAT('010-1234-00', LPAD(FLOOR(RAND() * 20) + 1, 2, '0')),
    NOW(),
    NOW()
FROM room r
WHERE r.room_status = 1
ORDER BY RAND()
LIMIT 50;

-- ===== 3. 결제 데이터 생성 =====
INSERT INTO payment (reservation_id, pg_provider_code, payment_method, order_id, pg_payment_key, request_amount, approved_amount, currency_code, payment_status, approved_at, created_at, updated_at)
SELECT 
    res.reservation_id,
    'TOSSPAY',
    '카드',
    CONCAT('ORD', res.reservation_id),
    CONCAT('PGK', res.reservation_id),
    res.final_payment_amount,
    res.final_payment_amount,
    'KRW',
    1,
    NOW(),
    NOW(),
    NOW()
FROM reservation res
WHERE res.created_at >= DATE_SUB(NOW(), INTERVAL 1 MINUTE)
  AND res.payment_status = 1;

-- 결과 확인
SELECT '생성된 사용자 수' as info, COUNT(*) as cnt FROM users WHERE email LIKE 'mocktest%@test.com'
UNION ALL
SELECT '생성된 예약 수', COUNT(*) FROM reservation WHERE created_at >= DATE_SUB(NOW(), INTERVAL 5 MINUTE)
UNION ALL
SELECT '생성된 결제 수', COUNT(*) FROM payment WHERE created_at >= DATE_SUB(NOW(), INTERVAL 5 MINUTE);
