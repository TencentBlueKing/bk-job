SET NAMES utf8mb4;
CREATE DATABASE IF NOT EXISTS job_execute DEFAULT CHARACTER SET utf8mb4;
USE job_execute;

CREATE TABLE IF NOT EXISTS `task_instance` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `app_id` bigint(20) NOT NULL,
  `task_id` bigint(20) NOT NULL DEFAULT '0',
  `task_template_id` bigint(20) NOT NULL DEFAULT '0',
  `name` varchar(512) NOT NULL,
  `type` tinyint(4) NOT NULL,
  `operator` varchar(128) NOT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '0',
  `current_step_id` bigint(20) NOT NULL DEFAULT '0',
  `startup_mode` tinyint(4) NOT NULL,
  `total_time` bigint(20) DEFAULT NULL,
  `callback_url` varchar(1024) DEFAULT NULL,
  `is_debug_task` tinyint(4) NOT NULL DEFAULT '0',
  `cron_task_id` bigint(20) NOT NULL DEFAULT '0',
  `create_time` bigint(20) DEFAULT NULL,
  `start_time` bigint(20) DEFAULT NULL,
  `end_time` bigint(20) DEFAULT NULL,
  `app_code` varchar(128) DEFAULT NULL,
  `row_create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `row_update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_app_create_time` (`app_id`,`create_time`),
  KEY `idx_create_time_app_name` (`app_id`,`create_time`,`name`),
  KEY `idx_create_time_app_operator` (`app_id`,`create_time`,`operator`),
  KEY `idx_create_time_app_task` (`app_id`,`create_time`,`task_id`),
  KEY `idx_create_time_app_status` (`app_id`,`create_time`,`status`),
  KEY `idx_create_time_app_cron` (`app_id`,`create_time`,`cron_task_id`),
  KEY `idx_create_time_app_type` (`app_id`,`create_time`,`type`),
  KEY `idx_create_time_app_startup` (`app_id`,`create_time`,`startup_mode`)
) ENGINE = InnoDB
  AUTO_INCREMENT=20000000000
  DEFAULT CHARSET = utf8mb4;
  
CREATE TABLE IF NOT EXISTS `step_instance` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `step_id` bigint(20) NOT NULL,
  `task_instance_id` bigint(20) NOT NULL,
  `app_id` bigint(20) NOT NULL,
  `name` varchar(512) DEFAULT NULL,
  `type` tinyint(4) NOT NULL,
  `operator` varchar(128) DEFAULT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `execute_count` int(11) NOT NULL DEFAULT '0',
  `target_servers` longtext /*!99104 COMPRESSED */,
  `abnormal_agent_ip_list` longtext /*!99104 COMPRESSED */,
  `start_time` bigint(20) DEFAULT NULL,
  `end_time` bigint(20) DEFAULT NULL,
  `total_time` bigint(20) DEFAULT NULL,
  `total_ip_num` int(11) DEFAULT '0',
  `abnormal_agent_num` int(11) DEFAULT '0',
  `run_ip_num` int(11) DEFAULT '0',
  `fail_ip_num` int(11) DEFAULT '0',
  `success_ip_num` int(11) DEFAULT '0',
  `create_time` bigint(20) DEFAULT NULL,
  `ignore_error` tinyint(4) NOT NULL DEFAULT 0,
  `row_create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `row_update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_task_instance_id` (`task_instance_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT=20000000000
  DEFAULT CHARSET = utf8mb4;
  
CREATE TABLE IF NOT EXISTS `step_instance_script` (
  `step_instance_id` bigint(20) NOT NULL,
  `script_content` mediumtext /*!99104 COMPRESSED */ NOT NULL,
  `script_type` tinyint(4) NOT NULL,
  `script_param` text /*!99104 COMPRESSED */,
  `resolved_script_param` text /*!99104 COMPRESSED */,
  `execution_timeout` int(11) DEFAULT NULL,
  `system_account_id` bigint(20) DEFAULT NULL,
  `system_account` varchar(256) DEFAULT NULL,
  `db_account_id` bigint(20) DEFAULT NULL,
  `db_type` tinyint(4) DEFAULT NULL,
  `db_account` varchar(256) DEFAULT NULL,
  `db_password` varchar(512) DEFAULT NULL,
  `db_port` int(5) DEFAULT NULL,
  `row_create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `row_update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,  
  PRIMARY KEY (`step_instance_id`)
) ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `step_instance_file` (
  `step_instance_id` bigint(20) NOT NULL,
  `file_source` mediumtext /*!99104 COMPRESSED */ NOT NULL,
  `resolved_file_source` mediumtext /*!99104 COMPRESSED */ DEFAULT NULL,
  `file_target_path` varchar(512) NOT NULL,
  `resolved_file_target_path` varchar(512) DEFAULT NULL,
  `file_upload_speed_limit` int(11) DEFAULT NULL,
  `file_download_speed_limit` int(11) DEFAULT NULL,
  `file_duplicate_handle` tinyint(4) DEFAULT NULL,
  `not_exist_path_handler` TINYINT(4) UNSIGNED DEFAULT 1,
  `execution_timeout` int(11) DEFAULT NULL,
  `system_account_id` bigint(20) DEFAULT NULL,
  `system_account` varchar(256) DEFAULT NULL,
  `row_create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `row_update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`step_instance_id`)
) ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `step_instance_confirm` (
  `step_instance_id` bigint(20) NOT NULL,
  `confirm_message` text /*!99104 COMPRESSED */ DEFAULT NULL,
  `confirm_users` varchar(1024) DEFAULT NULL,
  `confirm_roles` varchar(512) DEFAULT NULL,
  `notify_channels` varchar(256) DEFAULT NULL,
  `row_create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `row_update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`step_instance_id`)
) ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4;
  
CREATE TABLE IF NOT EXISTS `gse_task_log` (
  `step_instance_id` bigint(20) NOT NULL DEFAULT '0',
  `execute_count` int(11) NOT NULL DEFAULT '0',
  `start_time` bigint(20) DEFAULT NULL,
  `end_time` bigint(20) DEFAULT NULL,
  `total_time` bigint(11) DEFAULT NULL,
  `status` tinyint(4) DEFAULT '1',
  `gse_task_id` varchar(64) DEFAULT NULL,
  `row_create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `row_update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`step_instance_id`,`execute_count`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
  
CREATE TABLE IF NOT EXISTS `gse_task_ip_log` (
  `step_instance_id` bigint(20) NOT NULL,
  `execute_count` int(11) NOT NULL DEFAULT '0',
  `ip` varchar(30) NOT NULL,
  `status` int(11) DEFAULT '1',
  `start_time` bigint(20) DEFAULT NULL,
  `end_time` bigint(20) DEFAULT NULL,
  `total_time` bigint(20) DEFAULT NULL,
  `error_code` int(11) DEFAULT '0',
  `exit_code` int(11) DEFAULT NULL,
  `tag` varchar(256) COLLATE utf8mb4_bin DEFAULT '',
  `log_offset` int(11) NOT NULL DEFAULT '0',
  `display_ip` varchar(30) COLLATE utf8mb4_bin NOT NULL,
  `is_target` tinyint(1) NOT NULL default '1',
  `row_create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `row_update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`step_instance_id`,`execute_count`,`ip`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
  
CREATE TABLE IF NOT EXISTS `operation_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `task_instance_id` bigint(20) NOT NULL,
  `op_code` tinyint(4) NOT NULL,
  `operator` varchar(255) NOT NULL,
  `detail` text /*!99104 COMPRESSED */,
  `create_time` bigint(20) DEFAULT NULL,
  `row_create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `row_update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_task_instance_id` (`task_instance_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
  
CREATE TABLE IF NOT EXISTS `task_instance_variable` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `task_instance_id` bigint(20) NOT NULL,
  `name` varchar(512) NOT NULL,
  `type` tinyint(4) NOT NULL,
  `value` longtext /*!99104 COMPRESSED */,
  `is_changeable` tinyint(1) NOT NULL,
  `row_create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `row_update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_task_instance_id` (`task_instance_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
  
CREATE TABLE IF NOT EXISTS `step_instance_variable` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `task_instance_id` bigint(20) NOT NULL,
  `step_instance_id` bigint(20) NOT NULL,
  `type` tinyint(4) NOT NULL,
  `param_values` longtext /*!99104 COMPRESSED */ NOT NULL,
  `row_create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `row_update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_step_instance_id_type` (`step_instance_id`,`type`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;