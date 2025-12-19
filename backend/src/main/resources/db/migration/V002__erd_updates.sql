-- ERD 업데이트 반영 (신규 테이블 + 컬럼 추가)
-- 안전하게 기존 데이터 보존을 위해 ALTER/CREATE만 수행합니다.

SET FOREIGN_KEY_CHECKS = 0;

use guesthouse;

-- accommodation: 운영 정보 확장 (MySQL 5.7 호환성 이슈로 컬럼 추가는 V005에서 처리)

-- room: 주말 요금 컬럼 추가(옵션)
ALTER TABLE room
    ADD COLUMN IF NOT EXISTS weekend_price INT UNSIGNED NULL COMMENT '주말 1박 요금';

-- 채팅방 테이블
CREATE TABLE IF NOT EXISTS chat_room
(
    room_id           BIGINT   NOT NULL AUTO_INCREMENT COMMENT '채팅방 PK',
    accommodations_id BIGINT   NOT NULL COMMENT '숙소 PK',
    reservation_id    BIGINT   NOT NULL COMMENT '예약 PK',
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
    CONSTRAINT PK_CHAT_ROOM PRIMARY KEY (room_id),
    CONSTRAINT FK_CHAT_ROOM_ACC FOREIGN KEY (accommodations_id) REFERENCES accommodation (accommodations_id),
    CONSTRAINT FK_CHAT_ROOM_RSV FOREIGN KEY (reservation_id) REFERENCES reservation (reservation_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- 채팅 메시지 테이블
CREATE TABLE IF NOT EXISTS chat_message
(
    message_id BIGINT   NOT NULL AUTO_INCREMENT COMMENT '메시지 PK',
    room_id    BIGINT   NOT NULL COMMENT '채팅방 PK',
    user_id    BIGINT   NOT NULL COMMENT '보낸 회원 PK',
    content    TEXT     NOT NULL COMMENT '메시지 내용',
    sent_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '전송 시각',
    CONSTRAINT PK_CHAT_MESSAGE PRIMARY KEY (message_id),
    CONSTRAINT FK_CHAT_MESSAGE_ROOM FOREIGN KEY (room_id) REFERENCES chat_room (room_id),
    CONSTRAINT FK_CHAT_MESSAGE_USER FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;
