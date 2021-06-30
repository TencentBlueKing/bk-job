SET NAMES utf8mb4;
USE job_execute;
ALTER TABLE gse_task_ip_log ADD COLUMN is_source TINYINT(1) NOT NULL DEFAULT 0;