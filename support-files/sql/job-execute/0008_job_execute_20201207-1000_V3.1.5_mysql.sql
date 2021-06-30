SET NAMES utf8mb4;
USE job_execute;

ALTER TABLE step_instance_script ADD COLUMN `is_secure_param` tinyint(1) NOT NULL DEFAULT 0;
ALTER TABLE step_instance_variable ADD INDEX `idx_task_instance_id` (task_instance_id);