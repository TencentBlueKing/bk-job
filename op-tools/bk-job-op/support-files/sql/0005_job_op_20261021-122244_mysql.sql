USE job_op;

CREATE TABLE IF NOT EXISTS `pull_request` (
    `id`                         BIGINT        NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    `pr_id`                      BIGINT        NOT NULL COMMENT 'GitHub PR编号',
    `issue_id`                   BIGINT        NOT NULL COMMENT '对应GitHub Issue编号',
    `code_dir`                   VARCHAR(1024) NOT NULL COMMENT '开发PR时使用的代码目录',
    `status`                     VARCHAR(32)   NOT NULL COMMENT 'WAIT_FOR_REVIEW/HANDLING/MERGED/CLOSED',
    `agent_name`                 VARCHAR(128)  NOT NULL COMMENT '提交PR的Agent名称',
    `author_name`                VARCHAR(128)  NOT NULL COMMENT '提交PR的真实用户名',
    `imate_conversation_id`      VARCHAR(128)  NULL COMMENT 'IMate数字分身会话ID',
    `imate_conversation_title`   VARCHAR(512)  NULL COMMENT 'IMate数字分身会话标题',
    `create_time`                BIGINT        NOT NULL COMMENT '创建时间（毫秒时间戳，由应用层写入）',
    `update_time`                BIGINT        NOT NULL COMMENT '更新时间（毫秒时间戳，由应用层写入）',
    `row_create_time`            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '行创建时间',
    `row_update_time`            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
                                                ON UPDATE CURRENT_TIMESTAMP COMMENT '行更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_pr_id` (`pr_id`),
    KEY `idx_issue_id_create_time_pr_id` (`issue_id`, `create_time`, `pr_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = 'OP GitHub PR信息表';
