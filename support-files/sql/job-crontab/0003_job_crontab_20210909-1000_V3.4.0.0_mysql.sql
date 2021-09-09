SET NAMES utf8mb4;
USE job_crontab;

ALTER TABLE `job_crontab`.`cron_job` ADD COLUMN `last_execute_error_code` bigint(20) UNSIGNED NULL DEFAULT NULL AFTER `last_execute_status`;

ALTER TABLE `job_crontab`.`cron_job` ADD COLUMN `last_execute_error_count` int(11) UNSIGNED NOT NULL DEFAULT '0' AFTER `last_execute_error_code`;