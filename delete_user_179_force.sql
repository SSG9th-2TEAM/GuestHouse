-- userId 179 강제 삭제 (Auto-commit)
-- 각 DELETE 문이 즉시 커밋됩니다.

SET autocommit = 1;
SET FOREIGN_KEY_CHECKS = 0;

-- 1. 쿠폰 삭제
DELETE FROM user_coupon WHERE user_id = 179;

-- 2. 환불 내역 삭제
DELETE pr FROM payment_refund pr 
INNER JOIN payment p ON pr.payment_id = p.payment_id 
INNER JOIN reservation r ON p.reservation_id = r.reservation_id 
WHERE r.user_id = 179;

-- 3. 결제 내역 삭제
DELETE p FROM payment p 
INNER JOIN reservation r ON p.reservation_id = r.reservation_id 
WHERE r.user_id = 179;

-- 4. 예약 삭제
DELETE FROM reservation WHERE user_id = 179;

-- 5. 리뷰 삭제
DELETE FROM review WHERE user_id = 179;

-- 6. 위시리스트 삭제
DELETE FROM wishlist WHERE user_id = 179;

-- 7. 사용자 삭제
DELETE FROM users WHERE user_id = 179;

SET FOREIGN_KEY_CHECKS = 1;

-- 삭제 확인 (0 rows가 나와야 정상)
SELECT '=== 삭제 확인 (0 rows가 나와야 정상) ===' AS '';
SELECT COUNT(*) AS user_count FROM users WHERE user_id = 179;
SELECT COUNT(*) AS reservation_count FROM reservation WHERE user_id = 179;
SELECT COUNT(*) AS coupon_count FROM user_coupon WHERE user_id = 179;
