SET NAMES utf8mb4;
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
                        AND COLUMN_NAME = 'cluster_name') THEN
        ALTER TABLE `job_file_gateway`.`file_worker`
            ADD COLUMN `cluster_name` varchar(128) NOT NULL DEFAULT 'default' COMMENT '所在集群名称' AFTER `token`;
    END IF;

    IF NOT EXISTS(SELECT 1
                      FROM information_schema.statistics
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'file_worker'
                        AND INDEX_NAME = 'idx_clusterName_accessHost_accessPort') THEN
        ALTER TABLE `job_file_gateway`.`file_worker` ADD INDEX
            idx_clusterName_accessHost_accessPort(`cluster_name`,`access_host`,`access_port`);
    END IF;

    COMMIT;
END <JOB_UBF>
DELIMITER ;
CALL job_schema_update();

DROP PROCEDURE IF EXISTS job_schema_update;
