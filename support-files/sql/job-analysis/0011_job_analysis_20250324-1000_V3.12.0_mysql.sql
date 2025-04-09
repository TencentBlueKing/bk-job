SET NAMES utf8mb4;
USE job_analysis;

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
                    AND TABLE_NAME = 'statistics'
                    AND COLUMN_NAME = 'tenant_id') THEN
        ALTER TABLE `job_analysis`.`statistics` ADD COLUMN `tenant_id` VARCHAR(32) NOT NULL DEFAULT 'default' AFTER `id`;
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'statistics'
                    AND INDEX_NAME = 'idx_tenantId_appId_res_dim_dimValue_date') THEN
        ALTER TABLE `statistics` ADD INDEX `idx_tenantId_appId_res_dim_dimValue_date`(`tenant_id`,`app_id`,`resource`(32), `dimension`(32), `dimension_value`(32),`date`(32)) USING BTREE;
    END IF;

COMMIT;
END <JOB_UBF>
DELIMITER ;
CALL job_schema_update();

DROP PROCEDURE IF EXISTS job_schema_update;
