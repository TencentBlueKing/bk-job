use job_manage;

SET NAMES utf8mb4;

ALTER TABLE `dangerous_rule` ADD COLUMN `action` tinyint(4) NOT NULL DEFAULT 1,ADD COLUMN `status` tinyint(4) NOT NULL DEFAULT 1;

