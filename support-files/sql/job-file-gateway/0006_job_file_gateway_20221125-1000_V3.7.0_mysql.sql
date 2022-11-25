SET NAMES utf8mb4;
CREATE DATABASE IF NOT EXISTS job_file_gateway DEFAULT CHARACTER SET utf8mb4;
USE job_file_gateway;

DROP PROCEDURE IF EXISTS job_schema_update;

DELIMITER <JOB_UBF>

CREATE PROCEDURE job_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

    -- ----------------------------
    -- Table structure for file_worker
    -- ----------------------------
    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'file_worker'
                    AND COLUMN_NAME = 'inner_ip_protocol') THEN
        ALTER TABLE `job_file_gateway`.`file_worker`
            ADD COLUMN `inner_ip_protocol` varchar(4) NULL COMMENT '所在机器的内网IP使用的协议，参考取值：v4/v6' AFTER `cloud_area_id`;
    END IF;

    COMMIT;
END <JOB_UBF>
DELIMITER ;
CALL job_schema_update();

DROP PROCEDURE IF EXISTS job_schema_update;
