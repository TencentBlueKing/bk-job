USE job_op;

CREATE TABLE IF NOT EXISTS `issue_status` (
    `id`                 VARCHAR(128) NOT NULL COMMENT 'Issue标识，如 issue_4523',
    `issue_no`           BIGINT       NULL     COMMENT '从id中解析出的Issue编号（应用层写入），用于按编号排序；id不符合 issue_数字 格式时为NULL',
    `status`             VARCHAR(128) NULL     COMMENT 'Issue状态，如 issue_todo',
    `status_description` TEXT         NULL     COMMENT 'Issue状态描述',
    `update_time`        BIGINT       NOT NULL COMMENT '状态最后更新时间（毫秒时间戳，应用层写入）',
    `row_create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '行创建时间',
    `row_update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '行更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_issue_no` (`issue_no`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = 'OP Issue状态表';
