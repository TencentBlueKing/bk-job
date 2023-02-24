USE job_execute;

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS job_schema_update;

DELIMITER <JOB_UBF>

CREATE PROCEDURE job_schema_update()
BEGIN

  DECLARE db VARCHAR(100);
  SET AUTOCOMMIT = 0;
  SELECT DATABASE() INTO db;

  CREATE TABLE IF NOT EXISTS `task_instance_host` 
  (
    `task_instance_id` bigint(20)  NOT NULL DEFAULT '0',
    `host_id`          bigint(20)  NOT NULL DEFAULT '0',
    `ip`               varchar(15) DEFAULT NULL,
    `ipv6`             varchar(46) DEFAULT NULL,
    `row_create_time`  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `row_update_time`  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`task_instance_id`,`host_id`),
    KEY `idx_ip` (`ip`),
    KEY `idx_ipv6` (`ipv6`)
  ) ENGINE=InnoDB
    DEFAULT CHARSET=utf8mb4;

  COMMIT;
END <JOB_UBF>
DELIMITER ;
COMMIT;

CALL job_schema_update();

DROP PROCEDURE IF EXISTS job_schema_update;
