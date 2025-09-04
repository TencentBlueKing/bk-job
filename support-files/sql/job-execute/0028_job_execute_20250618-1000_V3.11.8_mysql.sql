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

  -- rolling_config
  IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'rolling_config'
                    AND COLUMN_NAME = 'type') THEN
    ALTER TABLE rolling_config ADD COLUMN `type` tinyint(4) NOT NULL DEFAULT 1;
  END IF;

  -- step_instance_file_batch
  CREATE TABLE IF NOT EXISTS `step_instance_file_batch` (
      `id`               bigint(20) NOT NULL AUTO_INCREMENT,
      `task_instance_id` bigint(20) NOT NULL,
      `step_instance_id` bigint(20) NOT NULL,
      `batch`            smallint(6) NOT NULL DEFAULT '0',
      `file_source`      mediumtext NOT NULL,
      `row_create_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
      `row_update_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
      PRIMARY KEY (`id`),
      KEY `idx_task_instance_id` (`task_instance_id`),
      KEY `idx_step_instance_id_batch` (`step_instance_id`, `batch`)
    ) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

  COMMIT;
END <JOB_UBF>
DELIMITER ;
COMMIT;

CALL job_schema_update();

DROP PROCEDURE IF EXISTS job_schema_update;
