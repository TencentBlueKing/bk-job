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
  IF EXISTS(SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = db
            AND TABLE_NAME = 'host'
            AND COLUMN_NAME = 'cloud_area_id') THEN
  ALTER TABLE host MODIFY COLUMN `cloud_area_id` BIGINT(20) UNSIGNED NULL DEFAULT NULL;
  END IF;

  IF NOT EXISTS(SELECT 1
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'host'
                AND COLUMN_NAME = 'cloud_id') THEN
  ALTER TABLE host ADD COLUMN `cloud_id` BIGINT(20) NULL DEFAULT NULL AFTER `cloud_area_id`;
  END IF;

COMMIT;
END <JOB_UBF>
DELIMITER ;
CALL job_schema_update();
DROP PROCEDURE IF EXISTS job_schema_update;

-- 更新 cloud_id 列
DROP PROCEDURE IF EXISTS job_add_cloud_id;

DELIMITER <JOB_UBF>

CREATE PROCEDURE job_add_cloud_id()
label:BEGIN
  DECLARE minHostId BIGINT;
  DECLARE maxHostId BIGINT;
  DECLARE fromHostId BIGINT;
  DECLARE endHostId BIGINT;

  SET AUTOCOMMIT = 0;

  -- 如果 host 表为空，无需变更
  IF NOT EXISTS (SELECT 1 FROM host LIMIT 1) THEN
      LEAVE label;
  END IF;

  -- 如果 host 表中不存在cloud_id = 0，说明已经执行过该变更
  IF NOT EXISTS (SELECT 1 FROM host WHERE cloud_id IS NULL LIMIT 1) THEN
      LEAVE label;
  END IF;

  SELECT MIN(host_id), MAX(host_id) INTO minHostId, maxHostId FROM host;

  SET fromHostId = minHostId - 1;

  WHILE fromHostId <= maxHostId DO
    SELECT MIN(t.host_id),MAX(t.host_id) INTO fromHostId,endHostId FROM (SELECT host_id FROM host WHERE host_id > fromHostId AND host_id <= maxHostId ORDER BY host_id asc LIMIT 1000) t;

    UPDATE host t1
      SET t1.cloud_id = t1.cloud_area_id
      WHERE host_id BETWEEN fromHostId AND endHostId;

    COMMIT;

    SET fromHostId = endHostId;
  END WHILE;

END <JOB_UBF>
DELIMITER ;
COMMIT;

CALL job_add_cloud_id();
DROP PROCEDURE IF EXISTS job_add_cloud_id;

