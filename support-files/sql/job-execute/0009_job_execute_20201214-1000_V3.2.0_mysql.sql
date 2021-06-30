USE job_execute;
SET NAMES utf8mb4;

-- ----------------------------
-- Table structure for file_source_task_log
-- ----------------------------
CREATE TABLE IF NOT EXISTS `file_source_task_log`  (
  `step_instance_id` bigint(20) NOT NULL DEFAULT 0,
  `execute_count` int(11) NOT NULL DEFAULT 0,
  `start_time` bigint(20) NULL DEFAULT NULL,
  `end_time` bigint(20) NULL DEFAULT NULL,
  `total_time` bigint(11) NULL DEFAULT NULL,
  `status` tinyint(4) NULL DEFAULT 1,
  `file_source_batch_task_id` varchar(34) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `row_create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `row_update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`step_instance_id`, `execute_count`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4;

