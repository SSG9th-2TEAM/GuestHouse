-- 가상 사용자 생성 및 리뷰 연결 스크립트
-- 1. 가상 사용자 100명 생성
-- 2. 기존 리뷰에 user_id 할당
-- 3. user_theme 데이터 추가

-- ===== 1. 가상 리뷰어 사용자 100명 생성 =====
INSERT INTO users (name, nickname, email, password, phone, role, gender, marketing_agree, created_at, updated_at)
SELECT 
    CONCAT('리뷰어', n) as name,
    CONCAT('reviewer', n) as nickname,
    CONCAT('reviewer', n, '@review.com') as email,
    '$2a$10$abcdefghijklmnopqrstuv' as password,
    CONCAT('010-9999-', LPAD(n, 4, '0')) as phone,
    'USER' as role,
    CASE WHEN n % 2 = 0 THEN 'MALE' ELSE 'FEMALE' END as gender,
    CASE WHEN n % 3 = 0 THEN 1 ELSE 0 END as marketing_agree,
    NOW() as created_at,
    NOW() as updated_at
FROM (
    SELECT @row := @row + 1 as n
    FROM (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) t1,
         (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) t2,
         (SELECT @row := 0) r
) numbers
WHERE n <= 100;

-- 가상 사용자 ID 범위 저장
SET @reviewer_start = (SELECT MIN(user_id) FROM users WHERE email LIKE 'reviewer%@review.com');
SET @reviewer_end = (SELECT MAX(user_id) FROM users WHERE email LIKE 'reviewer%@review.com');

-- ===== 2. 기존 리뷰에 user_id 할당 =====
UPDATE review 
SET user_id = @reviewer_start + FLOOR(RAND() * 100)
WHERE is_crawled = 1 OR user_id IS NULL;

-- ===== 3. user_theme 데이터 추가 (각 사용자에게 1~3개 테마 랜덤 할당) =====
-- 먼저 theme_id 목록 확인
SET @theme_count = (SELECT COUNT(*) FROM theme);

-- 랜덤 테마 할당 (사용자당 2개 테마)
INSERT IGNORE INTO user_theme (user_id, theme_id)
SELECT u.user_id, t.theme_id
FROM users u
CROSS JOIN theme t
WHERE u.email LIKE 'reviewer%@review.com'
  AND RAND() < 0.1  -- 약 10% 확률로 매칭 (24개 테마 중 약 2~3개)
ORDER BY RAND();

-- ===== 4. 검증 =====
SELECT '생성된 가상 사용자 수' as info, COUNT(*) as cnt FROM users WHERE email LIKE 'reviewer%@review.com'
UNION ALL
SELECT '리뷰에 user_id 할당 수', COUNT(*) FROM review WHERE user_id IS NOT NULL
UNION ALL
SELECT 'user_theme 연결 수', COUNT(*) FROM user_theme ut JOIN users u ON ut.user_id = u.user_id WHERE u.email LIKE 'reviewer%@review.com';
