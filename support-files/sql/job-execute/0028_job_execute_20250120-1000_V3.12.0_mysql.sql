USE job_execute;

SET NAMES utf8mb4;

-- 更新 schema
DROP PROCEDURE IF EXISTS job_schema_update;

DELIMITER <JOB_UBF>

CREATE PROCEDURE job_schema_update()
BEGIN

  DECLARE db VARCHAR(100);
  SET AUTOCOMMIT = 0;
  SELECT DATABASE() INTO db;

  -- dangerous_record
  IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'dangerous_record'
                    AND COLUMN_NAME = 'tenant_id') THEN
    ALTER TABLE dangerous_record ADD COLUMN `tenant_id` varchar(32) NOT NULL DEFAULT 'default';
  END IF;
  IF NOT EXISTS(SELECT 1
                FROM information_schema.statistics
                WHERE TABLE_SCHEMA = db
                  AND TABLE_NAME = 'dangerous_record'
                  AND INDEX_NAME = 'idx_tenant_id_ctime') THEN
      ALTER TABLE dangerous_record ADD INDEX `idx_tenant_id_ctime` (`tenant_id`,`create_time`);
  END IF;
  IF NOT EXISTS(SELECT 1
                FROM information_schema.statistics
                WHERE TABLE_SCHEMA = db
                  AND TABLE_NAME = 'dangerous_record'
                  AND INDEX_NAME = 'idx_rule_id_ctime') THEN
      ALTER TABLE dangerous_record ADD INDEX `idx_rule_id_ctime` (`rule_id`,`create_time`);
  END IF;
  IF NOT EXISTS(SELECT 1
                FROM information_schema.statistics
                WHERE TABLE_SCHEMA = db
                  AND TABLE_NAME = 'dangerous_record'
                  AND INDEX_NAME = 'idx_app_id_ctime') THEN
      ALTER TABLE dangerous_record ADD INDEX `idx_app_id_ctime` (`app_id`,`create_time`);
  END IF;
  IF NOT EXISTS(SELECT 1
                FROM information_schema.statistics
                WHERE TABLE_SCHEMA = db
                  AND TABLE_NAME = 'dangerous_record'
                  AND INDEX_NAME = 'idx_operator_ctime') THEN
      ALTER TABLE dangerous_record ADD INDEX `idx_operator_ctime` (`operator`,`create_time`);
  END IF;
  IF NOT EXISTS(SELECT 1
                FROM information_schema.statistics
                WHERE TABLE_SCHEMA = db
                  AND TABLE_NAME = 'dangerous_record'
                  AND INDEX_NAME = 'idx_client_ctime') THEN
      ALTER TABLE dangerous_record ADD INDEX `idx_client_ctime` (`client`,`create_time`);
  END IF;
  IF EXISTS(SELECT 1
                FROM information_schema.statistics
                WHERE TABLE_SCHEMA = db
                  AND TABLE_NAME = 'dangerous_record'
                  AND INDEX_NAME = 'idx_create_time_rule_id') THEN
      ALTER TABLE dangerous_record DROP INDEX `idx_create_time_rule_id`;
  END IF;
  IF EXISTS(SELECT 1
                FROM information_schema.statistics
                WHERE TABLE_SCHEMA = db
                  AND TABLE_NAME = 'dangerous_record'
                  AND INDEX_NAME = 'idx_create_time_rule_expression') THEN
      ALTER TABLE dangerous_record DROP INDEX `idx_create_time_rule_expression`;
  END IF;
  IF EXISTS(SELECT 1
                FROM information_schema.statistics
                WHERE TABLE_SCHEMA = db
                  AND TABLE_NAME = 'dangerous_record'
                  AND INDEX_NAME = 'idx_create_time_app_id') THEN
      ALTER TABLE dangerous_record DROP INDEX `idx_create_time_app_id`;
  END IF;
  IF EXISTS(SELECT 1
                FROM information_schema.statistics
                WHERE TABLE_SCHEMA = db
                  AND TABLE_NAME = 'dangerous_record'
                  AND INDEX_NAME = 'idx_create_time_operator') THEN
      ALTER TABLE dangerous_record DROP INDEX `idx_create_time_operator`;
  END IF;
  IF EXISTS(SELECT 1
                FROM information_schema.statistics
                WHERE TABLE_SCHEMA = db
                  AND TABLE_NAME = 'dangerous_record'
                  AND INDEX_NAME = 'idx_create_time_startup_mode') THEN
      ALTER TABLE dangerous_record DROP INDEX `idx_create_time_startup_mode`;
  END IF;
  IF EXISTS(SELECT 1
                FROM information_schema.statistics
                WHERE TABLE_SCHEMA = db
                  AND TABLE_NAME = 'dangerous_record'
                  AND INDEX_NAME = 'idx_create_time_client') THEN
      ALTER TABLE dangerous_record DROP INDEX `idx_create_time_client`;
  END IF;
  IF EXISTS(SELECT 1
                FROM information_schema.statistics
                WHERE TABLE_SCHEMA = db
                  AND TABLE_NAME = 'dangerous_record'
                  AND INDEX_NAME = 'idx_create_time_mode') THEN
      ALTER TABLE dangerous_record DROP INDEX `idx_create_time_mode`;
  END IF;
  COMMIT;
END <JOB_UBF>
DELIMITER ;
COMMIT;

CALL job_schema_update();

DROP PROCEDURE IF EXISTS job_schema_update;
