USE `job_manage`;

ALTER TABLE `job_manage`.`dangerous_rule` MODIFY COLUMN `script_type` int(10) NOT NULL DEFAULT 0 COMMENT '脚本类型';