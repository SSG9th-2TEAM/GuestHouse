CREATE TABLE IF NOT EXISTS coupon
(
    coupon_id      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '쿠폰 PK',
    code           VARCHAR(50)     NOT NULL COMMENT '쿠폰 식별 코드 (UNIQUE)',
    name           VARCHAR(100)    NOT NULL COMMENT '쿠폰 이름',
    description    VARCHAR(255)    NULL COMMENT '쿠폰 설명',
    discount_type  VARCHAR(20)     NOT NULL COMMENT 'AMOUNT / PERCENT',
    discount_value INT             NOT NULL COMMENT '할인 금액 또는 할인율',
    min_price      INT             NOT NULL COMMENT '최소 적용 금액',
    max_discount   INT             NULL COMMENT '정율 할인 상한',
    valid_from     DATETIME        NOT NULL COMMENT '시작 일시',
    valid_to       DATETIME        NOT NULL COMMENT '종료 일시',
    is_active      TINYINT         NOT NULL DEFAULT 1 COMMENT '사용 가능 여부 (0/1)',
    created_at     DATETIME        NOT NULL COMMENT '생성 시각',
    CONSTRAINT PK_COUPON PRIMARY KEY (coupon_id),
    CONSTRAINT UQ_COUPON_CODE UNIQUE (code)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4;