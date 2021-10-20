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
                AND TABLE_NAME = 'step_instance_variable'
                AND COLUMN_NAME = 'execute_count') THEN
        ALTER TABLE step_instance_variable ADD COLUMN `execute_count` int(11) NOT NULL DEFAULT '0' AFTER `step_instance_id`;
    END IF;
        
    IF EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'step_instance_variable'
                    AND INDEX_NAME = 'uk_step_instance_id_type') THEN
        ALTER TABLE step_instance_variable DROP INDEX `uk_step_instance_id_type`;
    END IF;
        
    IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'step_instance_variable'
                    AND INDEX_NAME = 'uk_step_instance_id_execute_count_type') THEN
        ALTER TABLE step_instance_variable ADD UNIQUE INDEX `uk_step_instance_id_execute_count_type` (`step_instance_id`,`execute_count`,`type`);
    END IF;

    COMMIT;
END <JOB_UBF>
DELIMITER ;
COMMIT;
CALL job_schema_update();

DROP PROCEDURE IF EXISTS job_schema_update;