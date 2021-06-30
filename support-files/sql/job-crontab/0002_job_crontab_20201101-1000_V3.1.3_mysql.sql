SET NAMES utf8mb4;
USE job_crontab;

ALTER TABLE `job_crontab`.`cron_job_history` ADD COLUMN `executor` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL AFTER `finish_time`;

ALTER TABLE `job_crontab`.`cron_job_history` ADD COLUMN `error_code` bigint(20) UNSIGNED NULL DEFAULT NULL AFTER `executor`;

ALTER TABLE `job_crontab`.`cron_job_history` ADD COLUMN `error_message` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL AFTER `error_code`;
