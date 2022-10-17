SET NAMES utf8mb4;
CREATE DATABASE IF NOT EXISTS job_file_gateway DEFAULT CHARACTER SET utf8mb4;
USE job_file_gateway;

-- ----------------------------
-- Table structure for file_worker_tag
-- ----------------------------
CREATE TABLE IF NOT EXISTS `file_worker_tag`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `worker_id` bigint(20) NOT NULL,
  `tag` varchar(255) CHARACTER SET utf8mb4 NOT NULL COMMENT '标签',
  `row_create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `unique_worker_tag`(`worker_id`, `tag`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4;
