USE job_manage;

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS job_schema_update;

DELIMITER <JOB_UBF>

CREATE PROCEDURE job_schema_update()
BEGIN

  DECLARE db VARCHAR(100);
  SET AUTOCOMMIT = 0;
  SELECT DATABASE() INTO db;

  -- Update `host_topo` schema
  IF NOT EXISTS(SELECT 1
                  FROM information_schema.columns
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'host_topo'
                    AND COLUMN_NAME = 'create_time') THEN
    ALTER TABLE host_topo ADD COLUMN create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3);
  END IF;

END <JOB_UBF>
DELIMITER ;
COMMIT;
CALL job_schema_update();

DROP PROCEDURE IF EXISTS job_schema_update;
