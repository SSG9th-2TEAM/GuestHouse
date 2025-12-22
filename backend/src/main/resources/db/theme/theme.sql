CREATE TABLE IF NOT EXISTS theme
(
    theme_id       BIGINT       NOT NULL AUTO_INCREMENT,
    theme_category VARCHAR(50)  NOT NULL,
    theme_name     VARCHAR(50)  NOT NULL,
    PRIMARY KEY (theme_id),
    UNIQUE KEY UQ_THEME_NAME (theme_name)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

INSERT INTO theme (theme_category, theme_name) VALUES
                                                   ('ACTIVITY_COMMUNITY', '불멍'),
                                                   ('ACTIVITY_COMMUNITY', '포틀럭'),
                                                   ('ACTIVITY_COMMUNITY', '러닝'),
                                                   ('ACTIVITY_COMMUNITY', '서핑'),
                                                   ('ACTIVITY_COMMUNITY', '하이킹'),
                                                   ('ACTIVITY_COMMUNITY', '신나는 게스트하우스'),
                                                   ('ACTIVITY_COMMUNITY', '조용한 게스트하우스'),

                                                   ('LOCATION', '공항 주변'),
                                                   ('LOCATION', '노을 맛집(노을 명소)'),

                                                   ('LIFESTYLE', '여성 전용'),
                                                   ('LIFESTYLE', '1인실'),
                                                   ('LIFESTYLE', '독서'),
                                                   ('LIFESTYLE', '스냅 촬영'),
                                                   ('LIFESTYLE', 'MBTI E'),
                                                   ('LIFESTYLE', 'MBTI I'),

                                                   ('FOOD', '조식'),
                                                   ('FOOD', '오마카세');