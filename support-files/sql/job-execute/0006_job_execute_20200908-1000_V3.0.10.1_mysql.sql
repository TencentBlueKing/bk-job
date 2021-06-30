SET NAMES utf8mb4;
USE job_execute;

ALTER TABLE step_instance_script ADD COLUMN script_id VARCHAR(32) NULL COMMENT '脚本Id';
ALTER TABLE step_instance_script ADD COLUMN script_version_id BIGINT(20) NULL COMMENT '脚本版本Id';