CREATE TABLE IF NOT EXISTS theme
(
    theme_id       BIGINT       NOT NULL AUTO_INCREMENT,
    theme_category VARCHAR(50)  NOT NULL,
    theme_name     VARCHAR(50)  NOT NULL,
    PRIMARY KEY (theme_id),
    UNIQUE KEY UQ_THEME_NAME (theme_name)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;