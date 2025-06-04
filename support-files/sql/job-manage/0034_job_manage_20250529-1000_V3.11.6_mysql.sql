USE job_manage;

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS job_schema_update;

DELIMITER <JOB_UBF>

CREATE PROCEDURE job_schema_update()
BEGIN

  DECLARE db VARCHAR(100);
  SELECT DATABASE() INTO db;

  IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'notify_policy_role_target'
                    AND INDEX_NAME = 'idx_policy_id') THEN
    ALTER TABLE notify_policy_role_target ADD INDEX `idx_policy_id` (policy_id);
  END IF;

  IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'notify_role_target_channel'
                    AND INDEX_NAME = 'idx_role_target_id') THEN
    ALTER TABLE notify_role_target_channel ADD INDEX `idx_role_target_id` (role_target_id);
  END IF;

  IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'notify_trigger_policy'
                    AND INDEX_NAME = 'idx_resource_type_resource_id') THEN
    ALTER TABLE notify_trigger_policy ADD INDEX `idx_resource_type_resource_id` (resource_type, resource_id);
  END IF;

COMMIT;
END <JOB_UBF>
DELIMITER ;
CALL job_schema_update();
DROP PROCEDURE IF EXISTS job_schema_update;
