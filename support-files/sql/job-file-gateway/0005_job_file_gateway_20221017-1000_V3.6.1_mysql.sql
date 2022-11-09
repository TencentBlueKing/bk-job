SET NAMES utf8mb4;
CREATE DATABASE IF NOT EXISTS job_file_gateway DEFAULT CHARACTER SET utf8mb4;
USE job_file_gateway;

-- ----------------------------
-- Table structure for file_worker_tag
-- ----------------------------
CREATE TABLE IF NOT EXISTS `file_worker_tag`
(
    `id`              bigint(20)                         NOT NULL AUTO_INCREMENT,
    `worker_id`       bigint(20)                         NOT NULL,
    `tag`             varchar(255) CHARACTER SET utf8mb4 NOT NULL COMMENT '标签',
    `row_create_time` DATETIME                           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_worker_tag` (`worker_id`, `tag`) USING BTREE,
    INDEX `idx_tag` (`tag`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4;


DROP PROCEDURE IF EXISTS job_schema_update;

DELIMITER <JOB_UBF>

CREATE PROCEDURE job_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

    -- ----------------------------
    -- Table structure for file_worker_ability
    -- ----------------------------
    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'file_worker_ability'
                    AND COLUMN_NAME = 'row_create_time') THEN
        ALTER TABLE `job_file_gateway`.`file_worker_ability`
            ADD COLUMN `row_create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
    END IF;

    -- ----------------------------
    -- Table structure for file_worker
    -- ----------------------------
    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'file_worker'
                    AND COLUMN_NAME = 'row_create_time') THEN
        ALTER TABLE `job_file_gateway`.`file_worker`
            ADD COLUMN `row_create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'file_worker'
                    AND COLUMN_NAME = 'row_update_time') THEN
        ALTER TABLE `job_file_gateway`.`file_worker`
            ADD COLUMN `row_update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
    END IF;

    -- ----------------------------
    -- Table structure for file_source
    -- ----------------------------
    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'file_source'
                    AND COLUMN_NAME = 'row_create_time') THEN
        ALTER TABLE `job_file_gateway`.`file_source`
            ADD COLUMN `row_create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'file_source'
                    AND COLUMN_NAME = 'row_update_time') THEN
        ALTER TABLE `job_file_gateway`.`file_source`
            ADD COLUMN `row_update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
    END IF;

    -- ----------------------------
    -- Table structure for file_source_batch_task
    -- ----------------------------
    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'file_source_batch_task'
                    AND COLUMN_NAME = 'row_create_time') THEN
        ALTER TABLE `job_file_gateway`.`file_source_batch_task`
            ADD COLUMN `row_create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'file_source_batch_task'
                    AND COLUMN_NAME = 'row_update_time') THEN
        ALTER TABLE `job_file_gateway`.`file_source_batch_task`
            ADD COLUMN `row_update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
    END IF;

    -- ----------------------------
    -- Table structure for file_source_share
    -- ----------------------------
    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'file_source_share'
                    AND COLUMN_NAME = 'row_create_time') THEN
        ALTER TABLE `job_file_gateway`.`file_source_share`
            ADD COLUMN `row_create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'file_source_share'
                    AND COLUMN_NAME = 'row_update_time') THEN
        ALTER TABLE `job_file_gateway`.`file_source_share`
            ADD COLUMN `row_update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
    END IF;

    -- ----------------------------
    -- Table structure for file_source_task
    -- ----------------------------
    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'file_source_task'
                    AND COLUMN_NAME = 'row_create_time') THEN
        ALTER TABLE `job_file_gateway`.`file_source_task`
            ADD COLUMN `row_create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'file_source_task'
                    AND COLUMN_NAME = 'row_update_time') THEN
        ALTER TABLE `job_file_gateway`.`file_source_task`
            ADD COLUMN `row_update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
    END IF;

    -- ----------------------------
    -- Table structure for file_source_type
    -- ----------------------------
    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'file_source_type'
                    AND COLUMN_NAME = 'row_create_time') THEN
        ALTER TABLE `job_file_gateway`.`file_source_type`
            ADD COLUMN `row_create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'file_source_type'
                    AND COLUMN_NAME = 'row_update_time') THEN
        ALTER TABLE `job_file_gateway`.`file_source_type`
            ADD COLUMN `row_update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
    END IF;

    -- ----------------------------
    -- Table structure for file_task
    -- ----------------------------
    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'file_task'
                    AND COLUMN_NAME = 'row_create_time') THEN
        ALTER TABLE `job_file_gateway`.`file_task`
            ADD COLUMN `row_create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'file_task'
                    AND COLUMN_NAME = 'row_update_time') THEN
        ALTER TABLE `job_file_gateway`.`file_task`
            ADD COLUMN `row_update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
    END IF;

    COMMIT;
END <JOB_UBF>
DELIMITER ;
CALL job_schema_update();

DROP PROCEDURE IF EXISTS job_schema_update;
