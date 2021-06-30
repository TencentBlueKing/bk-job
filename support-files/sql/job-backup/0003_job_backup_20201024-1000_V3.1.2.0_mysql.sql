USE `job_backup`;
ALTER TABLE `job_backup`.`export_job` ADD COLUMN `locale` varchar(20) NOT NULL DEFAULT 'zh-CN' AFTER `is_cleaned`;
ALTER TABLE `job_backup`.`import_job` ADD COLUMN `locale` varchar(20) NOT NULL DEFAULT 'zh-CN' AFTER `is_cleaned`;