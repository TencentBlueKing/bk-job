SET NAMES utf8mb4;
USE job_manage;

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
                    AND TABLE_NAME = 'host_topo'
                    AND COLUMN_NAME = 'app_id') THEN
        ALTER TABLE host_topo ADD COLUMN app_id BIGINT(20) UNSIGNED;
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'host_topo'
                    AND INDEX_NAME = 'idx_app_id') THEN
        ALTER TABLE host_topo ADD INDEX idx_app_id(app_id);
    END IF;


    COMMIT;
END <JOB_UBF>
DELIMITER ;
CALL job_schema_update();

DROP PROCEDURE IF EXISTS job_schema_update;
