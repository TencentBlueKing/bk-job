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
                    AND TABLE_NAME = 'ai_chat_session') THEN
        CREATE TABLE IF NOT EXISTS `ai_chat_session` (
            `id`                bigint(20)          NOT NULL AUTO_INCREMENT,
            `app_id`            bigint(20)          NOT NULL COMMENT '业务ID',
            `username`          varchar(64)         NOT NULL COMMENT '用户名',
            `scene_type`        tinyint(4)          NOT NULL COMMENT '场景类型: 1-任务报错分析, 2-脚本管理, 3-自由对话',
            `scene_resource_id` varchar(128)        NOT NULL DEFAULT '' COMMENT '场景资源标识(stepInstanceId/scriptId等)',
            `ai_session_id`     varchar(256)        NOT NULL COMMENT 'AI智能体会话ID',
            `session_name`      varchar(512)        NOT NULL COMMENT '会话名称',
            `create_time`       bigint(20) UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建时间',
            `update_time`       bigint(20) UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新时间',
            `row_create_time`   datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP,
            `row_update_time`   datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            PRIMARY KEY (`id`) USING BTREE,
            UNIQUE KEY `uk_app_user_scene` (`app_id`, `username`, `scene_type`, `scene_resource_id`) USING BTREE,
            INDEX `idx_username` (`username`) USING BTREE,
            INDEX `idx_create_time` (`create_time`) USING BTREE
        ) ENGINE = InnoDB CHARACTER SET = utf8mb4;
    END IF;

COMMIT;
END <JOB_UBF>
DELIMITER ;
CALL job_schema_update();

DROP PROCEDURE IF EXISTS job_schema_update;
