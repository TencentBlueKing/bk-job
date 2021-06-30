SET NAMES utf8mb4;
USE job_analysis;

ALTER TABLE `statistics` ADD INDEX `idx_res_dim_dimValue_date`(`resource`(32), `dimension`(32), `dimension_value`(32),`date`(32)) USING BTREE;
