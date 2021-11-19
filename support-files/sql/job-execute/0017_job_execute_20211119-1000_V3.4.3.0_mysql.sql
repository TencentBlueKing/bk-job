USE job_execute;

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS job_schema_update;

DELIMITER <JOB_UBF>

CREATE PROCEDURE job_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

    IF EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'file_source_task_log'
                    AND INDEX_NAME = 'PRIMARY'
                    AND COLUMN_NAME= 'step_instance_id'
        ) AND EXISTS(SELECT 1
                     FROM information_schema.statistics
                     WHERE TABLE_SCHEMA = db
                       AND TABLE_NAME = 'file_source_task_log'
                       AND INDEX_NAME = 'PRIMARY'
                       AND COLUMN_NAME= 'execute_count'
        ) THEN
        ALTER TABLE file_source_task_log DROP PRIMARY KEY;
        ALTER TABLE file_source_task_log ADD COLUMN id BIGINT AUTO_INCREMENT PRIMARY KEY FIRST ;
        ALTER TABLE file_source_task_log ADD INDEX idx_step_instance_id_execute_count(`step_instance_id`,`execute_count`);
    END IF;

    COMMIT;
END <JOB_UBF>
DELIMITER ;
COMMIT;
CALL job_schema_update();

DROP PROCEDURE IF EXISTS job_schema_update;
