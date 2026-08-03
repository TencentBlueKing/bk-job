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

  -- step_instance_script
  IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'step_instance_script'
                    AND COLUMN_NAME = 'account_var') THEN
    ALTER TABLE step_instance_script ADD COLUMN `account_var` varchar(255) DEFAULT NULL AFTER `system_account`;
  END IF;

  -- step_instance_file
  IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'step_instance_file'
                    AND COLUMN_NAME = 'account_var') THEN
    ALTER TABLE step_instance_file ADD COLUMN `account_var` varchar(255) DEFAULT NULL AFTER `system_account`;
  END IF;

  COMMIT;
END <JOB_UBF>
DELIMITER ;
COMMIT;

CALL job_schema_update();

DROP PROCEDURE IF EXISTS job_schema_update;
