use job_crontab;

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS job_schema_update;

DELIMITER <JOB_UBF>

CREATE PROCEDURE job_schema_update()
BEGIN
  DECLARE db VARCHAR(100);
  SET AUTOCOMMIT = 0;
  SELECT DATABASE() INTO db;

  IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'cron_job'
                    AND COLUMN_NAME = 'notify_type') THEN
    ALTER TABLE cron_job ADD COLUMN `notify_type` tinyint(4) UNSIGNED NOT NULL DEFAULT '1' COMMENT '通知配置';
  END IF;

  IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'cron_job'
                    AND COLUMN_NAME = 'custom_notify_role') THEN
    ALTER TABLE cron_job ADD COLUMN `custom_notify_role` varchar(255) CHARACTER SET utf8mb4 NULL DEFAULT NULL COMMENT '自定义通知角色';
  END IF;

  IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'cron_job'
                    AND COLUMN_NAME = 'custom_extra_observer') THEN
    ALTER TABLE cron_job ADD COLUMN `custom_extra_observer` text CHARACTER SET utf8mb4 NULL DEFAULT NULL COMMENT '自定义额外通知人';
  END IF;

  IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'cron_job'
                    AND COLUMN_NAME = 'custom_notify_trigger') THEN
    ALTER TABLE cron_job ADD COLUMN `custom_notify_trigger` text CHARACTER SET utf8mb4 NULL DEFAULT NULL COMMENT '自定义通知时机';
  END IF;

  IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'notify_policy_role_target'
                    AND INDEX_NAME = 'idx_policy_id') THEN
    ALTER TABLE notify_policy_role_target ADD INDEX `idx_policy_id` (policy_id);
  END IF;

  IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'notify_role_target_channel'
                    AND INDEX_NAME = 'idx_role_target_id') THEN
    ALTER TABLE notify_role_target_channel ADD INDEX `idx_role_target_id` (role_target_id);
  END IF;

COMMIT;
END <JOB_UBF>
DELIMITER ;
CALL job_schema_update();

DROP PROCEDURE IF EXISTS job_schema_update;

