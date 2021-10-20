SET NAMES utf8mb4;
USE job_file_gateway;

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
                    AND TABLE_NAME = 'file_worker'
                    AND COLUMN_NAME = 'config_str') THEN
        ALTER TABLE `job_file_gateway`.`file_worker` ADD COLUMN `config_str` MEDIUMTEXT DEFAULT NULL COMMENT '配置字符串';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'file_source_type'
                    AND COLUMN_NAME = 'worker_id') THEN
        ALTER TABLE `job_file_gateway`.`file_source_type` ADD COLUMN `worker_id` bigint(20) DEFAULT NULL COMMENT '上报文件源类型的WorkerId';
    END IF;

    IF EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'file_source'
                    AND COLUMN_NAME = 'region_code') THEN
        ALTER TABLE `job_file_gateway`.`file_source` DROP COLUMN `region_code`;
    END IF;

    IF EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'file_source'
                    AND COLUMN_NAME = 'region_name') THEN
        ALTER TABLE `job_file_gateway`.`file_source` DROP COLUMN `region_name`;
    END IF;

COMMIT;
END <JOB_UBF>
DELIMITER ;
CALL job_schema_update();

DROP PROCEDURE IF EXISTS job_schema_update;
