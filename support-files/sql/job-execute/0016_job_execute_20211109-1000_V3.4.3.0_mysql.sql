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
                AND TABLE_NAME = 'step_instance_file'
                AND COLUMN_NAME = 'file_target_name') THEN
        ALTER TABLE step_instance_file ADD COLUMN `file_target_name` varchar(512) DEFAULT NULL AFTER `file_target_path`;
    END IF;
        
    COMMIT;
END <JOB_UBF>
DELIMITER ;
COMMIT;
CALL job_schema_update();

DROP PROCEDURE IF EXISTS job_schema_update;