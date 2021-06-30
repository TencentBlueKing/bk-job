use job_execute;

SET NAMES utf8mb4;

ALTER TABLE step_instance_variable ADD COLUMN `execute_count` int(11) NOT NULL DEFAULT '0' AFTER `step_instance_id`;
ALTER TABLE step_instance_variable DROP INDEX `uk_step_instance_id_type`;
ALTER TABLE step_instance_variable ADD UNIQUE INDEX `uk_step_instance_id_execute_count_type` (`step_instance_id`,`execute_count`,`type`);