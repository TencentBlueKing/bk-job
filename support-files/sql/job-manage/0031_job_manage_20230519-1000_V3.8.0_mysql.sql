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
                  FROM information_schema.columns
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'task_template_step_script'
                    AND COLUMN_NAME = 'secure_param_encrypt_algorithm') THEN
    ALTER TABLE task_template_step_script ADD COLUMN secure_param_encrypt_algorithm varchar(32) NOT NULL DEFAULT "None";
  END IF;

  IF NOT EXISTS(SELECT 1
                  FROM information_schema.columns
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'task_plan_step_script'
                    AND COLUMN_NAME = 'secure_param_encrypt_algorithm') THEN
    ALTER TABLE task_plan_step_script ADD COLUMN secure_param_encrypt_algorithm varchar(32) NOT NULL DEFAULT "None";
  END IF;

  IF NOT EXISTS(SELECT 1
                  FROM information_schema.columns
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'task_template_variable'
                    AND COLUMN_NAME = 'cipher_encrypt_algorithm') THEN
    ALTER TABLE task_template_variable ADD COLUMN cipher_encrypt_algorithm varchar(32) NOT NULL DEFAULT "None";
  END IF;

  IF NOT EXISTS(SELECT 1
                  FROM information_schema.columns
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'task_plan_variable'
                    AND COLUMN_NAME = 'cipher_encrypt_algorithm') THEN
    ALTER TABLE task_plan_variable ADD COLUMN cipher_encrypt_algorithm varchar(32) NOT NULL DEFAULT "None";
  END IF;

COMMIT;
END <JOB_UBF>
DELIMITER ;
CALL job_schema_update();

DROP PROCEDURE IF EXISTS job_schema_update;
