SET NAMES utf8mb4;
USE job_file_gateway;

ALTER TABLE `job_file_gateway`.`file_worker` ADD COLUMN `config_str` MEDIUMTEXT DEFAULT NULL COMMENT '配置字符串';

ALTER TABLE `job_file_gateway`.`file_source_type` ADD COLUMN `worker_id` bigint(20) DEFAULT NULL COMMENT '上报文件源类型的WorkerId';

ALTER TABLE `job_file_gateway`.`file_source` DROP COLUMN `region_code`;
ALTER TABLE `job_file_gateway`.`file_source` DROP COLUMN `region_name`;