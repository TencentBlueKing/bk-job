USE job_crontab;

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `crypto_password_rotation_progress` (
  `id`                          BIGINT UNSIGNED     NOT NULL AUTO_INCREMENT,
  `target_password_fingerprint` VARCHAR(16)         NOT NULL COMMENT '目标主密钥指纹(SHA-256前8字节hex)',
  `table_name`                  VARCHAR(64)         NOT NULL COMMENT '目标表名',
  `field_name`                  VARCHAR(64)         NOT NULL COMMENT '目标字段名',
  `last_processed_pk`           VARCHAR(64)         NULL     DEFAULT NULL COMMENT '上次已处理到的主键游标（断点续处理，支持任意PK类型）',
  `processed_rows`              BIGINT              NOT NULL DEFAULT 0 COMMENT '累计已处理行数',
  `re_encrypted_rows`           BIGINT              NOT NULL DEFAULT 0 COMMENT '累计成功重加密行数',
  `skipped_rows`                BIGINT              NOT NULL DEFAULT 0 COMMENT '累计跳过行数（已是主密钥/空值/乐观锁失败）',
  `total_rows`                  BIGINT              NULL     DEFAULT NULL COMMENT '首次启动时统计的待迁移总行数（仅作进度展示参考，NULL表示尚未统计）',
  `status`                      VARCHAR(16)         NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/RUNNING/DONE/FAILED',
  `last_error`                  VARCHAR(512)        NULL     DEFAULT NULL COMMENT '最近一次错误信息',
  `row_create_time`             DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `row_update_time`             DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_fingerprint_table_field` (`target_password_fingerprint`, `table_name`, `field_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='密码轮换迁移进度';
