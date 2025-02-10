USE job_manage;

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS job_schema_update;

DELIMITER <JOB_UBF>

CREATE PROCEDURE job_schema_update()
BEGIN

  DECLARE db VARCHAR(100);
  SET AUTOCOMMIT = 0;
  SELECT DATABASE() INTO db;
  
  -- Update `task_template_step_script` schema
  IF NOT EXISTS(SELECT 1
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'task_template_step_script'
                AND COLUMN_NAME = 'windows_interpreter') THEN
  ALTER TABLE task_template_step_script ADD COLUMN `windows_interpreter` varchar(260) NULL DEFAULT NULL ;
  END IF;

  -- Update `task_plan_step_script` schema
  IF NOT EXISTS(SELECT 1
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'task_plan_step_script'
                AND COLUMN_NAME = 'windows_interpreter') THEN
  ALTER TABLE task_plan_step_script ADD COLUMN `windows_interpreter` varchar(260) NULL DEFAULT NULL ;
  END IF;

COMMIT;
END <JOB_UBF>
DELIMITER ;
CALL job_schema_update();

DROP PROCEDURE IF EXISTS job_schema_update;
