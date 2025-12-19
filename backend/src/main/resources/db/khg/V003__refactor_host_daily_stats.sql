-- V003__refactor_host_daily_stats.sql
-- host_daily_stats를 "user_id + stat_date 복합 PK" 구조로 재생성 (구현 초기 단계용)

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS host_daily_stats;

CREATE TABLE host_daily_stats
(
    user_id           BIGINT        NOT NULL COMMENT '호스트 user_id (users.user_id)',
    stat_date         DATE          NOT NULL COMMENT '기준 일자',
    reservation_count INT           NOT NULL COMMENT '예약 건수',
    reserved_nights   INT           NOT NULL COMMENT '총 숙박 박수',
    total_guests      INT           NOT NULL COMMENT '총 투숙 인원',
    revenue           INT           NOT NULL COMMENT '결제 완료 금액 합',
    canceled_count    INT           NOT NULL COMMENT '취소/실패 건수',
    avg_price         INT           NULL DEFAULT NULL COMMENT '평균 요금(1박 기준)',
    review_count      INT           NULL COMMENT '등록 리뷰 수',
    avg_rating        DECIMAL(2, 1) NULL COMMENT '신규 리뷰 평균 평점',
    occupancy_rate    DECIMAL(5, 2) NULL COMMENT '객실 점유율 %',
    created_at        DATETIME      NOT NULL COMMENT '생성 시각',
    updated_at        DATETIME      NOT NULL COMMENT '갱신 시각',

    PRIMARY KEY (user_id, stat_date),
    CONSTRAINT FK_HOST_DAILY_STATS_USER
        FOREIGN KEY (user_id) REFERENCES users (user_id),

    INDEX IDX_HOST_DAILY_STATS_DATE (stat_date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;
