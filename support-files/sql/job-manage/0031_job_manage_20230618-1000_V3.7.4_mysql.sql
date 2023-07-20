USE job_manage;

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS job_schema_update;

DELIMITER <JOB_UBF>

CREATE PROCEDURE job_schema_update()
BEGIN

  DECLARE db VARCHAR(100);
  SET AUTOCOMMIT = 0;
  SELECT DATABASE() INTO db;
  
  -- Update `host` schema
  IF NOT EXISTS(SELECT 1
                  FROM information_schema.columns
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'host'
                    AND COLUMN_NAME = 'last_time') THEN
    ALTER TABLE host ADD COLUMN last_time BIGINT(20) NOT NULL DEFAULT 0 COMMENT 'host last time from CMDB';
  END IF;

  -- Update `host_topo` schema
  IF NOT EXISTS(SELECT 1
                  FROM information_schema.columns
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'host_topo'
                    AND COLUMN_NAME = 'last_time') THEN
    ALTER TABLE host_topo ADD COLUMN last_time BIGINT(20) NOT NULL DEFAULT 0 COMMENT 'host topo last time from CMDB' AFTER `app_id`;
  END IF;

COMMIT;
END <JOB_UBF>
DELIMITER ;
CALL job_schema_update();

DROP PROCEDURE IF EXISTS job_schema_update;
