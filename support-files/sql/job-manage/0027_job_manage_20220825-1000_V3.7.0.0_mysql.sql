SET NAMES utf8mb4;
USE job_manage;

-- ----------------------------
-- Table structure for user_custom_setting
-- ----------------------------
CREATE TABLE IF NOT EXISTS `user_custom_setting`
(
    `username`         varchar(64) CHARACTER SET utf8mb4  NOT NULL COMMENT '用户名',
    `app_id`           bigint(20)                         NOT NULL COMMENT 'Job业务Id',
    `module`           varchar(32) CHARACTER SET utf8mb4  NOT NULL COMMENT '模块',
    `key`              varchar(128) CHARACTER SET utf8mb4 NOT NULL COMMENT '唯一键',
    `value`            text CHARACTER SET utf8mb4         NULL COMMENT '配置项的值Json串',
    `last_modify_user` varchar(255) CHARACTER SET utf8mb4 NULL     DEFAULT NULL COMMENT '最后更新人',
    `last_modify_time` bigint(20)                         NULL     DEFAULT NULL COMMENT '最后更新时间',
    `row_create_time`  datetime                           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `row_update_time`  datetime                           NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`username`, `app_id`, `module`) USING BTREE,
    INDEX idx_key (`key`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4;


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
                    AND TABLE_NAME = 'host'
                    AND COLUMN_NAME = 'cloud_vendor_id') THEN
        ALTER TABLE `host`
            ADD COLUMN `cloud_vendor_id` VARCHAR(20) DEFAULT NULL COMMENT '云厂商ID';
    END IF;

    IF EXISTS(SELECT 1
              FROM information_schema.columns
              WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'host'
                AND COLUMN_NAME = 'ip') THEN
        ALTER TABLE `host`
            MODIFY COLUMN `ip` VARCHAR(15) NULL COMMENT '主机IPv4地址';
    END IF;

    IF EXISTS(SELECT 1
              FROM information_schema.columns
              WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'host'
                AND COLUMN_NAME = 'display_ip') THEN
        ALTER TABLE `host`
            MODIFY COLUMN `display_ip` text NULL COMMENT '主机展示用的IPv4地址，可能存在多个IP';
    END IF;

    IF EXISTS(SELECT 1
              FROM information_schema.columns
              WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'host'
                AND COLUMN_NAME = 'cloud_ip') THEN
        ALTER TABLE `host`
            MODIFY COLUMN `cloud_ip` VARCHAR(65) NULL COMMENT '云区域ID:主机IP地址';
    END IF;

    COMMIT;
END <JOB_UBF>
DELIMITER ;
CALL job_schema_update();

DROP PROCEDURE IF EXISTS job_schema_update;
