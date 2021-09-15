SET NAMES utf8mb4;
USE job_crontab;

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
                    AND TABLE_NAME = 'cron_job_history'
                    AND COLUMN_NAME = 'executor') THEN
        ALTER TABLE `job_crontab`.`cron_job_history` ADD COLUMN `executor` varchar(255) CHARACTER SET utf8mb4 NULL DEFAULT NULL;
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'cron_job_history'
                    AND COLUMN_NAME = 'error_code') THEN
        ALTER TABLE `job_crontab`.`cron_job_history` ADD COLUMN `error_code` bigint(20) UNSIGNED NULL DEFAULT NULL;
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'cron_job_history'
                    AND COLUMN_NAME = 'error_message') THEN
        ALTER TABLE `job_crontab`.`cron_job_history` ADD COLUMN `error_message` varchar(255) CHARACTER SET utf8mb4 NULL DEFAULT NULL;
    END IF;

COMMIT;
END <JOB_UBF>
DELIMITER ;
CALL job_schema_update();

DROP PROCEDURE IF EXISTS job_schema_update;
