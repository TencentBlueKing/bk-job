SET NAMES utf8mb4;
USE job_execute;
ALTER TABLE step_instance ADD COLUMN step_num INT(11) NOT NULL DEFAULT 1;
ALTER TABLE step_instance ADD COLUMN step_order INT(11) NOT NULL DEFAULT 1;