-- 테스트용 기본 참조 데이터
-- 모든 테스트에서 공통으로 사용

-- account_number 기본 데이터 (외래 키 참조용)
INSERT IGNORE INTO account_number (account_number_id, bank_name, account_number, account_holder)
VALUES (1, '테스트은행', '000-0000-0000', '테스트계좌');

-- users 기본 데이터 (외래 키 참조용)
INSERT IGNORE INTO users (user_id, email, password, phone, role, marketing_agree, created_at, updated_at, host_approved)
VALUES (1, 'test@example.com', 'password123', '010-0000-0000', 'HOST', 0, NOW(), NOW(), 1);

-- 추가 테스트 유저들 (WishlistServiceTest에서 userId 2~7 사용)
INSERT IGNORE INTO users (user_id, email, password, phone, role, marketing_agree, created_at, updated_at, host_approved)
VALUES
(2, 'test2@example.com', 'password123', '010-0000-0002', 'USER', 0, NOW(), NOW(), NULL),
(3, 'test3@example.com', 'password123', '010-0000-0003', 'USER', 0, NOW(), NOW(), NULL),
(4, 'test4@example.com', 'password123', '010-0000-0004', 'USER', 0, NOW(), NOW(), NULL),
(5, 'test5@example.com', 'password123', '010-0000-0005', 'USER', 0, NOW(), NOW(), NULL),
(6, 'test6@example.com', 'password123', '010-0000-0006', 'USER', 0, NOW(), NOW(), NULL),
(7, 'test7@example.com', 'password123', '010-0000-0007', 'USER', 0, NOW(), NOW(), NULL);
