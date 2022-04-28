USE job_execute;

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS job_schema_update;

DELIMITER <JOB_UBF>

CREATE PROCEDURE job_schema_update()
BEGIN

  DECLARE db VARCHAR(100);
  SET AUTOCOMMIT = 0;
  SELECT DATABASE() INTO db;

  CREATE TABLE IF NOT EXISTS `gse_task` 
    (
      `id`               bigint(20)  NOT NULL AUTO_INCREMENT,
      `step_instance_id` bigint(20)  NOT NULL DEFAULT '0',
      `execute_count`    int(11)     NOT NULL DEFAULT '0',
      `batch`            smallint(6) NOT NULL DEFAULT '0',
      `start_time`       bigint(20)  DEFAULT NULL,
      `end_time`         bigint(20)  DEFAULT NULL,
      `total_time`       bigint(11)  DEFAULT NULL,
      `status`           tinyint(4)  DEFAULT '1',
      `gse_task_id`      varchar(64) DEFAULT NULL,
      `row_create_time`  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
      `row_update_time`  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
      PRIMARY KEY (`id`),
      UNIQUE KEY `uk_step_id_execute_count_batch` (`step_instance_id`,`execute_count`,`batch`)
    ) ENGINE=InnoDB
      AUTO_INCREMENT=30000000000 
      DEFAULT CHARSET=utf8mb4;

  CREATE TABLE IF NOT EXISTS `gse_script_agent_task` 
    (
      `id`               bigint(20)     NOT NULL AUTO_INCREMENT,
      `step_instance_id` bigint(20)     NOT NULL,
      `execute_count`    int(11)        NOT NULL DEFAULT '0',
      `batch`            smallint(6)    NOT NULL DEFAULT '0',
      `ip`               varchar(30)    NOT NULL,
      `gse_task_id`      bigint(20)     NOT NULL DEFAULT '0',
      `status`           int(11)        DEFAULT '1',
      `start_time`       bigint(20)     DEFAULT NULL,
      `end_time`         bigint(20)     DEFAULT NULL,
      `total_time`       bigint(20)     DEFAULT NULL,
      `error_code`       int(11)        DEFAULT '0',
      `exit_code`        int(11)        DEFAULT NULL,
      `tag`              varchar(256)   CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT '',
      `log_offset`       int(11)        NOT NULL DEFAULT '0',
      `display_ip`       varchar(30)    CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
      `is_target`        tinyint(1)     NOT NULL DEFAULT '1',
      `is_source`        tinyint(1)     NOT NULL DEFAULT '0',
      `row_create_time`  datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP,
      `row_update_time`  datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
      PRIMARY KEY (`id`),
      UNIQUE KEY `uk_step_id_execute_count_batch_ip` (`step_instance_id`,`execute_count`,`batch`, `ip`),
      KEY `idx_step_id_execute_count_ip` (`step_instance_id`,`execute_count`,`ip`),
      KEY `idx_display_ip` (`display_ip`)
    ) ENGINE=InnoDB
      AUTO_INCREMENT=30000000000
      DEFAULT CHARSET=utf8mb4;

  CREATE TABLE IF NOT EXISTS `gse_file_agent_task` 
    (
      `id`               bigint(20)     NOT NULL AUTO_INCREMENT,
      `step_instance_id` bigint(20)     NOT NULL,
      `execute_count`    int(11)        NOT NULL DEFAULT '0',
      `batch`            smallint(6)    NOT NULL DEFAULT '0',
      `ip`               varchar(30)    NOT NULL,
      `mode`             tinyint(1)     NOT NULL,
      `gse_task_id`      bigint(20)     NOT NULL DEFAULT '0',
      `status`           int(11)        DEFAULT '1',
      `start_time`       bigint(20)     DEFAULT NULL,
      `end_time`         bigint(20)     DEFAULT NULL,
      `total_time`       bigint(20)     DEFAULT NULL,
      `error_code`       int(11)        DEFAULT '0',
      `exit_code`        int(11)        DEFAULT NULL,
      `tag`              varchar(256)   CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT '',
      `log_offset`       int(11)        NOT NULL DEFAULT '0',
      `display_ip`       varchar(30)    CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
      `is_target`        tinyint(1)     NOT NULL DEFAULT '1',
      `is_source`        tinyint(1)     NOT NULL DEFAULT '0',
      `row_create_time`  datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP,
      `row_update_time`  datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
      PRIMARY KEY (`id`),
      UNIQUE KEY `uk_step_id_execute_count_batch_ip` (`step_instance_id`,`execute_count`,`batch`, `ip`),
      KEY `idx_step_id_execute_count_ip` (`step_instance_id`,`execute_count`,`ip`),
      KEY `idx_display_ip` (`display_ip`)
    ) ENGINE=InnoDB
      AUTO_INCREMENT=30000000000
      DEFAULT CHARSET=utf8mb4;
	
	CREATE TABLE IF NOT EXISTS `task_instance_rolling_config`
    (
      `id`                  bigint(20)   NOT NULL AUTO_INCREMENT,
      `task_instance_id`    bigint(20)   NOT NULL DEFAULT '0',
      `config_name`         varchar(128) NOT NULL,
      `config`              longtext     NOT NULL,
      `row_create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
      `row_update_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
      PRIMARY KEY(`id`),
      UNIQUE KEY `uk_task_instance_id_config_name` (`task_instance_id`,`config_name`)
    ) ENGINE = InnoDB
      DEFAULT CHARSET = utf8mb4;

  CREATE TABLE IF NOT EXISTS `step_instance_rolling_task`
    (
      `id`               bigint(20)  NOT NULL AUTO_INCREMENT,
      `step_instance_id` bigint(20)  NOT NULL DEFAULT '0',
      `execute_count`    tinyint(4)  NOT NULL DEFAULT '0',
      `batch`            smallint(6) NOT NULL DEFAULT '0',
      `start_time`       bigint(20)           DEFAULT NULL,
      `end_time`         bigint(20)           DEFAULT NULL,
      `total_time`       bigint(11)           DEFAULT NULL,
      `status`           tinyint(4)  NOT NULL DEFAULT '1',
      `row_create_time`  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
      `row_update_time`  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
      PRIMARY KEY(`id`),
      UNIQUE KEY `uk_step_instance_id_execute_count_batch` (`step_instance_id`, `execute_count`, `batch`)
    ) ENGINE = InnoDB
      DEFAULT CHARSET = utf8mb4;

  IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'step_instance'
                    AND COLUMN_NAME = 'batch') THEN
    ALTER TABLE step_instance ADD COLUMN `batch` smallint(6) NOT NULL DEFAULT '0' AFTER execute_count;
  END IF;
	
	IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'step_instance'
                    AND COLUMN_NAME = 'rolling_config_id') THEN
    ALTER TABLE step_instance ADD COLUMN `rolling_config_id` bigint(20);
  END IF;
	
  IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'gse_task_ip_log'
                    AND COLUMN_NAME = 'batch') THEN
    ALTER TABLE gse_task_ip_log ADD COLUMN `batch` smallint(6) NOT NULL DEFAULT '0' AFTER execute_count;
  END IF;

  IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'gse_task_ip_log'
                    AND COLUMN_NAME = 'gse_task_id') THEN
    ALTER TABLE gse_task_ip_log ADD COLUMN `gse_task_id` bigint(20) NOT NULL DEFAULT '0' AFTER batch;
  END IF;	
	
	IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'gse_task_ip_log'
                    AND INDEX_NAME = 'idx_gse_task_id') THEN
    ALTER TABLE gse_task_ip_log ADD INDEX `uk_gse_task_id` (`gse_task_id`);
  END IF;
	
	
  IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'gse_task_log'
                    AND COLUMN_NAME = 'batch') THEN
        ALTER TABLE gse_task_log ADD COLUMN `batch` smallint(6) NOT NULL DEFAULT '0' AFTER execute_count;
  END IF;
    
  IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'gse_task_log'
                    AND INDEX_NAME = 'idx_gse_task_id') THEN
    ALTER TABLE gse_task_log ADD INDEX `idx_gse_task_id` (`gse_task_id`);
  END IF;        
	
	IF EXISTS(SELECT 1
              FROM information_schema.statistics
              WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'gse_task_log'
                AND INDEX_NAME = 'PRIMARY'
                AND COLUMN_NAME= 'step_instance_id'
           )
    AND EXISTS(SELECT 1
                 FROM information_schema.statistics
                 WHERE TABLE_SCHEMA = db
                   AND TABLE_NAME = 'gse_task_log'
                   AND INDEX_NAME = 'PRIMARY'
                   AND COLUMN_NAME= 'execute_count'
              ) THEN
    ALTER TABLE gse_task_log ADD UNIQUE INDEX `uk_step_id_execute_count_batch` (`step_instance_id`,`execute_count`,`batch`);
    ALTER TABLE gse_task_log DROP PRIMARY KEY;
    ALTER TABLE gse_task_log ADD COLUMN `id` bigint(20) NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;
  END IF;

  COMMIT;
END <JOB_UBF>
DELIMITER ;
COMMIT;

CALL job_schema_update();

DROP PROCEDURE IF EXISTS job_schema_update;
