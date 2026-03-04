-- 库名：job_op
CREATE DATABASE IF NOT EXISTS job_op DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE job_op;

-- 业务迁移记录表
CREATE TABLE IF NOT EXISTS `migrate_biz` (
    `biz_id`          VARCHAR(32)  NOT NULL COMMENT '业务ID',
    `create_time`     BIGINT       NOT NULL COMMENT '迁移时间（毫秒时间戳）',
    `row_create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '行创建时间',
    `row_update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '行更新时间',
    PRIMARY KEY (`biz_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = '业务迁移记录表';

-- 按业务分批迁移环境时的审计历史记录表
CREATE TABLE IF NOT EXISTS `migrate_history` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    `biz_id`      VARCHAR(32)  NOT NULL                COMMENT '业务ID',
    `action`      VARCHAR(16)  NOT NULL                COMMENT '操作类型：add-新增迁移标记，delete-删除迁移标记',
    `create_time` BIGINT       NOT NULL                COMMENT '操作时间（毫秒时间戳，由应用层传入）',
    PRIMARY KEY (`id`),
    KEY `idx_biz_id_action` (`biz_id`, `action`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = '按业务分批迁移环境时的审计历史记录表';
