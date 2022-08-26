SET NAMES utf8mb4;
USE job_manage;

-- ----------------------------
-- Table structure for user_custom_setting
-- ----------------------------
CREATE TABLE IF NOT EXISTS `user_custom_setting`
(
    `username`         varchar(64) CHARACTER SET utf8mb4  NOT NULL COMMENT '用户名',
    `app_id`           bigint(20)                         NOT NULL COMMENT 'Job业务Id',
    `module`           varchar(32) CHARACTER SET utf8mb4  NOT NULL COMMENT '模块',
    `key`              varchar(128) CHARACTER SET utf8mb4 NOT NULL COMMENT '唯一键',
    `value`            text CHARACTER SET utf8mb4         NULL COMMENT '配置项的值Json串',
    `last_modify_user` varchar(255) CHARACTER SET utf8mb4 NULL     DEFAULT NULL COMMENT '最后更新人',
    `last_modify_time` bigint(20)                         NULL     DEFAULT NULL COMMENT '最后更新时间',
    `row_create_time`  datetime                           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `row_update_time`  datetime                           NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`username`, `app_id`, `module`) USING BTREE,
    INDEX idx_key (`key`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4;
