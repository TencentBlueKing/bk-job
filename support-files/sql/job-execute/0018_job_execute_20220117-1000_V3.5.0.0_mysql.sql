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
              FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'step_instance'
                AND COLUMN_NAME = 'batch') THEN
        ALTER TABLE step_instance ADD COLUMN `batch` smallint(6) NOT NULL DEFAULT '0' AFTER execute_count;
    END IF;
	
	IF NOT EXISTS(SELECT 1
              FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'step_instance'
                AND COLUMN_NAME = 'rolling_config_id') THEN
        ALTER TABLE step_instance ADD COLUMN `rolling_config_id` bigint(20);
    END IF;
	
    IF NOT EXISTS(SELECT 1
              FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'gse_task_ip_log'
                AND COLUMN_NAME = 'batch') THEN
        ALTER TABLE gse_task_ip_log ADD COLUMN `batch` smallint(6) NOT NULL DEFAULT '0' AFTER execute_count;
    END IF;
	
    IF NOT EXISTS(SELECT 1
              FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'gse_task_log'
                AND COLUMN_NAME = 'batch') THEN
        ALTER TABLE gse_task_log ADD COLUMN `batch` smallint(6) NOT NULL DEFAULT '0' AFTER execute_count;
    END IF;
	
	IF EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'gse_task_log'
                    AND INDEX_NAME = 'PRIMARY'
                    AND COLUMN_NAME= 'step_instance_id'
        ) AND EXISTS(SELECT 1
                     FROM information_schema.statistics
                     WHERE TABLE_SCHEMA = db
                       AND TABLE_NAME = 'gse_task_log'
                       AND INDEX_NAME = 'PRIMARY'
                       AND COLUMN_NAME= 'execute_count'
        ) THEN
        ALTER TABLE gse_task_log ADD UNIQUE INDEX `uk_step_id_execute_count_batch` (`step_instance_id`,`execute_count`,`batch`);
        ALTER TABLE gse_task_log DROP PRIMARY KEY;
        ALTER TABLE gse_task_log ADD COLUMN `id` bigint(20) NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;
    END IF;

    COMMIT;
END <JOB_UBF>
DELIMITER ;
COMMIT;
CALL job_schema_update();

DROP PROCEDURE IF EXISTS job_schema_update;
