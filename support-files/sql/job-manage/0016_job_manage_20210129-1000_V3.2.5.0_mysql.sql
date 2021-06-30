use job_manage;

SET NAMES utf8mb4;

-- ----------------------------
-- Table structure for application_favor
-- ----------------------------
CREATE TABLE IF NOT EXISTS `application_favor`  (
  `username` varchar(255) CHARACTER SET utf8mb4 NOT NULL COMMENT '用户名称',
  `app_id` bigint(20) NOT NULL COMMENT '业务Id',
  `favor_time` bigint(20) NULL DEFAULT NULL COMMENT '收藏时间',
  PRIMARY KEY (`username`, `app_id`) USING BTREE,
  INDEX `idx_appId`(`app_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4;

