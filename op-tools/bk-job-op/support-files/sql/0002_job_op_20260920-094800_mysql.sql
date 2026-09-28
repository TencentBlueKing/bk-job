USE job_op;

CREATE TABLE IF NOT EXISTS `version_branch` (
    `version_branch`                      VARCHAR(64)   NOT NULL COMMENT '版本分支，如 3.10.x / master',
    `description`                         VARCHAR(512)  NULL     COMMENT '版本分支描述',
    `dev_branch`                          VARCHAR(128)  NULL     COMMENT '对应开发分支，如 bk-dev_3.10.x',
    `dev_branch_deploy_pipeline_cmd`      TEXT          NULL     COMMENT '开发分支部署流水线远程触发命令',
    `dev_branch_deploy_pipeline_cmd_desc` TEXT          NULL     COMMENT '上述命令的使用说明与示例',
    `create_time`                         BIGINT        NOT NULL COMMENT '创建时间（毫秒时间戳，应用层写入）',
    `last_modify_time`                    BIGINT        NOT NULL COMMENT '最后修改时间（毫秒时间戳，应用层写入）',
    `row_create_time`                     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '行创建时间',
    `row_update_time`                     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '行更新时间',
    PRIMARY KEY (`version_branch`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = 'OP版本分支信息表';
