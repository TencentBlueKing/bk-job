USE job_manage;

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS job_schema_update;

DELIMITER <JOB_UBF>

CREATE PROCEDURE job_schema_update()
BEGIN

  DECLARE db VARCHAR(100);
  DECLARE current_primary_key VARCHAR(100);
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

  IF NOT EXISTS(SELECT 1
                FROM information_schema.columns
                WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'application'
                    AND COLUMN_NAME = 'default') THEN
    ALTER TABLE application ADD COLUMN `default` int(10) NOT NULL DEFAULT 0;
  END IF;

  ALTER TABLE application MODIFY COLUMN bk_supplier_account varchar(128) NULL DEFAULT NULL COMMENT '供应商账号，CMDB在多租户版本已废弃该字段';

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

  SELECT GROUP_CONCAT(COLUMN_NAME) INTO current_primary_key
    FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = db
      AND TABLE_NAME = 'user'
      AND CONSTRAINT_NAME = 'PRIMARY';

  IF current_primary_key = 'username' THEN
    ALTER TABLE user DROP PRIMARY KEY;
    ALTER TABLE user ADD PRIMARY KEY (`tenant_id`, `username`);
  END IF;

  IF NOT EXISTS(SELECT 1
                    FROM information_schema.columns
                    WHERE TABLE_SCHEMA = db
                      AND TABLE_NAME = 'notify_template'
                      AND COLUMN_NAME = 'tenant_id') THEN
    ALTER TABLE notify_template ADD COLUMN tenant_id VARCHAR(32) NOT NULL DEFAULT 'default' AFTER `id`;
  END IF;

  IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'notify_template'
                    AND INDEX_NAME = 'idx_tenant_id') THEN
    ALTER TABLE notify_template ADD INDEX idx_tenant_id(`tenant_id`);
  END IF;

  IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'notify_template'
                    AND INDEX_NAME = 'uniq_tenantId_code_channel_isDefault') THEN
    ALTER TABLE notify_template ADD UNIQUE INDEX uniq_tenantId_code_channel_isDefault(tenant_id,code,channel,is_default);
  END IF;

  IF EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'notify_template'
                    AND INDEX_NAME = 'uniq_code_channel_isDefault') THEN
    ALTER TABLE notify_template DROP INDEX uniq_code_channel_isDefault;
  END IF;

  IF NOT EXISTS(SELECT 1
                    FROM information_schema.columns
                    WHERE TABLE_SCHEMA = db
                      AND TABLE_NAME = 'global_setting'
                      AND COLUMN_NAME = 'tenant_id') THEN
    ALTER TABLE global_setting ADD COLUMN tenant_id VARCHAR(32) NOT NULL DEFAULT 'default';
  END IF;

  SELECT GROUP_CONCAT(COLUMN_NAME) INTO current_primary_key
    FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = db
      AND TABLE_NAME = 'global_setting'
      AND CONSTRAINT_NAME = 'PRIMARY';

  IF current_primary_key = 'key' OR current_primary_key = 'key,tenant_id' THEN
    ALTER TABLE global_setting DROP PRIMARY KEY;
    ALTER TABLE global_setting ADD PRIMARY KEY (`tenant_id`, `key`);
  END IF;

  IF NOT EXISTS(SELECT 1
                    FROM information_schema.columns
                    WHERE TABLE_SCHEMA = db
                      AND TABLE_NAME = 'available_esb_channel'
                      AND COLUMN_NAME = 'tenant_id') THEN
    ALTER TABLE available_esb_channel ADD COLUMN tenant_id VARCHAR(32) NOT NULL DEFAULT 'default';
  END IF;

  SELECT GROUP_CONCAT(COLUMN_NAME) INTO current_primary_key
    FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = db
      AND TABLE_NAME = 'available_esb_channel'
      AND CONSTRAINT_NAME = 'PRIMARY';

  IF current_primary_key = 'type' OR current_primary_key = 'type,tenant_id' THEN
    ALTER TABLE available_esb_channel DROP PRIMARY KEY;
    ALTER TABLE available_esb_channel ADD PRIMARY KEY (tenant_id, type);
  END IF;

  IF NOT EXISTS(SELECT 1
                    FROM information_schema.columns
                    WHERE TABLE_SCHEMA = db
                      AND TABLE_NAME = 'notify_black_user_info'
                      AND COLUMN_NAME = 'tenant_id') THEN
    ALTER TABLE notify_black_user_info ADD COLUMN tenant_id VARCHAR(32) NOT NULL DEFAULT 'default' AFTER `id`;
  END IF;

  IF NOT EXISTS(SELECT 1
                    FROM information_schema.columns
                    WHERE TABLE_SCHEMA = db
                      AND TABLE_NAME = 'notify_black_user_info'
                      AND COLUMN_NAME = 'display_name') THEN
    ALTER TABLE notify_black_user_info ADD COLUMN display_name varchar(128) DEFAULT NULL AFTER `username`;
  END IF;

  IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'notify_black_user_info'
                    AND INDEX_NAME = 'idx_tenant_id_username') THEN
    ALTER TABLE notify_black_user_info ADD INDEX idx_tenant_id_username(`tenant_id`, `username`);
  END IF;

  IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'user'
                    AND INDEX_NAME = 'idx_tenant_id_display_name') THEN
    ALTER TABLE user ADD INDEX idx_tenant_id_display_name(`tenant_id`, `display_name`);
  END IF;

  IF NOT EXISTS(SELECT 1
                    FROM information_schema.columns
                    WHERE TABLE_SCHEMA = db
                      AND TABLE_NAME = 'host'
                      AND COLUMN_NAME = 'tenant_id') THEN
    ALTER TABLE host ADD COLUMN tenant_id VARCHAR(32) NOT NULL DEFAULT 'default' AFTER `app_id`;
  END IF;

  IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'host'
                    AND INDEX_NAME = 'idx_tenant_id') THEN
    ALTER TABLE host ADD INDEX idx_tenant_id(`tenant_id`);
  END IF;


COMMIT;
END <JOB_UBF>
DELIMITER ;
CALL job_schema_update();

DROP PROCEDURE IF EXISTS job_schema_update;
