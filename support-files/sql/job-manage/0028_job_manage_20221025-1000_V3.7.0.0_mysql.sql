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
                    AND COLUMN_NAME = 'ip_v6') THEN
    ALTER TABLE host ADD COLUMN ip_v6 VARCHAR(46) DEFAULT NULL;
  END IF;
  
  IF NOT EXISTS(SELECT 1
                  FROM information_schema.columns
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'host'
                    AND COLUMN_NAME = 'agent_id') THEN
    ALTER TABLE host ADD COLUMN agent_id VARCHAR(64) DEFAULT NULL;
  END IF;

  -- Update `white_ip_ip` schema
  IF NOT EXISTS(SELECT 1
                FROM information_schema.columns
                WHERE TABLE_SCHEMA = db
                  AND TABLE_NAME = 'white_ip_ip'
                  AND COLUMN_NAME = 'ip_v6') THEN
      ALTER TABLE white_ip_ip ADD COLUMN ip_v6 VARCHAR(46) DEFAULT NULL AFTER `ip`;
  END IF;

  IF NOT EXISTS(SELECT 1
                FROM information_schema.columns
                WHERE TABLE_SCHEMA = db
                  AND TABLE_NAME = 'white_ip_ip'
                  AND COLUMN_NAME = 'host_id') THEN
      ALTER TABLE white_ip_ip ADD COLUMN host_id BIGINT(20) DEFAULT NULL AFTER `record_id`;
  END IF;

  IF NOT EXISTS(SELECT 1
                FROM information_schema.statistics
                WHERE TABLE_SCHEMA = db
                  AND TABLE_NAME = 'white_ip_ip'
                  AND INDEX_NAME = 'idx_host_id') THEN
      ALTER TABLE white_ip_ip ADD INDEX idx_host_id(`host_id`);
  END IF;

COMMIT;
END <JOB_UBF>
DELIMITER ;
CALL job_schema_update();

DROP PROCEDURE IF EXISTS job_schema_update;
