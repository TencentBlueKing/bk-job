SET NAMES utf8mb4;
USE job_manage;

-- ----------------------------
-- Table structure for credential
-- ----------------------------
CREATE TABLE IF NOT EXISTS `credential`  (
  `id` varchar(34) CHARACTER SET utf8mb4 NOT NULL,
  `app_id` bigint(20) NOT NULL COMMENT '业务Id',
  `name` varchar(255) CHARACTER SET utf8mb4 NOT NULL COMMENT '凭证名称',
  `type` varchar(64) CHARACTER SET utf8mb4 NOT NULL COMMENT '凭证类型Code',
  `description` text CHARACTER SET utf8mb4 NULL COMMENT '凭证描述',
  `value` text CHARACTER SET utf8mb4 NULL,
  `creator` varchar(255) CHARACTER SET utf8mb4 NULL DEFAULT NULL COMMENT '创建人',
  `create_time` bigint(20) NULL DEFAULT NULL COMMENT '创建时间',
  `last_modify_user` varchar(255) CHARACTER SET utf8mb4 NULL DEFAULT NULL COMMENT '最后更新人',
  `last_modify_time` bigint(20) NULL DEFAULT NULL COMMENT '最后更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4;

