USE job_execute;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `statistics`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `app_id` bigint(20) NOT NULL COMMENT '业务id',
  `resource` varchar(128) NOT NULL COMMENT '统计的资源',
  `dimension` varchar(128) NOT NULL COMMENT '统计维度',
  `dimension_value` varchar(128) NOT NULL COMMENT '统计维度取值',
  `date` varchar(64) NOT NULL COMMENT '统计时间',
  `value` mediumtext NOT NULL COMMENT '统计值',
  `create_time` bigint(20) UNSIGNED NULL DEFAULT NULL COMMENT '创建时间',
  `last_modify_time` bigint(20) UNSIGNED NULL DEFAULT NULL COMMENT '更新时间',
  `row_create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `row_update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_date`(`date`(22)) USING BTREE,
  INDEX `idx_lastModifyTime`(`last_modify_time`) USING BTREE,
  INDEX `idx_res_dim_dimValue_date`(`resource`(32), `dimension`(32), `dimension_value`(32), `date`(32)) USING BTREE,
  INDEX `idx_appId_res_dim_dimValue_date`(`app_id`, `resource`(32), `dimension`(32), `dimension_value`(32), `date`(32)) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4;
