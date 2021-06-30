SET NAMES utf8mb4;
USE job_execute;
ALTER TABLE step_instance_script ADD COLUMN script_source TINYINT(4) DEFAULT 1 COMMENT '脚本来源：1-本地脚本 2-引用业务脚本 3-引用公共脚本';