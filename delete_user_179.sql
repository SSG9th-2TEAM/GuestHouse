-- userId 179 완전 삭제 스크립트
-- 실행 방법: MySQL Workbench나 DBeaver에서 실행하거나,
-- 터미널에서: mysql -h 127.0.0.1 -P 13306 -u thismo -pthismo1234 guesthouse < delete_user_179.sql

START TRANSACTION;

SET FOREIGN_KEY_CHECKS = 0;

-- 1. 쿠폰 관련 데이터 삭제
DELETE FROM user_coupon WHERE user_id = 179;
SELECT CONCAT('✓ user_coupons 삭제: ', ROW_COUNT(), '행') AS progress;

-- 2. 예약 관련 데이터 삭제
-- 2-1. 환불 내역 삭제 (payment_refund -> payment -> reservation)
DELETE pr FROM payment_refund pr 
INNER JOIN payment p ON pr.payment_id = p.payment_id 
INNER JOIN reservation r ON p.reservation_id = r.reservation_id 
WHERE r.user_id = 179;
SELECT CONCAT('✓ payment_refund 삭제: ', ROW_COUNT(), '행') AS progress;

-- 2-2. 결제 내역 삭제
DELETE p FROM payment p
INNER JOIN reservation r ON p.reservation_id = r.reservation_id 
WHERE r.user_id = 179;
SELECT CONCAT('✓ payment 삭제: ', ROW_COUNT(), '행') AS progress;

-- 2-3. 예약 삭제
DELETE FROM reservation WHERE user_id = 179;
SELECT CONCAT('✓ reservation 삭제: ', ROW_COUNT(), '행') AS progress;

-- 3. 리뷰 삭제
DELETE FROM review WHERE user_id = 179;
SELECT CONCAT('✓ review 삭제: ', ROW_COUNT(), '행') AS progress;

-- 4. 위시리스트 삭제
DELETE FROM wishlist WHERE user_id = 179;
SELECT CONCAT('✓ wishlist 삭제: ', ROW_COUNT(), '행') AS progress;

-- 5. 사용자 삭제
DELETE FROM users WHERE user_id = 179;
SELECT CONCAT('✓ users 삭제: ', ROW_COUNT(), '행') AS progress;

SET FOREIGN_KEY_CHECKS = 1;

-- 삭제 확인
SELECT '========================================' AS '';
SELECT '✅ userId 179 삭제 완료!' AS result;
SELECT '========================================' AS '';

COMMIT;

-- 삭제 확인 쿼리 (결과가 없어야 정상)
SELECT * FROM users WHERE user_id = 179;
SELECT * FROM reservation WHERE user_id = 179;
SELECT * FROM user_coupon WHERE user_id = 179;


COMMIT;