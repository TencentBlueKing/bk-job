USE job_execute;

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
                    AND TABLE_NAME = 'step_instance_confirm'
                    AND COLUMN_NAME = 'confirm_reason') THEN
        ALTER TABLE step_instance_confirm ADD COLUMN `confirm_reason` varchar(256) DEFAULT NULL;
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'task_instance'
                    AND INDEX_NAME = 'idx_app_cron_id') THEN
        ALTER TABLE task_instance ADD INDEX `idx_app_cron_id` (app_id,cron_task_id);
    END IF;

    IF EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'task_instance'
                    AND INDEX_NAME = 'idx_create_time_app_cron') THEN
        ALTER TABLE task_instance DROP INDEX idx_create_time_app_cron;
    END IF;
	
    COMMIT;
END <JOB_UBF>
DELIMITER ;
COMMIT;
CALL job_schema_update();

DROP PROCEDURE IF EXISTS job_schema_update;
