USE job_op;

-- 代码仓库Tag表
-- 本表是Git仓库Tag的派生镜像：数据源头在Git，误删后重跑一次批量写入接口即可完全恢复，
-- 因此不像migrate_biz那样配套migrate_history审计表，删除操作的追溯诉求由应用层的INFO操作日志覆盖。
-- 表数据永久保留，不设任何过期/归档策略。
-- 各段版本号拆成独立整数列是因为按版本前缀查询无法用字符串LIKE实现：LIKE '3.1%' 会错误命中3.10.x与3.11.x，
-- 且字典序下 'v3.10.0' < 'v3.9.20' 与语义相反。
-- tag列使用utf8mb4_unicode_ci（大小写不敏感），应用层写入前已统一归一化为小写；
-- 未来若需要区分大小写的Tag，须将tag列改为utf8mb4_bin。
CREATE TABLE IF NOT EXISTS `repo_tag` (
    `id`              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    `tag`             VARCHAR(64) NOT NULL                COMMENT '归一化后的完整Tag，小写v前缀，如v3.10.3-alpha.11',
    `major`           INT         NOT NULL                COMMENT '主版本号',
    `minor`           INT         NOT NULL                COMMENT '小版本号',
    `patch`           INT         NOT NULL                COMMENT '修订号',
    `pre_type`        VARCHAR(16) NOT NULL DEFAULT ''     COMMENT '先行版本类型：空串-稳定版，alpha/beta/rc',
    `pre_rank`        TINYINT     NOT NULL                COMMENT '类型序：1-alpha，2-beta，3-rc，4-stable，值越大版本越新',
    `pre_num`         INT         NOT NULL DEFAULT 0      COMMENT '先行版本号，从1开始，稳定版为0',
    `create_time`     BIGINT      NOT NULL                COMMENT '入库时间（毫秒时间戳，由应用层传入）',
    `row_create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '行创建时间',
    `row_update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '行更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tag` (`tag`),
    KEY `idx_series` (`major`, `minor`, `patch`, `pre_rank`, `pre_num`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = '代码仓库Tag表';
