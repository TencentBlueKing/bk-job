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
                    AND TABLE_NAME = 'tag'
                    AND COLUMN_NAME = 'description') THEN
        ALTER TABLE tag ADD COLUMN description VARCHAR(1024) NULL AFTER name;
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.columns
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'tag'
                    AND COLUMN_NAME = 'create_time') THEN
        ALTER TABLE tag ADD COLUMN create_time bigint(20) NOT NULL DEFAULT 0 AFTER creator;
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.columns
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'tag'
                    AND COLUMN_NAME = 'last_modify_time') THEN
        ALTER TABLE tag ADD COLUMN last_modify_time bigint(20) NOT NULL DEFAULT 0 AFTER last_modify_user;
    END IF;
	
	UPDATE tag SET create_time=UNIX_TIMESTAMP(row_create_time),last_modify_time=UNIX_TIMESTAMP(row_create_time) WHERE create_time = 0;
	
    COMMIT;
END <JOB_UBF>
DELIMITER ;
COMMIT;
CALL job_schema_update();

DROP PROCEDURE IF EXISTS job_schema_update;

CREATE TABLE IF NOT EXISTS `resource_tag`
(
    `id`                 BIGINT(20)          NOT NULL AUTO_INCREMENT,
    `row_create_time`    DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `row_update_time`    DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `tag_id`             BIGINT(20)          NOT NULL,
    `resource_id`        VARCHAR(32)         NOT NULL,
    `resource_type`      TINYINT(4)          NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_res_id_res_type_tag_id`(`resource_id`, `resource_type`, `tag_id`),
    KEY `idx_tag_id_res_type_res_id` (`tag_id`, `resource_type`, `resource_id`)
) ENGINE = INNODB
  DEFAULT CHARSET = UTF8MB4;
