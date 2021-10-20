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
                    AND TABLE_NAME = 'step_instance'
                    AND COLUMN_NAME = 'step_num') THEN
        ALTER TABLE step_instance ADD COLUMN step_num INT(11) NOT NULL DEFAULT 1;
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.columns
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'step_instance'
                    AND COLUMN_NAME = 'step_order') THEN
        ALTER TABLE step_instance ADD COLUMN step_order INT(11) NOT NULL DEFAULT 1;
    END IF;
	
    COMMIT;
END <JOB_UBF>
DELIMITER ;
COMMIT;
CALL job_schema_update();

DROP PROCEDURE IF EXISTS job_schema_update;