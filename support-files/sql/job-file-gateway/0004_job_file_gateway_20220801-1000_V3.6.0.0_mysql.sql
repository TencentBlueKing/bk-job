SET NAMES utf8mb4;
USE job_file_gateway;

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
                    AND TABLE_NAME = 'file_source'
                    AND INDEX_NAME = 'uniq_code') THEN
        ALTER TABLE file_source DROP INDEX uniq_code;
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'file_source'
                    AND INDEX_NAME = 'uniq_appId_code') THEN
        ALTER TABLE file_source ADD UNIQUE INDEX uniq_appId_code(`app_id`,`code`);
    END IF;

COMMIT;
END <JOB_UBF>
DELIMITER ;
CALL job_schema_update();

DROP PROCEDURE IF EXISTS job_schema_update;
