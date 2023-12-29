SET NAMES utf8mb4;
CREATE DATABASE IF NOT EXISTS job_execute DEFAULT CHARACTER SET utf8mb4;
USE job_execute;

CREATE TABLE IF NOT EXISTS `gse_script_execute_obj_task` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
	`task_instance_id` bigint(20) NOT NULL,
  `step_instance_id` bigint(20) NOT NULL,
  `execute_count` smallint(6) NOT NULL DEFAULT '0',
  `actual_execute_count` smallint(6) DEFAULT NULL,
  `batch` smallint(6) NOT NULL DEFAULT '0',
	`execute_obj_type` tinyint(4) NOT NULL,
  `execute_obj_id` varchar(24) NOT NULL,
  `gse_task_id` bigint(20) NOT NULL DEFAULT '0',
  `status` int(11) DEFAULT '1',
  `start_time` bigint(20) DEFAULT NULL,
  `end_time` bigint(20) DEFAULT NULL,
  `total_time` bigint(20) DEFAULT NULL,
  `error_code` int(11) DEFAULT '0',
  `exit_code` int(11) DEFAULT NULL,
  `tag` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT '',
  `log_offset` int(11) NOT NULL DEFAULT '0',
  `row_create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `row_update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_step_id_execute_count_batch_execute_obj_id` (`step_instance_id`,`execute_count`,`batch`,`execute_obj_id`),
	KEY `idx_task_instance_id` (`task_instance_id`),
  KEY `idx_step_id_execute_obj_id` (`step_instance_id`,`execute_obj_id`)
) ENGINE=InnoDB AUTO_INCREMENT=120000000000 DEFAULT CHARSET=utf8mb4;


CREATE TABLE IF NOT EXISTS `gse_file_execute_obj_task` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
	`task_instance_id` bigint(20) NOT NULL,
  `step_instance_id` bigint(20) NOT NULL,
  `execute_count` smallint(6) NOT NULL DEFAULT '0',
  `actual_execute_count` smallint(6) DEFAULT NULL,
  `batch` smallint(6) NOT NULL DEFAULT '0',
	`execute_obj_type` tinyint(4) NOT NULL,
  `execute_obj_id` varchar(24) NOT NULL,
  `mode` tinyint(1) NOT NULL,
  `gse_task_id` bigint(20) NOT NULL DEFAULT '0',
  `status` int(11) DEFAULT '1',
  `start_time` bigint(20) DEFAULT NULL,
  `end_time` bigint(20) DEFAULT NULL,
  `total_time` bigint(20) DEFAULT NULL,
  `error_code` int(11) DEFAULT '0',
  `row_create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `row_update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_step_id_execute_count_batch_mode_execute_obj_id` (`step_instance_id`,`execute_count`,`batch`,`mode`,`execute_obj_id`),
	KEY `idx_task_instance_id` (`task_instance_id`),
  KEY `idx_step_id_execute_obj_id` (`step_instance_id`,`execute_obj_id`)
) ENGINE=InnoDB AUTO_INCREMENT=120000000000 DEFAULT CHARSET=utf8mb4;