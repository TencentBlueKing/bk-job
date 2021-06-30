SET NAMES utf8mb4;
USE job_execute;
ALTER TABLE step_instance_confirm ADD COLUMN `confirm_reason` varchar(256) DEFAULT NULL;
ALTER TABLE task_instance ADD INDEX `idx_app_cron_id` (app_id,cron_task_id);
ALTER TABLE task_instance DROP INDEX idx_create_time_app_cron;