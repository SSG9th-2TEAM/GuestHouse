-- 제주 스테이블 게스트하우스 전용 쿠폰 (accommodations_id = 135)
-- 15% 할인, 발급일로부터 30일간 유효
-- 최소 주문 금액 80,000원, 최대 할인 30,000원

-- 기존 쿠폰 삭제 (있다면)
DELETE FROM coupon WHERE code = 'stableCoupon';

-- 제주 스테이블 게스트하우스 전용 쿠폰 (accommodations_id = 135)
INSERT INTO coupon
(code, name, description, discount_type, discount_value, min_price, max_discount, trigger_type, validity_type, validity_days, accommodations_id, is_active, created_at)
VALUES
    ('stableCoupon', '스테이블 게스트하우스 전용 15% 할인', '제주 스테이블 게스트하우스에서만 사용 가능한 특별 쿠폰', 'PERCENT', 15, 80000, 30000, 'DOWNLOAD', 'DAYS_FROM_ISSUE', 30, 135, 1, NOW());



-- 1. DB에서 쿠폰 확인
SELECT coupon_id, code, name, trigger_type, is_active, accommodations_id
FROM coupon
WHERE code = 'stableCoupon';

-- 2. 해당 숙소 ID 확인
SELECT accommodations_id, accommodations_name
FROM accommodation
WHERE accommodations_id = 135;
