USE job_manage;

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS job_schema_update;

DELIMITER <JOB_UBF>

CREATE PROCEDURE job_schema_update()
BEGIN

  DECLARE db VARCHAR(100);
  SET AUTOCOMMIT = 0;
  SELECT DATABASE() INTO db;
  ALTER TABLE task_template_step_script MODIFY COLUMN `execute_account` bigint(20) UNSIGNED NULL DEFAULT NULL;

  ALTER TABLE task_plan_step_script MODIFY COLUMN `execute_account` bigint(20) UNSIGNED NULL DEFAULT NULL;
  ALTER TABLE task_template_step_file MODIFY COLUMN `execute_account` bigint(20) UNSIGNED NULL DEFAULT NULL;
  ALTER TABLE task_plan_step_file MODIFY COLUMN `execute_account` bigint(20) UNSIGNED NULL DEFAULT NULL;
  ALTER TABLE task_template_step_file_list MODIFY COLUMN `host_account` bigint(20) UNSIGNED NULL DEFAULT NULL;
  ALTER TABLE task_plan_step_file_list MODIFY COLUMN `host_account` bigint(20) UNSIGNED NULL DEFAULT NULL;

  IF NOT EXISTS(SELECT 1
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'task_template_step_script'
                AND COLUMN_NAME = 'execute_account_var') THEN
  ALTER TABLE task_template_step_script ADD COLUMN `execute_account_var` varchar(255) NULL DEFAULT NULL AFTER `execute_account`;
  END IF;

  IF NOT EXISTS(SELECT 1
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'task_plan_step_script'
                AND COLUMN_NAME = 'execute_account_var') THEN
  ALTER TABLE task_plan_step_script ADD COLUMN `execute_account_var` varchar(255) NULL DEFAULT NULL AFTER `execute_account`;
  END IF;

  IF NOT EXISTS(SELECT 1
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'task_template_step_file'
                AND COLUMN_NAME = 'execute_account_var') THEN
  ALTER TABLE task_template_step_file ADD COLUMN `execute_account_var` varchar(255) NULL DEFAULT NULL AFTER `execute_account`;
  END IF;

  IF NOT EXISTS(SELECT 1
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'task_plan_step_file'
                AND COLUMN_NAME = 'execute_account_var') THEN
  ALTER TABLE task_plan_step_file ADD COLUMN `execute_account_var` varchar(255) NULL DEFAULT NULL AFTER `execute_account`;
  END IF;

  IF NOT EXISTS(SELECT 1
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'task_template_step_file_list'
                AND COLUMN_NAME = 'host_account_var') THEN
  ALTER TABLE task_template_step_file_list ADD COLUMN `host_account_var` varchar(255) NULL DEFAULT NULL AFTER `host_account`;
  END IF;

  IF NOT EXISTS(SELECT 1
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'task_plan_step_file_list'
                AND COLUMN_NAME = 'host_account_var') THEN
  ALTER TABLE task_plan_step_file_list ADD COLUMN `host_account_var` varchar(255) NULL DEFAULT NULL AFTER `host_account`;
  END IF;

COMMIT;
END <JOB_UBF>
DELIMITER ;
CALL job_schema_update();

DROP PROCEDURE IF EXISTS job_schema_update;
