SET NAMES utf8mb4;
USE job_file_gateway;

DROP PROCEDURE IF EXISTS job_schema_update;

DELIMITER <JOB_UBF>

CREATE PROCEDURE job_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

    DELETE FROM `file_source_type` WHERE `id`=1 AND `last_modify_user`="admin";

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'file_source_type'
                    AND COLUMN_NAME = 'enabled') THEN
        ALTER TABLE `file_source_type` ADD COLUMN `enabled` BIT NOT NULL DEFAULT 0 COMMENT 'file-worker支持该类型文件源的能力是否启用';
    END IF;

COMMIT;
END <JOB_UBF>
DELIMITER ;
CALL job_schema_update();

DROP PROCEDURE IF EXISTS job_schema_update;
