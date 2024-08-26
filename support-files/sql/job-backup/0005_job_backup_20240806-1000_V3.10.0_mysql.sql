USE `job_backup`;

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `archive_task` (
  `row_create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `row_update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `task_type` TINYINT(2),
  `data_node` VARCHAR(128) NOT NULL,
  `day` INT(8),
  `hour` TINYINT(2),
  `from_timestamp` BIGINT(20) NOT NULL,
  `to_timestamp` BIGINT(20) NOT NULL,
  `process` VARCHAR(256),
  `status` TINYINT(2) NOT NULL DEFAULT '0',
  `create_time` BIGINT(20) NOT NULL,
  `last_update_time` BIGINT(20) NOT NULL,
  PRIMARY KEY (`task_type`,`data_node`,`day`,`hour`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

