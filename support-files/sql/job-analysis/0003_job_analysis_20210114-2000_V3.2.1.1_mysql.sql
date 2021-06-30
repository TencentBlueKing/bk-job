SET NAMES utf8mb4;
USE job_analysis;

ALTER TABLE `statistics` DROP INDEX `idx_appId_res_dim_dimValue`;

ALTER TABLE `statistics` ADD INDEX `idx_appId_res_dim_dimValue_date`(`app_id`,`resource`(32), `dimension`(32), `dimension_value`(32),`date`(32)) USING BTREE;