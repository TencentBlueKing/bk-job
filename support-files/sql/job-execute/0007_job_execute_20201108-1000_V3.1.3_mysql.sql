SET NAMES utf8mb4;
USE job_execute;

ALTER TABLE gse_task_ip_log ADD INDEX `idx_display_ip` (display_ip);
