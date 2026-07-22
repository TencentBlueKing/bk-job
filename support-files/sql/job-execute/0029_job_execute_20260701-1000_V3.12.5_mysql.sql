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

  -- step_instance_rolling_task 增加并行错峰下发相关列（兼容旧数据）
  IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'step_instance_rolling_task'
                    AND COLUMN_NAME = 'dispatch_time') THEN
    ALTER TABLE step_instance_rolling_task ADD COLUMN `dispatch_time` bigint(20) DEFAULT NULL AFTER `total_time`;
  END IF;

  IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'step_instance_rolling_task'
                    AND COLUMN_NAME = 'dispatched') THEN
    ALTER TABLE step_instance_rolling_task ADD COLUMN `dispatched` tinyint(1) NOT NULL DEFAULT '0' AFTER `dispatch_time`;
  END IF;

  COMMIT;
END <JOB_UBF>
DELIMITER ;
COMMIT;

CALL job_schema_update();

DROP PROCEDURE IF EXISTS job_schema_update;
