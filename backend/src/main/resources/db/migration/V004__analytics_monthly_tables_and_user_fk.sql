-- V004__monthly_stats.sql
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE platform_monthly_stats
(
    month_start          DATE          NOT NULL PRIMARY KEY COMMENT '해당 월의 첫날(YYYY-MM-01)',
    total_hosts          INT           NOT NULL COMMENT '월 말 기준 전체 호스트 수(스냅샷)',
    new_hosts            INT           NOT NULL COMMENT '월 신규 호스트 수(합)',
    total_accommodations INT           NOT NULL COMMENT '월 말 기준 전체 숙소 수(스냅샷)',
    new_accommodations   INT           NOT NULL COMMENT '월 신규 숙소 수(합)',
    total_reservations   INT           NOT NULL COMMENT '월 예약 생성 건수(합)',
    reservations_success INT           NOT NULL COMMENT '월 결제 성공 건수(합)',
    reservations_failed  INT           NOT NULL COMMENT '월 결제 실패/취소 건수(합)',
    total_revenue        INT           NOT NULL COMMENT '월 결제 완료 금액(합)',
    avg_occupancy_rate   DECIMAL(5, 2) NOT NULL COMMENT '월 평균 점유율(단순평균)',
    last_snapshot_at     DATETIME      NOT NULL COMMENT '원천 일별 최신 시각',
    created_at           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE host_monthly_stats
(
    user_id            BIGINT        NOT NULL COMMENT '호스트 user_id',
    month_start        DATE          NOT NULL COMMENT '해당 월의 첫날(YYYY-MM-01)',
    reservation_count  INT           NOT NULL COMMENT '월 예약 건수(합)',
    reserved_nights    INT           NOT NULL COMMENT '월 숙박 박수(합)',
    total_guests       INT           NOT NULL COMMENT '월 투숙 인원(합)',
    revenue            INT           NOT NULL COMMENT '월 매출(합)',
    canceled_count     INT           NOT NULL COMMENT '월 취소/실패(합)',
    avg_price          INT           NULL COMMENT '월 평균 요금(단순평균/근사)',
    review_count       INT           NULL COMMENT '월 리뷰 수(합)',
    avg_rating         DECIMAL(2, 1) NULL COMMENT '월 평점(단순평균/근사)',
    avg_occupancy_rate DECIMAL(5, 2) NULL COMMENT '월 점유율(단순평균/근사)',
    last_snapshot_at   DATETIME      NOT NULL,
    created_at         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, month_start),
    CONSTRAINT FK_HOST_MONTHLY_USER FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

DELIMITER $$
CREATE PROCEDURE refresh_monthly_stats(IN p_from DATE, IN p_to DATE)
BEGIN
    -- 플랫폼 월별
    INSERT INTO platform_monthly_stats (month_start, total_hosts, new_hosts, total_accommodations, new_accommodations,
                                        total_reservations, reservations_success, reservations_failed, total_revenue,
                                        avg_occupancy_rate, last_snapshot_at)
    SELECT DATE_SUB(stat_date, INTERVAL DAYOFMONTH(stat_date) - 1 DAY) AS month_start,
           MAX(total_hosts)                                            AS total_hosts,          -- 스냅샷은 MAX 추천
           SUM(new_hosts)                                              AS new_hosts,
           MAX(total_accommodations)                                   AS total_accommodations, -- 스냅샷은 MAX 추천
           SUM(new_accommodations)                                     AS new_accommodations,
           SUM(total_reservations)                                     AS total_reservations,
           SUM(reservations_success)                                   AS reservations_success,
           SUM(reservations_failed)                                    AS reservations_failed,
           SUM(total_revenue)                                          AS total_revenue,
           AVG(occupancy_rate)                                         AS avg_occupancy_rate,
           MAX(created_at)                                             AS last_snapshot_at
    FROM platform_daily_stats
    WHERE stat_date BETWEEN p_from AND p_to
    GROUP BY month_start
    ON DUPLICATE KEY UPDATE total_hosts          = VALUES(total_hosts),
                            new_hosts            = VALUES(new_hosts),
                            total_accommodations = VALUES(total_accommodations),
                            new_accommodations   = VALUES(new_accommodations),
                            total_reservations   = VALUES(total_reservations),
                            reservations_success = VALUES(reservations_success),
                            reservations_failed  = VALUES(reservations_failed),
                            total_revenue        = VALUES(total_revenue),
                            avg_occupancy_rate   = VALUES(avg_occupancy_rate),
                            last_snapshot_at     = VALUES(last_snapshot_at),
                            updated_at           = NOW();

    -- 호스트 월별
    INSERT INTO host_monthly_stats (user_id, month_start, reservation_count, reserved_nights, total_guests,
                                    revenue, canceled_count, avg_price, review_count, avg_rating,
                                    avg_occupancy_rate, last_snapshot_at)
    SELECT user_id,
           DATE_SUB(stat_date, INTERVAL DAYOFMONTH(stat_date) - 1 DAY) AS month_start,
           SUM(reservation_count),
           SUM(reserved_nights),
           SUM(total_guests),
           SUM(revenue),
           SUM(canceled_count),
           AVG(avg_price),      -- 근사치(정확도 필요하면 분자/분모로 재설계)
           SUM(review_count),
           AVG(avg_rating),     -- 근사치
           AVG(occupancy_rate), -- 근사치
           MAX(updated_at)
    FROM host_daily_stats
    WHERE stat_date BETWEEN p_from AND p_to
    GROUP BY user_id, month_start
    ON DUPLICATE KEY UPDATE reservation_count  = VALUES(reservation_count),
                            reserved_nights    = VALUES(reserved_nights),
                            total_guests       = VALUES(total_guests),
                            revenue            = VALUES(revenue),
                            canceled_count     = VALUES(canceled_count),
                            avg_price          = VALUES(avg_price),
                            review_count       = VALUES(review_count),
                            avg_rating         = VALUES(avg_rating),
                            avg_occupancy_rate = VALUES(avg_occupancy_rate),
                            last_snapshot_at   = VALUES
