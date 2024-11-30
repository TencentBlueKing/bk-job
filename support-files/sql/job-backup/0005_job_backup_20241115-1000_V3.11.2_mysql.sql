USE `job_backup`;

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `archive_task` (
  `row_create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `row_update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `task_type` TINYINT(2),
  `data_node` VARCHAR(128) NOT NULL,
  `db_node` VARCHAR(64) NOT NULL,
  `day` INT(8),
  `hour` TINYINT(2),
  `from_timestamp` BIGINT(20) NOT NULL,
  `to_timestamp` BIGINT(20) NOT NULL,
  `process` VARCHAR(256),
  `status` TINYINT(2) NOT NULL DEFAULT '0',
  `create_time` BIGINT(20) NOT NULL,
  `last_update_time` BIGINT(20) NOT NULL,
  `task_start_time` BIGINT(20),
  `task_end_time` BIGINT(20),
  `task_cost` BIGINT(20),
  `detail` TEXT,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_type_node_day_hour` (`task_type`,`data_node`,`day`,`hour`),
  KEY `idx_type_status_db` (`task_type`,`status`,`db_node`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

