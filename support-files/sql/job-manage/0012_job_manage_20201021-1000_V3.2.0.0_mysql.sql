SET NAMES utf8mb4;
USE job_manage;

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
                    AND TABLE_NAME = 'task_template_step_file_list'
                    AND COLUMN_NAME = 'file_source_id') THEN
        ALTER TABLE `job_manage`.`task_template_step_file_list` ADD COLUMN file_source_id INT(11);
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'task_plan_step_file_list'
                    AND COLUMN_NAME = 'file_source_id') THEN
        ALTER TABLE `job_manage`.`task_plan_step_file_list` ADD COLUMN file_source_id INT(11);
    END IF;

    COMMIT;
END <JOB_UBF>
DELIMITER ;
CALL job_schema_update();

DROP PROCEDURE IF EXISTS job_schema_update;
