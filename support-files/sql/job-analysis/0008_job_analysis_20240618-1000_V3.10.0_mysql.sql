SET NAMES utf8mb4;
USE job_analysis;

-- ------------------------
-- 创建AI提示符模板数据表
-- ------------------------
CREATE TABLE IF NOT EXISTS `ai_prompt_template`  (
    `id`                          int(10)                           NOT NULL    AUTO_INCREMENT,
    `code`                        varchar(255)                      NOT NULL                    COMMENT '模板代码，用于唯一标识模板',
    `locale`                      varchar(16)                       NOT NULL    DEFAULT 'zh_CN' COMMENT '语言',
    `name`                        varchar(255)                      NOT NULL                    COMMENT '模板名称',
    `raw_prompt`                  varchar(255)                          NULL    DEFAULT NULL    COMMENT '不含上下文的简单提示符',
    `template`                    TEXT                              NOT NULL                    COMMENT '模板内容',
    `description`                 TEXT                                                          COMMENT '对模板的描述',
    `creator`                     varchar(128)                      NOT NULL                    COMMENT '创建者',
    `last_modify_user`            varchar(128)                          NULL    DEFAULT NULL    COMMENT '更新者',
    `create_time`                 bigint(20) UNSIGNED                   NULL    DEFAULT NULL    COMMENT '创建时间',
    `last_modify_time`            bigint(20) UNSIGNED                   NULL    DEFAULT NULL    COMMENT '更新时间',
    `row_create_time`             datetime                          NOT NULL    DEFAULT CURRENT_TIMESTAMP,
    `row_update_time`             datetime                          NOT NULL    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `idx_code_locale` (`code`,`locale`) USING BTREE
) ENGINE = InnoDB
CHARACTER SET = utf8mb4;

-- ------------------------
-- 创建AI对话记录表
-- ------------------------
CREATE TABLE IF NOT EXISTS `ai_chat_history`  (
    `id`                          bigint(20)                        NOT NULL    AUTO_INCREMENT,
    `username`                    varchar(64)                       NOT NULL                    COMMENT '用户名',
    `user_input`                  TEXT                              NOT NULL                    COMMENT '用户输入内容',
    `prompt_template_id`          int(10)                               NULL    DEFAULT NULL    COMMENT '使用的提示符模板ID',
    `ai_input`                    TEXT                              NOT NULL                    COMMENT '提交给AI的输入内容',
    `ai_answer`                   TEXT                              NOT NULL                    COMMENT 'AI回答的内容',
    `error_code`                  varchar(128)                          NULL    DEFAULT NULL    COMMENT 'AI回答失败时的错误码',
    `error_message`               varchar(512)                          NULL    DEFAULT NULL    COMMENT 'AI回答失败时的错误信息',
    `start_time`                  bigint(20) UNSIGNED                   NULL    DEFAULT NULL    COMMENT '开始时间',
    `answer_time`                 bigint(20) UNSIGNED                   NULL    DEFAULT NULL    COMMENT 'AI回答完成时间',
    `total_time`                  bigint(20) UNSIGNED                   NULL    DEFAULT NULL    COMMENT '总耗时',
    `is_deleted`                  TINYINT(1) UNSIGNED               NOT NULL    DEFAULT '0'     COMMENT '是否已删除：0表示未删除，1表示已删除',
    `row_create_time`             datetime                          NOT NULL    DEFAULT CURRENT_TIMESTAMP,
    `row_update_time`             datetime                          NOT NULL    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_username_startTime` (`username`,`start_time`) USING BTREE,
    INDEX `idx_start_time` (`start_time`) USING BTREE
) ENGINE = InnoDB
CHARACTER SET = utf8mb4;

