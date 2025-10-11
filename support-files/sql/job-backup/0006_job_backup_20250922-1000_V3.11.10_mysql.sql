USE `job_backup`;

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS job_schema_update;

DELIMITER <JOB_UBF>

CREATE PROCEDURE job_schema_update()
BEGIN

  DECLARE db VARCHAR(100);
  SET AUTOCOMMIT = 0;
  SELECT DATABASE() INTO db;

  IF NOT EXISTS(SELECT 1
                FROM information_schema.statistics
                WHERE TABLE_SCHEMA = db
                  AND TABLE_NAME = 'archive_task'
                  AND INDEX_NAME = 'idx_task_type_day_hour') THEN
    ALTER TABLE archive_task ADD INDEX `idx_task_type_day_hour` (`task_type`, `day`, `hour`);
  END IF;
  commit;

END <JOB_UBF>

DELIMITER ;
CALL job_schema_update();

DROP PROCEDURE IF EXISTS job_schema_update;


