USE job_manage;

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS job_schema_update;

DELIMITER <JOB_UBF>

CREATE PROCEDURE job_schema_update()
BEGIN

  DECLARE db VARCHAR(100);
  SET AUTOCOMMIT = 0;
  SELECT DATABASE() INTO db;

  IF NOT EXISTS(SELECT 1
                  FROM information_schema.columns
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'task_plan_variable'
                    AND COLUMN_NAME = 'is_follow_template') THEN
    ALTER TABLE task_plan_variable
        ADD COLUMN `is_follow_template` TINYINT(1) UNSIGNED NOT NULL DEFAULT 0 AFTER `is_required`;
  END IF;

COMMIT;
END <JOB_UBF>
DELIMITER ;
CALL job_schema_update();
DROP PROCEDURE IF EXISTS job_schema_update;
