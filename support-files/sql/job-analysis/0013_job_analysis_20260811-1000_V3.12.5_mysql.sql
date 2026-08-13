SET NAMES utf8mb4;
USE job_analysis;

DROP PROCEDURE IF EXISTS job_schema_update;

DELIMITER <JOB_UBF>

CREATE PROCEDURE job_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.TABLES
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'approval_task') THEN
        CREATE TABLE IF NOT EXISTS `approval_task` (
            `id`                    bigint(20)          NOT NULL AUTO_INCREMENT,
            `approval_task_id`      varchar(32)         NOT NULL COMMENT '对外暴露的审批任务ID(32位UUID，无连字符，不可猜测)',
            `tenant_id`             varchar(32)         NOT NULL COMMENT '租户ID',
            `app_id`                bigint(20)          NOT NULL COMMENT '业务ID',
            `operation_type`        varchar(64)         NOT NULL COMMENT '操作类型: FAST_EXECUTE_SCRIPT/FAST_TRANSFER_FILE/EXECUTE_JOB_PLAN/CREATE_JOB_PLAN/SAVE_CRON/UPDATE_CRON_STATUS',
            `operation_params`      mediumtext          NOT NULL COMMENT '操作参数快照(JSON，敏感字段已加密)，仅insert时写入，不提供update',
            `resolved_summary`      mediumtext          NULL COMMENT 'dryRun解析出的概要(JSON)：实际目标机、账号、脚本版本、高危命中等',
            `creator`               varchar(128)        NOT NULL COMMENT '发起人，放行时校验 approver==creator',
            `app_code`              varchar(128)        NOT NULL DEFAULT '' COMMENT '发起方appCode',
            `approval_channel`      varchar(32)         NOT NULL COMMENT '审批渠道枚举: IMATE/...',
            `approval_ticket_id`    varchar(256)        NULL COMMENT '审批渠道单据ID，首次回查确认绑定后写入，此后不可更换',
            `ticket_fetched_at`     bigint(20) UNSIGNED NULL COMMENT '审批渠道拉取审批内容的时间(毫秒)，仅作观测，不参与放行校验',
            `status`                varchar(32)         NOT NULL COMMENT '状态: PENDING/EXECUTING/EXECUTED/REJECTED/CANCELED/FAILED',
            `approver`              varchar(128)        NULL COMMENT '审批人(来自回查响应)',
            `approved_at`           bigint(20) UNSIGNED NULL COMMENT '审批通过时间(毫秒，来自回查响应)',
            `execute_result`        mediumtext          NULL COMMENT '放行后的操作结果(JSON)：作业实例ID/执行方案ID/定时任务ID与状态等',
            `expire_at`             bigint(20) UNSIGNED NOT NULL COMMENT '过期时刻(毫秒) = create_time + TTL',
            `consumed_at`           bigint(20) UNSIGNED NULL COMMENT '被消费(CAS成功)时刻(毫秒)',
            `dispatched_at`         bigint(20) UNSIGNED NULL COMMENT '下发下游执行请求的时刻(毫秒)，用于区分"未下发"与"已下发结果未知"',
            `create_time`           bigint(20) UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建时间(毫秒)',
            `row_create_time`       datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP,
            `row_update_time`       datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            PRIMARY KEY (`id`) USING BTREE,
            UNIQUE KEY `uk_approval_task_id` (`approval_task_id`) USING BTREE,
            INDEX `idx_app_creator_status` (`app_id`, `creator`, `status`) USING BTREE,
            INDEX `idx_create_time` (`create_time`) USING BTREE,
            INDEX `idx_expire_at` (`expire_at`) USING BTREE
        ) ENGINE = InnoDB CHARACTER SET = utf8mb4;
    END IF;

COMMIT;
END <JOB_UBF>
DELIMITER ;
CALL job_schema_update();

DROP PROCEDURE IF EXISTS job_schema_update;
