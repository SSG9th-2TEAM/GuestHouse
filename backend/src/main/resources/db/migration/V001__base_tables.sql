-- 초기 스키마 정의 (MySQL / Flyway V001)
-- 테이블/컬럼 코멘트는 원문 요구사항을 최대한 반영했습니다.
-- FK/인덱스는 기본만 포함했으니, 실제 조회 패턴에 맞춰 추가 검토하세요.

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

USE guesthouse;

-- 이전 실패 이력으로 남아있을 수 있는 테이블 정리
DROP TABLE IF EXISTS accommodation_theme;
DROP TABLE IF EXISTS user_theme;
DROP TABLE IF EXISTS theme;

CREATE TABLE IF NOT EXISTS theme
(
    theme_id       BIGINT         NOT NULL AUTO_INCREMENT COMMENT '테마 PK',
    theme_category VARCHAR(50)     NOT NULL COMMENT '대분류',
    theme_name     VARCHAR(50)     NOT NULL COMMENT '테마 이름 (UNIQUE)',
    CONSTRAINT PK_THEME PRIMARY KEY (theme_id),
    CONSTRAINT UQ_THEME_NAME UNIQUE (theme_name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

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

CREATE TABLE IF NOT EXISTS users
(
    user_id         BIGINT               NOT NULL AUTO_INCREMENT COMMENT '회원 PK',
    email           VARCHAR(50)          NOT NULL COMMENT '로그인 ID, UNIQUE',
    password        VARCHAR(255)         NULL COMMENT '소셜 로그인 시 NULL 가능',
    phone           VARCHAR(50)          NOT NULL COMMENT '휴대폰 번호',
    role            ENUM ('HOST','USER') NULL COMMENT '호스트 / 유저',
    marketing_agree TINYINT              NOT NULL COMMENT '마케팅 동의 (0/1)',
    created_at      DATETIME             NOT NULL COMMENT '가입일',
    updated_at      DATETIME             NOT NULL COMMENT '수정일',
    host_approved   TINYINT              NULL COMMENT '호스트 승인 상태 (1 승인, 0 거절)',
    CONSTRAINT PK_USERS PRIMARY KEY (user_id),
    CONSTRAINT UQ_USERS_EMAIL UNIQUE (email)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS account_number
(
    account_number_id INT         NOT NULL AUTO_INCREMENT COMMENT '계좌 PK',
    bank_name         VARCHAR(50) NOT NULL COMMENT '은행명',
    account_number    VARCHAR(50) NOT NULL COMMENT '계좌번호',
    account_holder    VARCHAR(50) NULL COMMENT '예금주',
    CONSTRAINT PK_ACCOUNT_NUMBER PRIMARY KEY (account_number_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS accommodation
(
    accommodations_id            BIGINT                                 NOT NULL AUTO_INCREMENT COMMENT '숙소 PK',
    account_number_id            INT                                    NOT NULL COMMENT '정산 계좌 ID',
    user_id                      BIGINT                                 NOT NULL COMMENT '회원 ID (호스트)',
    accommodations_name          VARCHAR(100)                           NOT NULL COMMENT '숙소 이름',
    accommodations_category      ENUM ('PENSION','GUESTHOUSE')          NOT NULL DEFAULT 'PENSION' COMMENT '숙소 카테고리',
    accommodations_description   TEXT                                   NOT NULL COMMENT '숙소 상세 설명',
    city                         VARCHAR(50)                            NOT NULL COMMENT '도시(광역시)',
    district                     VARCHAR(50)                            NULL COMMENT '구/군',
    address_detail               VARCHAR(200)                           NOT NULL COMMENT '상세 주소',
    latitude                     DECIMAL(10, 7)                         NOT NULL COMMENT '위도',
    longitude                    DECIMAL(10, 7)                         NOT NULL COMMENT '경도',
    transport_info               TEXT                                   NULL COMMENT '주변 교통 정보',
    accommodation_status         TINYINT(1)                             NOT NULL COMMENT '운영 상태 (예: 0 비활성 / 1 활성)',
    rejection_reason             TEXT                                   NULL COMMENT '승인 반려 사유',
    approval_status              ENUM ('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING' COMMENT '승인 상태',
    short_description            VARCHAR(100)                           NOT NULL COMMENT '한줄 소개',
    created_at                   DATETIME                               NOT NULL COMMENT '생성일',
    business_registration_number VARCHAR(20)                            NOT NULL COMMENT '사업자등록번호(하이픈 제거)',
    business_registration_image  VARCHAR(255)                           NOT NULL COMMENT '사업자등록증 이미지',
    CONSTRAINT PK_ACCOMMODATION PRIMARY KEY (accommodations_id),
    CONSTRAINT FK_ACC_USER FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT FK_ACC_ACCOUNT FOREIGN KEY (account_number_id) REFERENCES account_number (account_number_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS amenity
(
    amenity_id    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '편의시설 PK',
    amenity_code  VARCHAR(50)  NOT NULL COMMENT '편의시설 코드 (예: PARKING, WIFI)',
    amenity_name  VARCHAR(50)  NOT NULL COMMENT '편의시설 이름',
    is_active     TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '사용 여부 (0/1)',
    amenity_icon  VARCHAR(255) NOT NULL COMMENT '아이콘 URL/코드',
    display_order INT          NOT NULL DEFAULT 0 COMMENT '노출 순서',
    CONSTRAINT PK_AMENITY PRIMARY KEY (amenity_id),
    CONSTRAINT UQ_AMENITY_CODE UNIQUE (amenity_code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS accommodation_amenity
(
    accommodation_amenity_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '숙소-편의시설 매핑 PK',
    accommodations_id        BIGINT NOT NULL COMMENT '숙소 PK',
    amenity_id               BIGINT NOT NULL COMMENT '편의시설 PK',
    CONSTRAINT PK_ACCOMMODATION_AMENITY PRIMARY KEY (accommodation_amenity_id),
    CONSTRAINT FK_ACC_AMENITY_ACC FOREIGN KEY (accommodations_id) REFERENCES accommodation (accommodations_id),
    CONSTRAINT FK_ACC_AMENITY_AMENITY FOREIGN KEY (amenity_id) REFERENCES amenity (amenity_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS accommodation_image
(
    image_id          BIGINT UNSIGNED          NOT NULL AUTO_INCREMENT COMMENT '이미지 PK',
    accommodations_id BIGINT                   NOT NULL COMMENT '숙소 PK',
    image_url         VARCHAR(200)             NOT NULL COMMENT '이미지 경로',
    image_type        ENUM ('banner','detail') NOT NULL DEFAULT 'banner' COMMENT '이미지 타입',
    sort_order        INT                      NOT NULL COMMENT '정렬 순서',
    CONSTRAINT PK_ACCOMMODATION_IMAGE PRIMARY KEY (image_id),
    CONSTRAINT FK_ACC_IMAGE_ACC FOREIGN KEY (accommodations_id) REFERENCES accommodation (accommodations_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS accommodation_policy
(
    policy_id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '정책 PK',
    accommodations_id   BIGINT          NOT NULL COMMENT '숙소 PK',
    check_in_time       VARCHAR(10)     NOT NULL COMMENT '체크인 시각 (예: 15:00)',
    check_out_time      VARCHAR(10)     NOT NULL COMMENT '체크아웃 시각 (예: 11:00)',
    cancellation_policy TEXT            NOT NULL COMMENT '취소/환불 규정',
    house_rules         TEXT            NOT NULL COMMENT '하우스 규칙',
    CONSTRAINT PK_ACCOMMODATION_POLICY PRIMARY KEY (policy_id),
    CONSTRAINT FK_ACC_POLICY_ACC FOREIGN KEY (accommodations_id) REFERENCES accommodation (accommodations_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS accommodation_theme
(
    theme_mapping_id  BIGINT NOT NULL AUTO_INCREMENT COMMENT '숙소-테마 매핑 PK',
    theme_id          BIGINT NOT NULL COMMENT '테마 PK',
    accommodations_id BIGINT NOT NULL COMMENT '숙소 PK',
    CONSTRAINT PK_ACCOMMODATION_THEME PRIMARY KEY (theme_mapping_id),
    CONSTRAINT FK_ACC_THEME_THEME FOREIGN KEY (theme_id) REFERENCES theme (theme_id),
    CONSTRAINT FK_ACC_THEME_ACC FOREIGN KEY (accommodations_id) REFERENCES accommodation (accommodations_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS user_theme
(
    id       BIGINT NOT NULL AUTO_INCREMENT COMMENT '선택 PK',
    user_id  BIGINT NOT NULL COMMENT '회원 PK',
    theme_id BIGINT NOT NULL COMMENT '테마 PK',
    CONSTRAINT PK_USER_THEME PRIMARY KEY (id),
    CONSTRAINT FK_USER_THEME_USER FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT FK_USER_THEME_THEME FOREIGN KEY (theme_id) REFERENCES theme (theme_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS wishlist
(
    wish_id           BIGINT   NOT NULL AUTO_INCREMENT COMMENT '위시리스트 PK',
    accommodations_id BIGINT   NOT NULL COMMENT '숙소 PK',
    created_at        DATETIME NOT NULL COMMENT '추가 시각',
    user_id           BIGINT   NOT NULL COMMENT '회원 PK',
    CONSTRAINT PK_WISHLIST PRIMARY KEY (wish_id),
    CONSTRAINT FK_WISHLIST_USER FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT FK_WISHLIST_ACC FOREIGN KEY (accommodations_id) REFERENCES accommodation (accommodations_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS review_tag
(
    review_tag_id   INT         NOT NULL AUTO_INCREMENT COMMENT '태그 PK',
    review_tag_name VARCHAR(50) NOT NULL COMMENT '태그 이름',
    is_active       TINYINT     NOT NULL DEFAULT 1 COMMENT '사용 여부 (0/1)',
    CONSTRAINT PK_REVIEW_TAG PRIMARY KEY (review_tag_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS review
(
    review_id         BIGINT        NOT NULL AUTO_INCREMENT COMMENT '리뷰 PK',
    accommodations_id BIGINT        NOT NULL COMMENT '숙소 PK',
    user_id           BIGINT        NOT NULL COMMENT '회원 PK',
    rating            DECIMAL(2, 1) NOT NULL COMMENT '평점 0.0~5.0',
    content           TEXT          NOT NULL COMMENT '리뷰 본문',
    created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '작성 시각',
    updated_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
    is_deleted        TINYINT(1)    NOT NULL DEFAULT 1 COMMENT '삭제 플래그 (0/1)',
    CONSTRAINT PK_REVIEW PRIMARY KEY (review_id),
    CONSTRAINT FK_REVIEW_ACC FOREIGN KEY (accommodations_id) REFERENCES accommodation (accommodations_id),
    CONSTRAINT FK_REVIEW_USER FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT CK_REVIEW_RATING CHECK (rating >= 0.0 AND rating <= 5.0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS review_image
(
    review_image_id  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '리뷰 이미지 PK',
    review_id        BIGINT       NOT NULL COMMENT '리뷰 PK',
    review_image_url VARCHAR(255) NOT NULL COMMENT '이미지 URL',
    sort_order       INT          NULL     DEFAULT 1 COMMENT '노출 순서',
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
    CONSTRAINT PK_REVIEW_IMAGE PRIMARY KEY (review_image_id),
    CONSTRAINT FK_REVIEW_IMAGE_REVIEW FOREIGN KEY (review_id) REFERENCES review (review_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS review_tag_map
(
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '매핑 PK',
    review_tag_id INT             NOT NULL COMMENT '태그 PK',
    review_id     BIGINT          NOT NULL COMMENT '리뷰 PK',
    CONSTRAINT PK_REVIEW_TAG_MAP PRIMARY KEY (id),
    CONSTRAINT FK_REVIEW_TAG_MAP_TAG FOREIGN KEY (review_tag_id) REFERENCES review_tag (review_tag_id),
    CONSTRAINT FK_REVIEW_TAG_MAP_REVIEW FOREIGN KEY (review_id) REFERENCES review (review_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS review_replies
(
    reply_id   BIGINT   NOT NULL AUTO_INCREMENT COMMENT '답글 PK',
    review_id  BIGINT   NOT NULL COMMENT '리뷰 PK',
    user_id    BIGINT   NOT NULL COMMENT '회원 PK',
    content    TEXT     NOT NULL COMMENT '답글 내용',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
    updated_at DATETIME NULL COMMENT '수정 시각',
    CONSTRAINT PK_REVIEW_REPLIES PRIMARY KEY (reply_id),
    CONSTRAINT FK_REVIEW_REPLIES_REVIEW FOREIGN KEY (review_id) REFERENCES review (review_id),
    CONSTRAINT FK_REVIEW_REPLIES_USER FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS review_reports
(
    report_id  BIGINT      NOT NULL AUTO_INCREMENT COMMENT '신고 PK',
    review_id  BIGINT      NOT NULL COMMENT '리뷰 PK',
    user_id    BIGINT      NOT NULL COMMENT '회원 PK',
    reason     TEXT        NOT NULL COMMENT '신고 사유',
    state      VARCHAR(20) NOT NULL COMMENT 'WAIT / CHECKED / BLINDED 등',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '신고 접수 시각',
    updated_at DATETIME    NULL COMMENT '처리 완료 시각',
    CONSTRAINT PK_REVIEW_REPORTS PRIMARY KEY (report_id),
    CONSTRAINT FK_REVIEW_REPORT_REVIEW FOREIGN KEY (review_id) REFERENCES review (review_id),
    CONSTRAINT FK_REVIEW_REPORT_USER FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS room
(
    room_id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '객실 PK',
    accommodations_id BIGINT          NOT NULL COMMENT '숙소 PK',
    room_name         VARCHAR(100)    NOT NULL COMMENT '객실 이름',
    price             VARCHAR(255)    NOT NULL COMMENT '1박 기준 금액',
    min_guests        INT             NOT NULL COMMENT '기준 인원',
    max_guests        INT             NOT NULL COMMENT '최대 인원',
    room_size         INT             NULL COMMENT '제곱미터',
    room_description  TEXT            NOT NULL COMMENT '상세 설명',
    main_image_url    VARCHAR(255)    NOT NULL COMMENT '대표 이미지',
    room_status       TINYINT(1)      NOT NULL COMMENT '1 활성, 0 비활성',
    bathroom_count    INT             NOT NULL COMMENT '욕실 개수',
    room_type         VARCHAR(50)     NOT NULL COMMENT '객실 타입',
    bed_count         INT             NOT NULL COMMENT '침대 개수',
    create_room       DATETIME        NULL COMMENT '생성일',
    change_info_room  DATETIME        NULL COMMENT '수정일',
    CONSTRAINT PK_ROOM PRIMARY KEY (room_id),
    CONSTRAINT FK_ROOM_ACC FOREIGN KEY (accommodations_id) REFERENCES accommodation (accommodations_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS reservation
(
    reservation_id         BIGINT           NOT NULL AUTO_INCREMENT COMMENT '예약 PK',
    accommodations_id      BIGINT           NOT NULL COMMENT '숙소 PK',
    user_id                BIGINT           NOT NULL COMMENT '예약자 회원 PK',
    checkin                DATETIME         NOT NULL COMMENT '체크인',
    checkout               DATETIME         NOT NULL COMMENT '체크아웃',
    stay_nights            TINYINT UNSIGNED NOT NULL COMMENT '숙박 박수',
    guest_count            TINYINT UNSIGNED NOT NULL COMMENT '총 인원',
    reservation_status     TINYINT(1)       NOT NULL COMMENT '0 임시, 1 요청, 2 확정, 3 체크인완료, 9 취소',
    total_amount_before_dc INT UNSIGNED     NOT NULL COMMENT '할인 전 합계',
    coupon_discount_amount INT UNSIGNED     NOT NULL DEFAULT 0 COMMENT '쿠폰 할인액',
    final_payment_amount   INT UNSIGNED     NOT NULL COMMENT '최종 결제 금액',
    payment_status         TINYINT(1)       NOT NULL COMMENT '0 미결제, 1 완료, 2 실패, 3 환불완료',
    reserver_name          VARCHAR(50)      NOT NULL COMMENT '예약자 이름',
    reserver_phone         VARCHAR(20)      NOT NULL COMMENT '예약자 전화번호',
    created_at             DATETIME         NOT NULL COMMENT '생성 시각',
    updated_at             DATETIME         NOT NULL COMMENT '수정 시각',
    CONSTRAINT PK_RESERVATION PRIMARY KEY (reservation_id),
    CONSTRAINT FK_RESERVATION_ACC FOREIGN KEY (accommodations_id) REFERENCES accommodation (accommodations_id),
    CONSTRAINT FK_RESERVATION_USER FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS payment
(
    payment_id       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '결제 PK',
    reservation_id   BIGINT       NOT NULL COMMENT '예약 PK',
    pg_provider_code VARCHAR(20)  NOT NULL COMMENT 'PG사 코드 (TOSS 등)',
    payment_method   VARCHAR(20)  NOT NULL COMMENT '결제 수단',
    order_id         VARCHAR(50)  NOT NULL COMMENT '우리 주문번호',
    pg_payment_key   VARCHAR(100) NULL COMMENT 'PG 결제키',
    request_amount   INTEGER      NOT NULL COMMENT '요청 금액',
    approved_amount  INTEGER      NULL COMMENT '승인 금액',
    currency_code    CHAR(3)      NOT NULL DEFAULT 'KRW' COMMENT '통화 코드',
    payment_status   TINYINT(1)   NOT NULL COMMENT '0 요청, 1 성공, 2 실패, 3 취소',
    failure_code     VARCHAR(50)  NULL COMMENT 'PG 실패 코드',
    failure_message  VARCHAR(255) NULL COMMENT '실패 사유',
    approved_at      DATETIME     NULL COMMENT '승인 시각',
    created_at       DATETIME     NOT NULL COMMENT '생성 시각',
    updated_at       DATETIME     NOT NULL COMMENT '수정 시각',
    CONSTRAINT PK_PAYMENT PRIMARY KEY (payment_id),
    CONSTRAINT FK_PAYMENT_RESERVATION FOREIGN KEY (reservation_id) REFERENCES reservation (reservation_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS payment_refund
(
    refund_id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '환불 PK',
    payment_id        BIGINT       NOT NULL COMMENT '결제 PK',
    refund_amount     INTEGER      NOT NULL COMMENT '환불 요청 금액',
    refund_status     TINYINT(1)   NOT NULL COMMENT '0 요청, 1 성공, 2 실패',
    pg_refund_key     VARCHAR(100) NULL COMMENT 'PG 환불 키',
    pg_transaction_id VARCHAR(100) NULL COMMENT 'PG 트랜잭션 ID',
    failure_code      VARCHAR(50)  NULL COMMENT 'PG 실패 코드',
    failure_message   VARCHAR(255) NULL COMMENT '실패 사유',
    reason_code       VARCHAR(50)  NULL COMMENT '환불 사유 코드',
    reason_message    VARCHAR(255) NULL COMMENT '환불 사유 상세',
    requested_by      VARCHAR(50)  NOT NULL COMMENT '요청 주체',
    requested_at      DATETIME     NOT NULL COMMENT '요청 시각',
    approved_at       DATETIME     NULL COMMENT '성공 시각',
    created_at        DATETIME     NOT NULL COMMENT '생성 시각',
    updated_at        DATETIME     NOT NULL COMMENT '수정 시각',
    CONSTRAINT PK_PAYMENT_REFUND PRIMARY KEY (refund_id),
    CONSTRAINT FK_REFUND_PAYMENT FOREIGN KEY (payment_id) REFERENCES payment (payment_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS user_coupon
(
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '유저-쿠폰 PK',
    coupon_id  BIGINT UNSIGNED NOT NULL COMMENT '쿠폰 PK',
    user_id    BIGINT          NOT NULL COMMENT '회원 PK',
    issued_at  DATETIME        NOT NULL COMMENT '발급 시각',
    used_at    DATETIME        NULL COMMENT '사용 시각',
    expired_at DATETIME        NULL COMMENT '만료 시각',
    status     VARCHAR(20)     NOT NULL DEFAULT 'ISSUED' COMMENT 'ISSUED / USED / EXPIRED',
    CONSTRAINT PK_USER_COUPON PRIMARY KEY (id),
    CONSTRAINT FK_USER_COUPON_COUPON FOREIGN KEY (coupon_id) REFERENCES coupon (coupon_id),
    CONSTRAINT FK_USER_COUPON_USER FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS user_social
(
    social_id     BIGINT                  NOT NULL AUTO_INCREMENT COMMENT '소셜 PK',
    user_id       BIGINT                  NOT NULL COMMENT '회원 PK',
    provider      ENUM ('GOOGLE','KAKAO') NOT NULL COMMENT '소셜 제공자',
    provider_uid  VARCHAR(255)            NOT NULL COMMENT '프로바이더 고유값',
    email         VARCHAR(255)            NULL COMMENT '소셜 이메일',
    profile_image VARCHAR(255)            NULL COMMENT '프로필 이미지',
    created_at    DATETIME                NOT NULL COMMENT '최초 로그인',
    CONSTRAINT PK_USER_SOCIAL PRIMARY KEY (social_id),
    CONSTRAINT UQ_USER_SOCIAL UNIQUE (provider, provider_uid),
    CONSTRAINT FK_USER_SOCIAL_USER FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS platform_daily_stats
(
    stat_date            DATE          NOT NULL COMMENT '기준 일자',
    total_hosts          INT           NOT NULL COMMENT '전체 호스트 수',
    new_hosts            INT           NOT NULL COMMENT '신규 호스트 수',
    total_accommodations INT           NOT NULL COMMENT '전체 숙소 수',
    new_accommodations   INT           NOT NULL COMMENT '신규 숙소 수',
    total_reservations   INT           NOT NULL COMMENT '생성된 예약 건수',
    reservations_success INT           NOT NULL COMMENT '결제 성공 건수',
    reservations_failed  INT           NOT NULL COMMENT '결제 실패/취소 건수',
    total_revenue        INT           NOT NULL COMMENT '결제 완료 금액 합',
    active_guests        INT           NOT NULL COMMENT '활동 게스트 수',
    active_hosts         INT           NOT NULL COMMENT '활동 호스트 수',
    occupancy_rate       DECIMAL(5, 2) NOT NULL COMMENT '객실 점유율(%)',
    created_at           DATETIME      NOT NULL COMMENT '생성 시각(배치 실행 시간)',
    CONSTRAINT PK_PLATFORM_DAILY_STATS PRIMARY KEY (stat_date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS host_daily_stats
(
    host_id           INT           NOT NULL COMMENT '호스트 ID',
    stat_date         DATE          NOT NULL COMMENT '기준 일자',
    user_id           BIGINT        NOT NULL COMMENT '회원 PK',
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
    CONSTRAINT PK_HOST_DAILY_STATS PRIMARY KEY (host_id, stat_date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS admins
(
    admin_id       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '관리자 PK',
    admin_username VARCHAR(100) NOT NULL COMMENT '로그인 ID(UNIQUE)',
    admin_password VARCHAR(255) NOT NULL COMMENT '비밀번호',
    created_at     DATETIME     NOT NULL COMMENT '생성일',
    CONSTRAINT PK_ADMINS PRIMARY KEY (admin_id),
    CONSTRAINT UQ_ADMINS_USERNAME UNIQUE (admin_username)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS admin_log
(
    log_id      BIGINT      NOT NULL AUTO_INCREMENT COMMENT '로그 PK',
    admin_id    BIGINT      NOT NULL COMMENT '관리자 PK',
    target_type VARCHAR(20) NOT NULL COMMENT 'USER, ACC, RSV 등',
    target_id   BIGINT      NOT NULL COMMENT '대상 ID',
    action_type VARCHAR(50) NOT NULL COMMENT 'APPROVE, REJECT, BAN 등',
    reason      TEXT        NULL COMMENT '사유',
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '발생 시각',
    CONSTRAINT PK_ADMIN_LOG PRIMARY KEY (log_id),
    CONSTRAINT FK_ADMIN_LOG_ADMIN FOREIGN KEY (admin_id) REFERENCES admins (admin_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS notices
(
    notice_id  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '공지 PK',
    title      VARCHAR(200) NOT NULL COMMENT '제목',
    content    TEXT         NOT NULL COMMENT '내용',
    is_popup   TINYINT      NOT NULL DEFAULT 0 COMMENT '팝업 여부',
    view_count INT          NOT NULL DEFAULT 0 COMMENT '조회수',
    created_at DATETIME     NULL     DEFAULT CURRENT_TIMESTAMP COMMENT '작성 시간',
    updated_at DATETIME     NULL     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',
    CONSTRAINT PK_NOTICES PRIMARY KEY (notice_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS inquiries
(
    inquiry_id  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '문의 PK',
    type        VARCHAR(20)  NULL     DEFAULT 'GENERAL' COMMENT '문의 유형',
    title       VARCHAR(200) NOT NULL COMMENT '문의 제목',
    content     TEXT         NOT NULL COMMENT '문의 내용',
    answer      TEXT         NULL COMMENT '관리자 답변',
    status      VARCHAR(20)  NOT NULL DEFAULT 'WAIT' COMMENT 'WAIT / DONE',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록 시각',
    answered_at DATETIME     NULL COMMENT '답변 완료 시각',
    CONSTRAINT PK_INQUIRIES PRIMARY KEY (inquiry_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;
