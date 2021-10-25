use job_crontab;

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
                    AND TABLE_NAME = 'cron_job'
                    AND COLUMN_NAME = 'last_execute_error_code') THEN
        ALTER TABLE cron_job ADD COLUMN `last_execute_error_code` bigint(20) UNSIGNED NULL DEFAULT NULL;
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'cron_job'
                    AND COLUMN_NAME = 'last_execute_error_count') THEN
        ALTER TABLE cron_job ADD COLUMN `last_execute_error_count` int(11) UNSIGNED NOT NULL DEFAULT '0';
    END IF;

    COMMIT;
END <JOB_UBF>
DELIMITER ;
COMMIT;
CALL job_schema_update();

DROP PROCEDURE IF EXISTS job_schema_update;