USE `job_manage`;

ALTER TABLE `job_manage`.`host` ADD COLUMN `os_type` VARCHAR(32) DEFAULT NULL COMMENT '系统类型';