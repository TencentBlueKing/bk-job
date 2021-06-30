SET NAMES utf8mb4;

USE job_backup;

ALTER TABLE `job_backup`.`export_job` ADD COLUMN `is_cleaned` tinyint(1) UNSIGNED NOT NULL DEFAULT 0 AFTER `file_name`;
ALTER TABLE `job_backup`.`import_job` ADD COLUMN `is_cleaned` tinyint(1) UNSIGNED NOT NULL DEFAULT 0 AFTER `id_name_info`;
