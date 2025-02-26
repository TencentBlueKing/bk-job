USE job_execute;

SET NAMES utf8mb4;

-- 更新 schema
DROP PROCEDURE IF EXISTS job_schema_update;

DELIMITER <JOB_UBF>

CREATE PROCEDURE job_schema_update()
BEGIN

  DECLARE db VARCHAR(100);
  SET AUTOCOMMIT = 0;
  SELECT DATABASE() INTO db;

  CREATE TABLE IF NOT EXISTS `task_instance_app` (
    `id` bigint(20) NOT NULL,
    `app_id` bigint(20) NOT NULL,
    `task_id` bigint(20) NOT NULL DEFAULT '0',
    `task_template_id` bigint(20) NOT NULL DEFAULT '0',
    `name` varchar(512) NOT NULL,
    `type` tinyint(4) NOT NULL,
    `operator` varchar(128) NOT NULL,
    `status` tinyint(4) NOT NULL DEFAULT '0',
    `current_step_id` bigint(20) NOT NULL DEFAULT '0',
    `startup_mode` tinyint(4) NOT NULL,
    `total_time` bigint(20) DEFAULT NULL,
    `callback_url` varchar(1024) DEFAULT NULL,
    `is_debug_task` tinyint(4) NOT NULL DEFAULT '0',
    `cron_task_id` bigint(20) NOT NULL DEFAULT '0',
    `create_time` bigint(20) DEFAULT NULL,
    `start_time` bigint(20) DEFAULT NULL,
    `end_time` bigint(20) DEFAULT NULL,
    `app_code` varchar(128) DEFAULT NULL,
    `row_create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `row_update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_app_create_time` (`app_id`,`create_time`) USING BTREE,
    KEY `idx_operator_app_id_create_time` (`operator`,`app_id`,`create_time`) USING BTREE,
    KEY `idx_app_id_status_create_time` (`app_id`,`status`,`create_time`) USING BTREE,
    KEY `idx_app_id_task_id_create_time` (`app_id`,`task_id`,`create_time`) USING BTREE,
    KEY `idx_app_id_cron_task_id_create_time` (`app_id`,`cron_task_id`,`create_time`) USING BTREE,
    KEY `idx_app_id_app_code_create_time` (`app_id`,`app_code`,`create_time`) USING BTREE,
    KEY `idx_create_time` (`create_time`) USING BTREE
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

  COMMIT;
END <JOB_UBF>
DELIMITER ;
COMMIT;

CALL job_schema_update();

DROP PROCEDURE IF EXISTS job_schema_update;