USE job_manage;

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS job_schema_update;

DELIMITER <JOB_UBF>

CREATE PROCEDURE job_schema_update()
BEGIN

  DECLARE db VARCHAR(100);
  SET AUTOCOMMIT = 0;
  SELECT DATABASE() INTO db;

  IF NOT EXISTS(SELECT 1
                  FROM information_schema.columns
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'application'
                    AND COLUMN_NAME = 'tenant_id') THEN
    ALTER TABLE application ADD COLUMN tenant_id VARCHAR(32) NOT NULL DEFAULT 'default';
  END IF;

  IF NOT EXISTS(SELECT 1
                FROM information_schema.statistics
                WHERE TABLE_SCHEMA = db
                  AND TABLE_NAME = 'application'
                  AND INDEX_NAME = 'idx_tenant_id') THEN
    ALTER TABLE application ADD INDEX idx_tenant_id(`tenant_id`);
  END IF;

  CREATE TABLE IF NOT EXISTS `user` (
      `username` varchar(64) NOT NULL,
      `tenant_id` varchar(32) NOT NULL,
      `display_name` varchar(128) DEFAULT NULL,
      `row_update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
      `last_modify_time` bigint(20) unsigned DEFAULT NULL,
      PRIMARY KEY (`username`) USING BTREE,
      KEY `idx_tenant_id` (`tenant_id`) USING BTREE,
      KEY `idx_display_name` (`display_name`) USING BTREE
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

  IF NOT EXISTS(SELECT 1
                    FROM information_schema.columns
                    WHERE TABLE_SCHEMA = db
                      AND TABLE_NAME = 'dangerous_rule'
                      AND COLUMN_NAME = 'tenant_id') THEN
    ALTER TABLE dangerous_rule ADD COLUMN tenant_id VARCHAR(32) NOT NULL DEFAULT 'default';
  END IF;

  IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'dangerous_rule'
                    AND INDEX_NAME = 'idx_tenant_id') THEN
    ALTER TABLE dangerous_rule ADD INDEX idx_tenant_id(`tenant_id`);
  END IF;

  IF NOT EXISTS(SELECT 1
                    FROM information_schema.columns
                    WHERE TABLE_SCHEMA = db
                      AND TABLE_NAME = 'script'
                      AND COLUMN_NAME = 'tenant_id') THEN
    ALTER TABLE script ADD COLUMN tenant_id VARCHAR(32) NOT NULL DEFAULT 'default';
  END IF;

  IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'script'
                    AND INDEX_NAME = 'idx_tenant_id') THEN
    ALTER TABLE script ADD INDEX idx_tenant_id(`tenant_id`);
  END IF;

  IF NOT EXISTS(SELECT 1
                    FROM information_schema.columns
                    WHERE TABLE_SCHEMA = db
                      AND TABLE_NAME = 'white_ip_record'
                      AND COLUMN_NAME = 'tenant_id') THEN
    ALTER TABLE white_ip_record ADD COLUMN tenant_id VARCHAR(32) NOT NULL DEFAULT 'default' AFTER `id`;
  END IF;

  IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'white_ip_record'
                    AND INDEX_NAME = 'idx_tenant_id') THEN
    ALTER TABLE white_ip_record ADD INDEX idx_tenant_id(`tenant_id`);
  END IF;

  IF NOT EXISTS(SELECT 1
                    FROM information_schema.columns
                    WHERE TABLE_SCHEMA = db
                      AND TABLE_NAME = 'application'
                      AND COLUMN_NAME = 'default') THEN
    ALTER TABLE application ADD COLUMN `default` int(10) NOT NULL DEFAULT 0;
  END IF;

COMMIT;
END <JOB_UBF>
DELIMITER ;
CALL job_schema_update();

DROP PROCEDURE IF EXISTS job_schema_update;
