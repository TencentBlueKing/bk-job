SET NAMES utf8mb4;
CREATE DATABASE IF NOT EXISTS job_manage DEFAULT CHARACTER SET utf8mb4;
USE job_manage;

CREATE TABLE IF NOT EXISTS `script`
(
    `id`               VARCHAR(32)                    NOT NULL,
    `row_create_time`  DATETIME                       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `row_update_time`  DATETIME                       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `name`             VARCHAR(512)                   NOT NULL,
    `app_id`           BIGINT(20) UNSIGNED            NOT NULL,
    `type`             TINYINT(4) UNSIGNED            NOT NULL DEFAULT '1',
    `is_public`        TINYINT(1) UNSIGNED          DEFAULT '0',
    `creator`          VARCHAR(128)        NOT NULL,
    `create_time`      BIGINT(20) UNSIGNED            NOT NULL DEFAULT '0',
    `last_modify_user` VARCHAR(128)                 DEFAULT NULL,
    `last_modify_time` BIGINT(20) UNSIGNED            NOT NULL DEFAULT '0',
    `category`         TINYINT(1) UNSIGNED          DEFAULT '1',
    `description`      VARCHAR(1024),
    `is_deleted`       TINYINT(1) UNSIGNED          DEFAULT '0',
    `tags`             VARCHAR(512)                 DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY (`app_id`),
    KEY (`app_id`, `name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `script_version`
(
    `id`               BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    `row_create_time`   DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `row_update_time`   DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `script_id`        VARCHAR(32)                  DEFAULT NULL,
    `content`          LONGTEXT /*!99104 COMPRESSED */           NOT NULL,
    `creator`          VARCHAR(128)        NOT NULL,
    `create_time`      BIGINT(20) UNSIGNED            NOT NULL DEFAULT '0',
    `last_modify_user` VARCHAR(128)                 DEFAULT NULL,
    `last_modify_time` BIGINT(20) UNSIGNED            NOT NULL DEFAULT '0',
    `version`          VARCHAR(100)                 DEFAULT NULL,
    `is_deleted`       TINYINT(1) UNSIGNED          DEFAULT '0',
    `status`           TINYINT(1) UNSIGNED          DEFAULT '0',
    `version_desc`     VARCHAR(1024),
    PRIMARY KEY (`id`),
    KEY (`script_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT=1000000
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS  `tag`
(
    `id`               bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `row_create_time`  datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `row_update_time`  datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `app_id`           bigint(20) unsigned NOT NULL,
    `name`             varchar(512)        NOT NULL,
    `creator`          varchar(128)        NOT NULL,
    `last_modify_user` varchar(128)        NOT NULL,
    `is_deleted`       tinyint(1) unsigned NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_app_id_name` (`app_id`, `name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `task_favorite_plan`
(
    `id`              bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `row_create_time` datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `row_update_time` datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `app_id`          bigint(20) unsigned NOT NULL,
    `username`        varchar(128)        NOT NULL,
    `plan_id`         bigint(20) unsigned NOT NULL,
    `is_deleted`      tinyint(1) unsigned NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_app_id_username_template_id` (`app_id`, `username`, `plan_id`) USING BTREE,
    KEY `idx_username` (`username`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `task_favorite_template`
(
    `id`              bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `row_create_time` datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `row_update_time` datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `app_id`          bigint(20) unsigned NOT NULL,
    `username`        varchar(128)        NOT NULL,
    `template_id`     bigint(20) unsigned NOT NULL,
    `is_deleted`      tinyint(1) unsigned NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_app_id_username_template_id` (`app_id`, `username`, `template_id`) USING BTREE,
    KEY `idx_username` (`username`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `task_plan`
(
    `id`                bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `row_create_time`   datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `row_update_time`   datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `app_id`            bigint(20) unsigned NOT NULL,
    `template_id`       bigint(20) unsigned NOT NULL,
    `name`              varchar(512)        NOT NULL,
    `creator`           varchar(128)        NOT NULL,
    `type`              tinyint(1) unsigned NOT NULL DEFAULT '0',
    `is_deleted`        tinyint(1) unsigned NOT NULL DEFAULT '0',
    `create_time`       bigint(20) unsigned NOT NULL,
    `last_modify_user`  varchar(128)        NOT NULL,
    `last_modify_time`  bigint(20) unsigned NOT NULL DEFAULT '0',
    `first_step_id`     bigint(20) unsigned          DEFAULT NULL,
    `last_step_id`      bigint(20) unsigned          DEFAULT NULL,
    `version`           char(64)            NOT NULL,
    `is_latest_version` tinyint(1) unsigned NOT NULL DEFAULT '1',
    PRIMARY KEY (`id`),
    KEY `idx_template_id` (`template_id`) USING BTREE,
    KEY `idx_app_id` (`app_id`) USING BTREE,
    KEY `idx_creator` (`creator`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1000000
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `task_plan_step`
(
    `id`               bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `row_create_time`  datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `row_update_time`  datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `plan_id`          bigint(20) unsigned NOT NULL,
    `type`             tinyint(2) unsigned NOT NULL,
    `name`             varchar(512)        NOT NULL,
    `previous_step_id` bigint(20) unsigned NOT NULL,
    `next_step_id`     bigint(20) unsigned NOT NULL,
    `is_enable`        tinyint(1) unsigned NOT NULL,
    `is_deleted`       tinyint(1) unsigned NOT NULL DEFAULT '0',
    `script_step_id`   bigint(20) unsigned          DEFAULT NULL,
    `file_step_id`     bigint(20) unsigned          DEFAULT NULL,
    `approval_step_id` bigint(20) unsigned          DEFAULT NULL,
    `template_step_id` bigint(20) unsigned NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_plan_id` (`plan_id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `task_plan_step_approval`
(
    `id`               bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `row_create_time`  datetime                     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    `row_update_time`  datetime                     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    `step_id`          bigint(20) unsigned NOT NULL,
    `approval_type`    tinyint(2) unsigned NOT NULL DEFAULT '0',
    `approval_user`    varchar(255)        NOT NULL,
    `approval_message` varchar(2048)       NOT NULL,
    `notify_channel`   varchar(1024)       NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_step_id` (`step_id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `task_plan_step_file`
(
    `id`                        bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    `row_create_time`           datetime                     DEFAULT CURRENT_TIMESTAMP,
    `row_update_time`           datetime                     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `step_id`                   bigint(20) UNSIGNED NOT NULL,
    `destination_file_location` varchar(512)        NOT NULL,
    `execute_account`           bigint(20) UNSIGNED NOT NULL,
    `destination_host_list`     longtext /*!99104 COMPRESSED */,
    `timeout`                   bigint(20) UNSIGNED NOT NULL DEFAULT '0',
    `origin_speed_limit`        bigint(20) UNSIGNED NULL     DEFAULT NULL,
    `target_speed_limit`        bigint(20) UNSIGNED NULL     DEFAULT NULL,
    `ignore_error`              tinyint(1) UNSIGNED NOT NULL,
    `duplicate_handler`         tinyint(2) UNSIGNED NOT NULL DEFAULT '1',
	`not_exist_path_handler`	TINYINT(2) UNSIGNED DEFAULT 1,
    PRIMARY KEY (`id`),
    KEY `idx_step_id` (`step_id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `task_plan_step_file_list`
(
    `id`              bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `row_create_time` datetime                     DEFAULT CURRENT_TIMESTAMP,
    `row_update_time` datetime                     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `step_id`         bigint(20) unsigned NOT NULL,
    `file_type`       tinyint(2) unsigned NOT NULL DEFAULT '0',
    `file_location`   varchar(512)        NOT NULL,
    `file_size`       bigint(20) unsigned          DEFAULT NULL,
    `file_hash`       char(64)                     DEFAULT NULL,
    `host`            longtext /*!99104 COMPRESSED */,
    `host_account`    bigint(20) unsigned          DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_step_id` (`step_id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `task_plan_step_script`
(
    `id`                    bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `row_create_time`       datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `row_update_time`       datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `plan_id`               bigint(20) unsigned NOT NULL,
    `step_id`               bigint(20) unsigned NOT NULL,
    `script_type`           tinyint(2) unsigned NOT NULL,
    `script_id`             char(32)                     DEFAULT NULL,
    `script_version_id`     bigint(20) unsigned          DEFAULT NULL,
    `content`               longtext /*!99104 COMPRESSED */,
    `language`              tinyint(5) unsigned NOT NULL,
    `script_param`          varchar(512)                 DEFAULT NULL,
    `script_timeout`        bigint(20) unsigned NOT NULL,
    `execute_account`       bigint(20) unsigned NOT NULL,
    `destination_host_list` longtext /*!99104 COMPRESSED */,
    `is_secure_param`       tinyint(1) unsigned NOT NULL DEFAULT '0',
    `is_latest_version`     tinyint(1) unsigned NOT NULL DEFAULT '1',
    `ignore_error`          tinyint(1) unsigned NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_step_id` (`step_id`) USING BTREE,
    KEY `idx_script_id` (`script_id`) USING BTREE,
    KEY `idx_template_id` (`plan_id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `task_plan_variable`
(
    `id`                   bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `row_create_time`      datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `row_update_time`      datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `plan_id`              bigint(20) unsigned NOT NULL,
    `template_variable_id` bigint(20) unsigned NOT NULL,
    `name`                 varchar(255)        NOT NULL,
    `type`                 tinyint(2) unsigned NOT NULL,
    `default_value`        longtext,
    `description`          varchar(512)        NOT NULL,
    `is_changeable`        tinyint(1) unsigned NOT NULL,
    `is_required`          tinyint(1) unsigned NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_template_id` (`plan_id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `task_template`
(
    `id`                bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `row_create_time`   datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `row_update_time`   datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `app_id`            bigint(20) unsigned NOT NULL,
    `name`              varchar(512)        NOT NULL,
    `description`       varchar(2048)                DEFAULT NULL,
    `creator`           varchar(128)        NOT NULL,
    `status`            tinyint(2) unsigned NOT NULL DEFAULT '0',
    `is_deleted`        tinyint(1) unsigned NOT NULL DEFAULT '0',
    `create_time`       bigint(20) unsigned NOT NULL,
    `last_modify_user`  varchar(128)        NOT NULL,
    `last_modify_time`  bigint(20) unsigned NOT NULL DEFAULT '0',
    `tags`              varchar(512)                 DEFAULT NULL,
    `first_step_id`     bigint(20) unsigned          DEFAULT NULL,
    `last_step_id`      bigint(20) unsigned          DEFAULT NULL,
    `version`           char(64)            NOT NULL,
    `is_latest_version` tinyint(1) unsigned NOT NULL DEFAULT '1',
    PRIMARY KEY (`id`),
    KEY `idx_name` (`name`) USING BTREE,
    KEY `idx_creator` (`creator`) USING BTREE,
    KEY `idx_app_id` (`app_id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `task_template_step`
(
    `id`               bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `row_create_time`  datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `row_update_time`  datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `template_id`      bigint(20) unsigned NOT NULL,
    `type`             tinyint(2) unsigned NOT NULL DEFAULT '0',
    `name`             varchar(512)        NOT NULL,
    `previous_step_id` bigint(20) unsigned NOT NULL DEFAULT '0',
    `next_step_id`     bigint(20) unsigned NOT NULL DEFAULT '0',
    `is_deleted`       tinyint(1) unsigned NOT NULL DEFAULT '0',
    `script_step_id`   bigint(20) unsigned          DEFAULT NULL,
    `file_step_id`     bigint(20) unsigned          DEFAULT NULL,
    `approval_step_id` bigint(20) unsigned          DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_template_id` (`template_id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `task_template_step_approval`
(
    `id`               bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `row_create_time`  datetime                     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    `row_update_time`  datetime                     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    `step_id`          bigint(20) unsigned NOT NULL,
    `approval_type`    tinyint(2) unsigned NOT NULL DEFAULT '0',
    `approval_user`    varchar(255)        NOT NULL,
    `approval_message` varchar(2048)       NOT NULL,
    `notify_channel`   varchar(1024)       NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_step_id` (`step_id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `task_template_step_file`
(
    `id`                        bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    `row_create_time`           datetime                     DEFAULT CURRENT_TIMESTAMP,
    `row_update_time`           datetime                     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `step_id`                   bigint(20) UNSIGNED NOT NULL,
    `destination_file_location` varchar(512)        NOT NULL,
    `execute_account`           bigint(20) UNSIGNED NOT NULL,
    `destination_host_list`     longtext,
    `timeout`                   bigint(20) UNSIGNED NOT NULL DEFAULT '0',
    `origin_speed_limit`        bigint(20) UNSIGNED NULL     DEFAULT NULL,
    `target_speed_limit`        bigint(20) UNSIGNED NULL     DEFAULT NULL,
    `ignore_error`              tinyint(1) UNSIGNED NOT NULL,
    `duplicate_handler`         tinyint(2) UNSIGNED NOT NULL DEFAULT '1',
	`not_exist_path_handler`	TINYINT(2) UNSIGNED DEFAULT 1,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_step_id` (`step_id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `task_template_step_file_list`
(
    `id`              bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `row_create_time` datetime                     DEFAULT CURRENT_TIMESTAMP,
    `row_update_time` datetime                     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `step_id`         bigint(20) unsigned NOT NULL,
    `file_type`       tinyint(2) unsigned NOT NULL DEFAULT '0',
    `file_location`   varchar(512)        NOT NULL,
    `file_size`       bigint(20) unsigned          DEFAULT NULL,
    `file_hash`       char(64)                     DEFAULT NULL,
    `host`            longtext,
    `host_account`    bigint(20) unsigned          DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_step_id` (`step_id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `task_template_step_script`
(
    `id`                    bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `row_create_time`       datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `row_update_time`       datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `template_id`           bigint(20) unsigned NOT NULL,
    `step_id`               bigint(20) unsigned NOT NULL,
    `script_type`           tinyint(2) unsigned NOT NULL DEFAULT '0',
    `script_id`             char(32)                     DEFAULT NULL,
    `script_version_id`     bigint(20) unsigned          DEFAULT NULL,
    `content`               longtext /*!99104 COMPRESSED */,
    `language`              tinyint(5) unsigned NOT NULL,
    `script_param`          varchar(512)                 DEFAULT NULL,
    `script_timeout`        bigint(20) unsigned NOT NULL,
    `execute_account`       bigint(20) unsigned NOT NULL,
    `destination_host_list` longtext /*!99104 COMPRESSED */,
    `is_secure_param`       tinyint(1) unsigned NOT NULL DEFAULT '0',
    `is_latest_version`     tinyint(1) unsigned NOT NULL DEFAULT '1',
    `ignore_error`          tinyint(1) unsigned NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_step_id` (`step_id`) USING BTREE,
    KEY `idx_script_id` (`script_id`) USING BTREE,
    KEY `idx_template_id` (`template_id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `task_template_variable`
(
    `id`              bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `row_create_time` datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `row_update_time` datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `template_id`     bigint(20) unsigned NOT NULL,
    `name`            varchar(255)        NOT NULL,
    `type`            tinyint(2) unsigned NOT NULL,
    `default_value`   longtext,
    `description`     varchar(512)        NOT NULL,
    `is_changeable`   tinyint(1) unsigned NOT NULL,
    `is_required`     tinyint(1) unsigned NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_template_id` (`template_id`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1000000
  DEFAULT CHARSET = utf8mb4;


 CREATE TABLE IF NOT EXISTS `host`
(  `host_id` bigint(20) unsigned NOT NULL,
  `row_create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `row_update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `app_id` bigint(20) unsigned NOT NULL,
  `ip` varchar(15) NOT NULL,
  `ip_desc` varchar(2000) DEFAULT NULL,
  `set_ids` varchar(2048) DEFAULT NULL,
  `module_ids` varchar(2048) DEFAULT NULL,
  `cloud_area_id` bigint(20) unsigned NOT NULL,
  `display_ip` varchar(1024) NOT NULL,
  `os` varchar(512) DEFAULT '',
  `module_type` varchar(2048) DEFAULT '1' COMMENT '模块类型数组字符串',
  `is_agent_alive` tinyint(4) unsigned DEFAULT '1',
  PRIMARY KEY (`host_id`),
  KEY `idx_app_ip_cloud_area_ip` (`app_id`,`ip`,`cloud_area_id`) USING BTREE,
  KEY `idx_ip_cloud_area_id` (`ip`,`cloud_area_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

  CREATE TABLE IF NOT EXISTS `application`
(
  `app_id` bigint(20) unsigned NOT NULL,
  `row_create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `row_update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `app_name` varchar(1024) DEFAULT NULL,
  `maintainers` varchar(8192) DEFAULT NULL,
  `bk_supplier_account` varchar(128) NOT NULL DEFAULT '0',
  `app_type` tinyint(4) NOT NULL DEFAULT '1',
  `sub_app_ids` text /*!99104 COMPRESSED */,
  `timezone` varchar(128) DEFAULT 'Asia/Shanghai',
  `bk_operate_dept_id` BIGINT(20) DEFAULT NULL,
  `language` VARCHAR(20) DEFAULT NULL,
  PRIMARY KEY (`app_id`),
  KEY `app_type` (`app_type`)
) ENGINE = INNODB
  DEFAULT CHARSET = utf8mb4;
  

CREATE TABLE IF NOT EXISTS `account` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `row_create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `row_update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `account` varchar(255) NOT NULL,
  `alias` varchar(255) DEFAULT NULL,
  `category` tinyint(4) NOT NULL,
  `type` tinyint(4) NOT NULL,
  `app_id` bigint(20) NOT NULL,
  `grantee` longtext DEFAULT NULL,
  `remark` varchar(1024) DEFAULT NULL,
  `os` varchar(32) DEFAULT 'Linux',
  `password` varchar(255) DEFAULT NULL,
  `db_password` varchar(255) DEFAULT NULL,
  `db_port` int(5) DEFAULT NULL,
  `db_system_account_id` bigint(20) DEFAULT NULL,
  `creator` varchar(128) NOT NULL,
  `create_time`      BIGINT(20) UNSIGNED            NOT NULL DEFAULT '0',
  `is_deleted`       TINYINT(1) UNSIGNED          DEFAULT '0',
  `last_modify_user` varchar(128) DEFAULT NULL,
  `last_modify_time` BIGINT(20) UNSIGNED            NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY (`app_id`,`category`,`alias`)
)  ENGINE=INNODB DEFAULT CHARSET=UTF8MB4;

-- ----------------------------
-- IP白名单部分：begin
-- ----------------------------

-- ----------------------------
-- Table structure for action_scope
-- ----------------------------
CREATE TABLE IF NOT EXISTS `action_scope`  (
  `id` bigint(20) NOT NULL,
  `code` VARCHAR(128) COMMENT '白名单IP生效范围Code',
  `name` varchar(255) CHARACTER SET utf8mb4 NOT NULL,
  `description` text CHARACTER SET utf8mb4 NULL,
  `row_create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `row_update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0),
  `creator` varchar(128) CHARACTER SET utf8mb4 NOT NULL,
  `last_modify_user` varchar(128) CHARACTER SET utf8mb4 NULL DEFAULT NULL,
  `create_time` bigint(20) UNSIGNED NULL DEFAULT NULL,
  `last_modify_time` bigint(20) UNSIGNED NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4;


-- ----------------------------
-- Records of action_scope
-- ----------------------------
REPLACE INTO `action_scope` VALUES (1, 'SCRIPT_EXECUTE', '脚本执行', '脚本执行', '2019-12-26 22:08:48', '2020-02-11 23:07:28', 'admin', 'admin', 1577369328, 1577369328);
REPLACE INTO `action_scope` VALUES (2, 'FILE_DISTRIBUTION', '文件分发', '文件分发', '2019-12-26 22:08:48', '2020-02-11 23:07:28', 'admin', 'admin', 1577369328, 1577369328);


-- ----------------------------
-- Table structure for white_ip_ip
-- ----------------------------
CREATE TABLE IF NOT EXISTS `white_ip_ip`  (
                                              `id`               bigint(20)                                                   NOT NULL AUTO_INCREMENT,
                                              `record_id`        bigint(20)                                                   NOT NULL,
                                              `ip`               varchar(16) CHARACTER SET utf8mb4                            NOT NULL,
                                              `cloud_area_id`    bigint(20)                                                   NOT NULL,
                                              `row_create_time`  datetime(0)                                                  NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                              `row_update_time`  datetime(0)                                                  NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0),
                                              `creator`          varchar(128) CHARACTER SET utf8mb4                           NOT NULL,
                                              `last_modify_user` varchar(128) CHARACTER SET utf8mb4                           NULL     DEFAULT NULL,
                                              `create_time`      bigint(20) UNSIGNED                                          NULL     DEFAULT NULL,
                                              `last_modify_time` bigint(20) UNSIGNED                                          NULL     DEFAULT NULL,
                                              PRIMARY KEY (`id`) USING BTREE,
                                              INDEX `idx_record_id` (`record_id`) USING BTREE,
                                              INDEX `idx_ip` (`ip`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4;


-- ----------------------------
-- Table structure for white_ip_action_scope
-- ----------------------------
CREATE TABLE IF NOT EXISTS `white_ip_action_scope`
(
    `id`               bigint(20)                                             NOT NULL AUTO_INCREMENT,
    `record_id`        bigint(20)                                             NOT NULL COMMENT '白名单id',
    `action_scope_id`  bigint(20)                                             NOT NULL DEFAULT 1 COMMENT '生效范围',
    `row_create_time`  datetime(0)                                            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `row_update_time`  datetime(0)                                            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0),
    `creator`          varchar(128) CHARACTER SET utf8mb4                     NOT NULL,
    `last_modify_user` varchar(128) CHARACTER SET utf8mb4                     NULL     DEFAULT NULL,
    `create_time`      bigint(20) UNSIGNED                                    NULL     DEFAULT NULL,
    `last_modify_time` bigint(20) UNSIGNED                                    NULL     DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_record_id` (`record_id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4;


-- ----------------------------
-- Table structure for white_ip_record
-- ----------------------------
CREATE TABLE IF NOT EXISTS `white_ip_record`
(
    `id`               bigint(20)                                             NOT NULL AUTO_INCREMENT,
    `remark`           text CHARACTER SET utf8mb4                             NOT NULL COMMENT '备注',
    `row_create_time`  datetime(0)                                            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `row_update_time`  datetime(0)                                            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0),
    `creator`          varchar(128) CHARACTER SET utf8mb4                     NOT NULL,
    `last_modify_user` varchar(128) CHARACTER SET utf8mb4                     NULL     DEFAULT NULL,
    `create_time`      bigint(20) UNSIGNED                                    NULL     DEFAULT NULL,
    `last_modify_time` bigint(20) UNSIGNED                                    NULL     DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4;


-- ----------------------------
-- IP白名单部分：end
-- ----------------------------

-- ----------------------------
-- 消息通知部分：begin
-- ----------------------------


-- ----------------------------
-- Table structure for available_esb_channel
-- ----------------------------
CREATE TABLE IF NOT EXISTS `available_esb_channel`  (
  `type` varchar(255) CHARACTER SET utf8mb4 NOT NULL COMMENT '消息类型',
  `enable` bit(1) NOT NULL DEFAULT b'1' COMMENT '超级管理员是否启用该渠道',
  `creator` varchar(255) CHARACTER SET utf8mb4 NULL DEFAULT NULL COMMENT '启用通道的超级管理员名称',
  `last_modify_time` timestamp(0) NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`type`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4;



-- ----------------------------
-- Table structure for esb_user_info
-- ----------------------------
CREATE TABLE IF NOT EXISTS  `esb_user_info`  (
  `id` bigint(20) NOT NULL,
  `username` varchar(255) CHARACTER SET utf8mb4 NOT NULL,
  `logo` TEXT NULL,
  `display_name` varchar(255) CHARACTER SET utf8mb4 NULL DEFAULT NULL,
  `row_update_time` timestamp(0) NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0),
  `last_modify_time` bigint(20) UNSIGNED NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_username`(`username`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4;



-- ----------------------------
-- Table structure for notify_black_user_info
-- ----------------------------
CREATE TABLE IF NOT EXISTS `notify_black_user_info`
(
    `id`               bigint(20)                                                    NOT NULL AUTO_INCREMENT,
    `username`         varchar(255) CHARACTER SET utf8mb4                            NOT NULL,
    `creator`          varchar(255) CHARACTER SET utf8mb4                            NULL DEFAULT NULL,
    `last_modify_time` bigint(20) UNSIGNED                                           NULL DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_username` (`username`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4;


-- ----------------------------
-- Table structure for notify_config_status
-- ----------------------------
CREATE TABLE IF NOT EXISTS `notify_config_status`
(
    `app_id`           bigint(20)                                                    NOT NULL,
    `username`         varchar(255) CHARACTER SET utf8mb4                            NOT NULL,
    `last_modify_time` bigint(20) UNSIGNED                                           NULL DEFAULT NULL,
    PRIMARY KEY (`app_id`, `username`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4;


-- ----------------------------
-- Table structure for notify_policy_role_target
-- ----------------------------
CREATE TABLE IF NOT EXISTS `notify_policy_role_target`  (
                                                            `id`               bigint(20)                                                    NOT NULL AUTO_INCREMENT,
                                                            `policy_id`        bigint(20)                                                    NOT NULL COMMENT '策略Id',
                                                            `role`             varchar(255) CHARACTER SET utf8mb4                            NOT NULL COMMENT '角色编码（RESOURCE_OWNER/EXECUTOR/APP_MANAGER/EXTRA_OBSERVER）（作业平台角色：资源所属者，任务执行人，额外通知人+CMDB业务角色）',
                                                            `enable`           bit(1)                                                        NOT NULL COMMENT '是否启用',
                                                            `extra_observers`  text CHARACTER SET utf8mb4                                    NULL COMMENT '额外通知者',
                                                            `creator`          varchar(128) CHARACTER SET utf8mb4                            NOT NULL,
                                                            `row_update_time`  datetime(0)                                                   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0),
                                                            `last_modify_user` varchar(128) CHARACTER SET utf8mb4                            NULL     DEFAULT NULL,
                                                            `create_time`      bigint(20) UNSIGNED                                           NULL     DEFAULT NULL,
                                                            `last_modify_time` bigint(20) UNSIGNED                                           NULL     DEFAULT NULL,
                                                            PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4;


-- ----------------------------
-- Table structure for notify_role_target_channel
-- ----------------------------
CREATE TABLE IF NOT EXISTS `notify_role_target_channel`
(
    `id`               bigint(20)                                                    NOT NULL AUTO_INCREMENT,
    `role_target_id`   bigint(20)                                                    NOT NULL,
    `channel`          varchar(255) CHARACTER SET utf8mb4                            NOT NULL,
    `creator`          varchar(128) CHARACTER SET utf8mb4                            NOT NULL,
    `row_create_time`  datetime(0)                                                   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `row_update_time`  datetime(0)                                                   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0),
    `last_modify_user` varchar(128) CHARACTER SET utf8mb4                            NULL     DEFAULT NULL,
    `create_time`      bigint(20) UNSIGNED                                           NULL     DEFAULT NULL,
    `last_modify_time` bigint(20) UNSIGNED                                           NULL     DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4;


-- ----------------------------
-- Table structure for notify_trigger_policy
-- ----------------------------
CREATE TABLE IF NOT EXISTS `notify_trigger_policy`
(
    `id`               bigint(20)                                                    NOT NULL AUTO_INCREMENT,
    `app_id`           bigint(20)                                                    NOT NULL,
    `resource_id`      varchar(255) CHARACTER SET utf8mb4                            NOT NULL COMMENT '执行内容（脚本/作业）id',
    `resource_type`    tinyint(4)                                                    NOT NULL COMMENT '执行内容类型码',
    `trigger_user`     varchar(128) CHARACTER SET utf8mb4                            NOT NULL,
    `trigger_type`     tinyint(4)                                                    NOT NULL COMMENT '触发类型（页面/API/定时）编码',
    `execute_status`   tinyint(4)                                                    NOT NULL COMMENT '执行状态编码',
    `creator`          varchar(128) CHARACTER SET utf8mb4                            NOT NULL,
    `row_create_time`  datetime(0)                                                   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `row_update_time`  datetime(0)                                                   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0),
    `last_modify_user` varchar(128) CHARACTER SET utf8mb4                            NOT NULL,
    `create_time`      bigint(20) UNSIGNED                                           NULL     DEFAULT NULL,
    `last_modify_time` bigint(20) UNSIGNED                                           NULL     DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4;

-- ----------------------------
-- 消息通知部分：end
-- ----------------------------

-- ----------------------------
-- Table structure for global_setting
-- ----------------------------
CREATE TABLE IF NOT EXISTS `global_setting`
(
    `key`      varchar(255) CHARACTER SET utf8mb4 NOT NULL,
  `value` text CHARACTER SET utf8mb4 NULL,
  `decription` varchar(255) CHARACTER SET utf8mb4 NULL DEFAULT NULL
) ENGINE = InnoDB CHARACTER SET = utf8mb4;


CREATE TABLE IF NOT EXISTS `dangerous_rule`  (
                                                 `id`               bigint(20)                                                    NOT NULL AUTO_INCREMENT,
                                                 `expression`       varchar(255) CHARACTER SET utf8mb4                            NOT NULL COMMENT '表达式',
                                                 `script_type`      TINYINT                                                            DEFAULT 1 COMMENT '脚本类型',
                                                 `description`      text CHARACTER SET utf8mb4                                    NULL COMMENT '描述',
                                                 `priority`         int(11)                                                       NULL DEFAULT NULL COMMENT '优先级',
                                                 `creator`          varchar(128) CHARACTER SET utf8mb4                            NOT NULL,
                                                 `last_modify_user` varchar(128) CHARACTER SET utf8mb4                            NULL DEFAULT NULL,
                                                 `create_time`      bigint(20) UNSIGNED                                           NULL DEFAULT NULL,
                                                 `last_modify_time` bigint(20) UNSIGNED                                           NULL DEFAULT NULL,
                                                 PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4;


CREATE TABLE IF NOT EXISTS `index_greeting`
(
    `id`               bigint(20)                                             NOT NULL AUTO_INCREMENT,
    `start_seconds`    int(10)                                                NOT NULL COMMENT '开始生效的时间（秒）',
    `end_seconds`      int(10)                                                NOT NULL COMMENT '结束生效的时间（秒）',
    `content`          text CHARACTER SET utf8mb4                             NOT NULL COMMENT '问候内容，${time}为时间占位符',
	`content_en` 	   TEXT                                                   NULL COMMENT '英文版内容',
    `priority`         int(10)                                                NOT NULL COMMENT '优先级',
    `active`           bit(1)                                                 NOT NULL COMMENT '是否启用',
    `description`      TEXT COMMENT '描述',
    `creator`          varchar(128) CHARACTER SET utf8mb4                     NOT NULL COMMENT '创建者',
    `last_modify_user` varchar(128) CHARACTER SET utf8mb4                     NULL DEFAULT NULL COMMENT '更新者',
    `create_time`      bigint(20) UNSIGNED                                    NULL DEFAULT NULL COMMENT '创建时间',
    `last_modify_time` bigint(20) UNSIGNED                                    NULL DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4;


REPLACE INTO `index_greeting`(`id`, `start_seconds`, `end_seconds`, `content`, `priority`, `active`, `creator`,
                             `last_modify_user`, `create_time`, `last_modify_time`, `description`)
VALUES (2, 82800, 86400, '现在是${time}，夜深了... 请注意休息，劳逸结合！', 2, b'1', 'admin', 'admin', 1, 1, NULL);
REPLACE INTO `index_greeting`(`id`, `start_seconds`, `end_seconds`, `content`, `priority`, `active`, `creator`,
                             `last_modify_user`, `create_time`, `last_modify_time`, `description`)
VALUES (3, 0, 3600, '现在是${time}，夜深了... 请注意休息，劳逸结合！', 2, b'1', 'admin', 'admin', 1, 1, NULL);
REPLACE INTO `index_greeting`(`id`, `start_seconds`, `end_seconds`, `content`, `priority`, `active`, `creator`,
                             `last_modify_user`, `create_time`, `last_modify_time`, `description`)
VALUES (4, 3600, 14400, '现在是${time}，午夜了！切忌劳累过度，影响身体且易误操作，请赶紧休息吧...', 2, b'1', 'admin', 'admin', 1, 1, NULL);
REPLACE INTO `index_greeting`(`id`, `start_seconds`, `end_seconds`, `content`, `priority`, `active`, `creator`,
                             `last_modify_user`, `create_time`, `last_modify_time`, `description`)
VALUES (6, 14400, 25200, '现在是${time}，一年之计在于春、一日之计在于晨！早起的鸟儿有虫吃~ 伙计，加油！', 2, b'1', 'admin', 'admin', 1, 1, NULL);
REPLACE INTO `index_greeting`(`id`, `start_seconds`, `end_seconds`, `content`, `priority`, `active`, `creator`,
                             `last_modify_user`, `create_time`, `last_modify_time`, `description`)
VALUES (7, 25200, 42000, '现在是${time}，早上好！专心工作时别忘了多喝水，促进身体新陈代谢，有益身体健康噢~', 2, b'1', 'admin', 'admin', 1, 1, NULL);
REPLACE INTO `index_greeting`(`id`, `start_seconds`, `end_seconds`, `content`, `priority`, `active`, `creator`,
                             `last_modify_user`, `create_time`, `last_modify_time`, `description`)
VALUES (8, 42000, 45000, '现在是${time}，午饭时间到了，记得按时就餐，保护好肠胃~', 2, b'1', 'admin', 'admin', 1, 1, NULL);
REPLACE INTO `index_greeting`(`id`, `start_seconds`, `end_seconds`, `content`, `priority`, `active`, `creator`,
                             `last_modify_user`, `create_time`, `last_modify_time`, `description`)
VALUES (9, 45000, 50400, '现在是${time}，午饭过后，散步几分、小歇片刻，下午办公精神更佳！', 2, b'1', 'admin', 'admin', 1, 1, NULL);
REPLACE INTO `index_greeting`(`id`, `start_seconds`, `end_seconds`, `content`, `priority`, `active`, `creator`,
                             `last_modify_user`, `create_time`, `last_modify_time`, `description`)
VALUES (10, 50400, 64800, '现在是${time}，下午好！预防「久坐成疾」，工作之余，记得要多站起来走走，扭扭腰、松松肩颈，放松片刻。', 2, b'1', 'admin', 'admin', 1, 1,
        NULL);
REPLACE INTO `index_greeting`(`id`, `start_seconds`, `end_seconds`, `content`, `priority`, `active`, `creator`, `last_modify_user`, `create_time`, `last_modify_time`, `description`) VALUES (11, 64800, 70200, '现在是${time}，晚上好！记得按时用膳，夜间人体消化能力较弱，饮食不要太饱满，健康绿色膳食为宜。', 2, b'1', 'admin', 'admin', 1, 1, NULL);
REPLACE INTO `index_greeting`(`id`, `start_seconds`, `end_seconds`, `content`, `priority`, `active`, `creator`, `last_modify_user`, `create_time`, `last_modify_time`, `description`) VALUES (12, 70200, 82800, '现在是${time}，晚上好！活是永远做不完的，只要每天规划好待办事项，按期交付即可；少加班，多锻炼噢~', 2, b'1', 'admin', 'admin', 1, 1, NULL);


CREATE TABLE IF NOT EXISTS `analysis_task`  (
                                                `id`                          bigint(20)                                                    NOT NULL AUTO_INCREMENT,
                                                `code`                        varchar(255) CHARACTER SET utf8mb4 NOT NULL COMMENT '任务代码，用于匹配处理器',
                                                `app_ids`                     text CHARACTER SET utf8mb4         NOT NULL COMMENT '生效的appId，null为全部生效',
                                                `result_description_template` TEXT COMMENT '任务结果总体描述模板',
												`result_description_template_en` TEXT NULL COMMENT '英文版分析结果总体描述模板内容',
                                                `result_item_template`        text CHARACTER SET utf8mb4         NOT NULL COMMENT '每条任务结果描述模板',
												`result_item_template_en` 	  TEXT NULL COMMENT '英文版分析结果子项模板内容',
                                                `priority`                    int(10)                                                       NOT NULL COMMENT '优先级',
                                                `active`                      bit(1)                                                        NOT NULL COMMENT '是否启用',
                                                `period_seconds`              bigint(20)                                                    NOT NULL COMMENT '触发周期',
                                                `creator`                     varchar(128) CHARACTER SET utf8mb4        NOT NULL COMMENT '创建者',
                                                `last_modify_user`            varchar(128) CHARACTER SET utf8mb4        NULL DEFAULT NULL COMMENT '更新者',
                                                `create_time`                 bigint(20) UNSIGNED                                           NULL DEFAULT NULL COMMENT '创建时间',
                                                `last_modify_time`            bigint(20) UNSIGNED                                           NULL DEFAULT NULL COMMENT '更新时间',
                                                `description`                 TEXT COMMENT '对任务的描述',
                                                PRIMARY KEY (`id`) USING BTREE,
                                                UNIQUE INDEX `idx_code` (`code`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4;


CREATE TABLE IF NOT EXISTS `analysis_task_instance`
(
    `id`               bigint(20)                                                  NOT NULL AUTO_INCREMENT,
    `app_id`           bigint(20)                                                  NOT NULL COMMENT '业务id',
    `task_id`          bigint(255)                                                 NOT NULL COMMENT '任务id',
    `status`           int(10)                                                     NOT NULL COMMENT '任务状态',
    `result_data`      MEDIUMTEXT CHARACTER SET utf8mb4 NOT NULL COMMENT '任务结果数据，各任务自定义格式',
    `priority`         int(10)                                                     NOT NULL COMMENT '优先级',
    `active`           bit(1)                                                      NOT NULL COMMENT '是否启用',
    `creator`          varchar(128) CHARACTER SET utf8mb4      NOT NULL COMMENT '创建者',
    `last_modify_user` varchar(128) CHARACTER SET utf8mb4      NULL DEFAULT NULL COMMENT '更新者',
    `create_time`      bigint(20) UNSIGNED                                         NULL DEFAULT NULL COMMENT '创建时间',
    `last_modify_time` bigint(20) UNSIGNED                                         NULL DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_appId_taskId` (`app_id`, `task_id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4;
  
-- ------------------------
-- 增加静态分析结果数据表
-- ------------------------
CREATE TABLE IF NOT EXISTS `analysis_task_static_instance`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `app_id` bigint(20) NOT NULL COMMENT '业务id',
  `task_id` bigint(255) NOT NULL COMMENT '任务id',
  `status` int(10) NOT NULL COMMENT '任务状态',
  `result_data` mediumtext CHARACTER SET utf8mb4 NOT NULL COMMENT '任务结果数据，各任务自定义格式',
  `result_data_en` mediumtext CHARACTER SET utf8mb4 NULL COMMENT '任务结果数据（英文）',
  `priority` int(10) NOT NULL COMMENT '优先级',
  `active` bit(1) NOT NULL COMMENT '是否启用',
  `creator` varchar(128) CHARACTER SET utf8mb4 NOT NULL COMMENT '创建者',
  `last_modify_user` varchar(128) CHARACTER SET utf8mb4 NULL DEFAULT NULL COMMENT '更新者',
  `create_time` bigint(20) UNSIGNED NULL DEFAULT NULL COMMENT '创建时间',
  `last_modify_time` bigint(20) UNSIGNED NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_appId_taskId`(`app_id`, `task_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 ;


REPLACE INTO `analysis_task_static_instance`(`id`, `app_id`, `task_id`, `status`, `result_data`, `result_data_en`, `priority`, `active`, `creator`, `last_modify_user`, `create_time`, `last_modify_time`) VALUES (1, -1, -1, 2, '使用作业平台 Shell 脚本的内置函数 `job_success` `job_fail`，可以轻松实现简单的执行结果归类分组效果；更多使用技巧，详见文档 <a>https://bk.tencent.com/docs/</a>', 'Using built-in functions `job_success` `job_fail`, execution results can be grouped easily. Find out more tips with docs <a>https://bk.tencent.com/docs/</a>.', 100, b'1', 'admin', 'admin', 1583492717314, 1583492717318);
REPLACE INTO `analysis_task_static_instance`(`id`, `app_id`, `task_id`, `status`, `result_data`, `result_data_en`, `priority`, `active`, `creator`, `last_modify_user`, `create_time`, `last_modify_time`) VALUES (2, -1, -1, 2, '脚本传参涉及到敏感信息（如密码）该如何规避因明文传输而导致信息泄露的风险？密码 变量可以帮你解决这个问题！使用案例详见[文档](https://bk.tencent.com/docs/)', 'Params involves sensitive information (such as passwords). How to avoid the risk of information leakage? The `password` variable can help you solve this problem! For more information and examples see docs (https://bk.tencent.com/docs/).', 100, b'1', 'admin', 'admin', 1583492717314, 1583492717318);
REPLACE INTO `analysis_task_static_instance`(`id`, `app_id`, `task_id`, `status`, `result_data`, `result_data_en`, `priority`, `active`, `creator`, `last_modify_user`, `create_time`, `last_modify_time`) VALUES (3, -1, -1, 2, '为你的作业模板设置一个标签可以方便你更好的进行分类管理，在「作业管理」页面左侧可以快速通过分类标签找到你的作业模板。', 'Job tags can help you manage template classification better, you can easily toggle different tags in left side on Jobs page.', 100, b'1', 'admin', 'admin', 1583492717314, 1583492717318);
REPLACE INTO `analysis_task_static_instance`(`id`, `app_id`, `task_id`, `status`, `result_data`, `result_data_en`, `priority`, `active`, `creator`, `last_modify_user`, `create_time`, `last_modify_time`) VALUES (4, -1, -1, 2, '收藏的作业模板可以显示在首页中，这样你就能更快的找到你需要执行的作业了。\r\n常用的脚本语言语法或方法使用技巧的推荐', 'Favorite Jobs can be displayed on the homepage, so you can find the job you need faster when you came to Job.', 100, b'1', 'admin', 'admin', 1583492717314, 1583492717318);
REPLACE INTO `analysis_task_static_instance`(`id`, `app_id`, `task_id`, `status`, `result_data`, `result_data_en`, `priority`, `active`, `creator`, `last_modify_user`, `create_time`, `last_modify_time`) VALUES (5, -1, -1, 2, '你知道么，Shell 也支持数组变量喔~ 写法示例： `var[0]=\'test1\'; var[1]=\'test2\';` 通过 `echo ${var[*]}` 可以打印变量的所有索引值，`echo ${#var[*]}` 可以打印变量一共有多少个索引。', 'You know what, Array variable is supported in Bash. How? `var[0]=\'test1\'; var[1]=\'test2\';` and using `echo ${var[*]}` to print all index values of Array variable, using `echo ${#var[*]}` to show how many index the Array variable has.', 100, b'1', 'admin', 'admin', 1583492717314, 1583492717318);


CREATE TABLE IF NOT EXISTS `white_ip_app_rel`
(
    `record_id`   bigint(20)                                             NOT NULL AUTO_INCREMENT,
    `app_id`      bigint(20)                                             NOT NULL COMMENT '业务ID',
    `creator`     varchar(128) CHARACTER SET utf8mb4 NOT NULL,
    `create_time` bigint(20) UNSIGNED                                    NULL DEFAULT NULL,
    PRIMARY KEY (`record_id`, `app_id`) USING BTREE,
    INDEX `idx_record_id` (`record_id`) USING BTREE,
    INDEX `idx_app_id` (`app_id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4;


REPLACE INTO `analysis_task`(`id`, `code`, `app_ids`, `result_description_template`, `result_description_template_en`, `result_item_template`, `result_item_template_en`, `priority`, `active`, `period_seconds`, `creator`, `last_modify_user`, `create_time`, `last_modify_time`, `description`) VALUES (-1, 'DefaultTipsProvider', '', NULL, NULL, '', NULL, 1, b'1', 3600, 'admin', 'admin', 1, 1, NULL);
REPLACE INTO `analysis_task`(`id`, `code`, `app_ids`, `result_description_template`, `result_description_template_en`, `result_item_template`, `result_item_template_en`, `priority`, `active`, `period_seconds`, `creator`, `last_modify_user`, `create_time`, `last_modify_time`, `description`) VALUES (2, 'TimerTaskFailWatcher', '', '系统发现多个运行中的定时任务在近期出现了执行失败的情况，请关注！', 'Attention! System has detected that many Crons have running failed recently, please click Details to check if there is problems in it.', '定时任务【${taskName}】在近期出现了执行失败的问题，请留意。', 'Caution: Cron[${taskName}] has executed failed recently.', 1, b'1', 1200, 'admin', 'admin', 1, 1, NULL);
REPLACE INTO `analysis_task`(`id`, `code`, `app_ids`, `result_description_template`, `result_description_template_en`, `result_item_template`, `result_item_template_en`, `priority`, `active`, `period_seconds`, `creator`, `last_modify_user`, `create_time`, `last_modify_time`, `description`) VALUES (3, 'TimerTaskFailRateWatcher', '', '系统发现多个运行中的定时任务周期执行成功率低于 60%，请关注！', 'Attention! System has detected that many Crons success rate is lower than 60%， it seems there are some problems, please pay attention.', '定时任务【${taskName}】周期执行成功率低于 60%，请留意。', 'Caution: Cron[${taskName}] success-rate is lower than 60%.', 1, b'1', 1200, 'admin', 'admin', 1, 1, NULL);
REPLACE INTO `analysis_task`(`id`, `code`, `app_ids`, `result_description_template`, `result_description_template_en`, `result_item_template`, `result_item_template_en`, `priority`, `active`, `period_seconds`, `creator`, `last_modify_user`, `create_time`, `last_modify_time`, `description`) VALUES (4, 'ForbiddenScriptFinder', '', '系统发现有多个作业模板/执行方案使用了【已禁用】状态的脚本版本，被禁用脚本版本将无法正常执行，请及时处理。', 'Caution! System has detected that many Jobs using the script version in \"Banned\" status, it will not able to execute， please handle it ASAP.', '${typeName}：${instanceName}的步骤【$stepName】使用了 已禁用 状态的脚本版本，该步骤将无法正常执行，请关注！', 'Caution: Job[${instanceName}]\'s step[$stepName] uses script version in \"Banned\" state, which will not be executed properly.', 1, b'1', 2400, 'admin', 'admin', 1, 1, NULL);
REPLACE INTO `analysis_task`(`id`, `code`, `app_ids`, `result_description_template`, `result_description_template_en`, `result_item_template`, `result_item_template_en`, `priority`, `active`, `period_seconds`, `creator`, `last_modify_user`, `create_time`, `last_modify_time`, `description`) VALUES (5, 'TaskPlanTargetChecker', '', '系统发现多个作业模板/执行方案的步骤中存在执行目标【Agent状态异常】的情况，请关注。', 'Caution! System has detected that many Jobs using Abnormal status Host, it will cause the job fail to execute, please handle it ASAP.', '作业：${planName}的步骤【$stepName】的执行目标存在异常：【$description】，请关注。', 'Caution: Job plan[${planName}]\'s step[$stepName] contains abnormal hosts.', 1, b'1', 7200, 'admin', 'admin', 1, 1, NULL);




-- ------------------------
-- 增加消息通知模板表
-- ------------------------
CREATE TABLE IF NOT EXISTS `notify_template`  (
                                                  `id`               int(11)                                                       NOT NULL AUTO_INCREMENT,
                                                  `code`             varchar(64) CHARACTER SET utf8        NOT NULL,
                                                  `name`             varchar(128) CHARACTER SET utf8       NOT NULL,
                                                  `channel`          varchar(64) CHARACTER SET utf8        NOT NULL,
                                                  `title`            text CHARACTER SET utf8               NOT NULL,
                                                  `content`          mediumtext CHARACTER SET utf8         NOT NULL,
                                                  `title_en`         text CHARACTER SET utf8               NULL,
                                                  `content_en`       mediumtext CHARACTER SET utf8         NULL,
                                                  `creator`          varchar(128) CHARACTER SET utf8mb4 NOT NULL,
                                                  `last_modify_user` varchar(128) CHARACTER SET utf8mb4        NOT NULL,
                                                  `create_time`      bigint(20) UNSIGNED                                           NOT NULL,
                                                  `last_modify_time` bigint(20) UNSIGNED                                           NOT NULL,
                                                  PRIMARY KEY (`id`) USING BTREE,
                                                  INDEX `idx_code` (`code`) USING BTREE,
                                                  INDEX `idx_name` (`name`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  CHARACTER SET = utf8;

-- ------------------------
-- 插入初始化数据
-- ------------------------
REPLACE INTO `notify_template`(`id`, `code`, `name`, `channel`, `title`, `content`, `title_en`, `content_en`, `creator`,
                              `last_modify_user`, `create_time`, `last_modify_time`)
VALUES (1, 'beforeCronJobExecute', '定时任务执行前通知模板', 'mail', '【蓝鲸作业平台】定时任务提醒消息',
        '<table class=\"template-table\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"font-size: 14px; min-width: auto; mso-table-lspace: 0pt; mso-table-rspace: 0pt; background-color: #fff; background: #fff;\">\r\n	<tbody>\r\n		<tr>\r\n			<td align=\"center\" valign=\"top\" width=\"100%\" style=\"padding: 16px;\">\r\n			   <table class=\"template-table\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"956\" style=\"font-size: 14px; min-width: auto; mso-table-lspace: 0pt; mso-table-rspace: 0pt; padding-top:30px;\">\r\n					<tbody>\r\n						<tr>\r\n							<td valign=\"top\" align=\"center\" style=\"padding: 24px;\" bgcolor=\"#f9f8f6\">\r\n								<table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"font-size: 14px; mso-table-lspace: 0pt; mso-table-rspace: 0pt; border: 1px solid #e6e6e6;\">\r\n									<tr>\r\n										<td class=\"email-title\" style=\"padding: 20px 36px; line-height: 1.5; border-bottom: 1px solid #e6e6e6; background: #fff; font-size: 22px;\">【蓝鲸作业平台】定时任务提醒通知</td>\r\n									</tr>\r\n									<tr>\r\n										<td class=\"email-content\" style=\"padding: 0 36px; background: #fff;\">\r\n											<table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"font-size: 14px; mso-table-lspace: 0pt; mso-table-rspace: 0pt;\">\r\n												<tr>\r\n													<td class=\"email-source\" style=\"padding: 14px 0; color: #bebebe;\">来自 蓝鲸作业平台 的消息推送</td>\r\n												</tr>\r\n\r\n												<tr class=\"email-information\">\r\n													<td class=\"table-info\">\r\n														<table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"font-size: 14px; mso-table-lspace: 0pt; mso-table-rspace: 0pt;\">\r\n															<tr class=\"table-title\">\r\n																<td style=\"padding-top: 36px; padding-bottom: 14px; color: #707070;\">下列定时任务将在 <font style=\"font-weight:bold;color:red;\">{{ notify_time }}</font> 分钟后 <font style=\"font-weight:bold;\">开始执行</font>，请知悉：</td>\r\n															</tr>\r\n															<tr>\r\n																<td>\r\n																	<table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"font-size: 14px; mso-table-lspace: 0pt; mso-table-rspace: 0pt; border: 1px solid #e6e6e6; border-collapse: collapse;\">\r\n																		<tbody style=\"color: #707070;\">\r\n																			<tr>\r\n																				<td width=\"30%\" style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: right; font-weight: bold; background-color: #f3f3f3;\">任务名称</td>\r\n																				<td width=\"70%\" style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;\"><a href=\"{{BASE_HOST}}{{cron_uri}}\" style=\"color: #3c96ff\"> {{ cron_name }}</a></td>\r\n																			</tr>\r\n																			<tr>\r\n																				<td width=\"30%\" style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: right; font-weight: bold; background-color: #f3f3f3;\">所属业务</td>\r\n																				<td width=\"70%\" style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;\">[{{ APP_ID }}] {{ APP_NAME }}</td>\r\n																			</tr>\r\n																			<tr>\r\n																				<td width=\"30%\" style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: right; font-weight: bold; background-color: #f3f3f3;\">设置人</td>\r\n																				<td width=\"70%\" style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;\"> {{ cron_updater }}</td>\r\n																			</tr>\r\n																			<tr>\r\n																				<td width=\"30%\" style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: right; font-weight: bold; background-color: #f3f3f3;\">执行策略</td>\r\n																				<td width=\"70%\" style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;\"> {{ cron_type }}</td>\r\n																			</tr>\r\n																			<tr>\r\n																				<td width=\"30%\" style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: right; font-weight: bold; background-color: #f3f3f3;\">策略详情</td>\r\n																				<td width=\"70%\" style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;\"> {{ cron_rule }}</td>\r\n																			</tr>\r\n																		</tbody>\r\n																	</table>\r\n																</td>\r\n															</tr>\r\n														</table>\r\n													</td>\r\n												</tr>\r\n\r\n												<tr class=\"prompt-tips\">\r\n													<td style=\"padding-top: 32px; padding-bottom: 10px; color: #707070;\">如有任何疑问，请随时联系蓝鲸助手。</td>\r\n												</tr>\r\n												<tr class=\"info-remark\">\r\n													<td style=\"padding: 20px 0; text-align: right; line-height: 24px; color: #707070;\">\r\n														<div>{{ triggerTime }}</div>\r\n													</td>\r\n												</tr>\r\n											</table>\r\n										</td>\r\n									</tr>\r\n									<tr class=\"email-footer\">\r\n										<td style=\" padding: 20px 0 20px 36px; border-top: 1px solid #e6e6e6; background: #fff; color: #c7c7c7;\">你收到此邮件，是因为开启了该定时任务的执行前消息通知。</td>\r\n									</tr>\r\n								</table>\r\n							</td>\r\n						</tr>\r\n					</tbody>\r\n			   </table>\r\n			</td>\r\n		</tr>\r\n	</tbody>\r\n</table>\r\n',
        '【BlueKing JOB】Cron Pre-Launch Notification',
        '<table class=\"template-table\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"font-size: 14px; min-width: auto; mso-table-lspace: 0pt; mso-table-rspace: 0pt; background-color: #fff; background: #fff;\">\r\n	<tbody>\r\n		<tr>\r\n			<td align=\"center\" valign=\"top\" width=\"100%\" style=\"padding: 16px;\">\r\n			   <table class=\"template-table\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"956\" style=\"font-size: 14px; min-width: auto; mso-table-lspace: 0pt; mso-table-rspace: 0pt; padding-top:30px;\">\r\n					<tbody>\r\n						<tr>\r\n							<td valign=\"top\" align=\"center\" style=\"padding: 24px;\" bgcolor=\"#f9f8f6\">\r\n								<table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"font-size: 14px; mso-table-lspace: 0pt; mso-table-rspace: 0pt; border: 1px solid #e6e6e6;\">\r\n									<tr>\r\n										<td class=\"email-title\" style=\"padding: 20px 36px; line-height: 1.5; border-bottom: 1px solid #e6e6e6; background: #fff; font-size: 22px;\">【BlueKing JOB】Cron Pre-Launch Notification</td>\r\n									</tr>\r\n									<tr>\r\n										<td class=\"email-content\" style=\"padding: 0 36px; background: #fff;\">\r\n											<table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"font-size: 14px; mso-table-lspace: 0pt; mso-table-rspace: 0pt;\">\r\n												<tr>\r\n													<td class=\"email-source\" style=\"padding: 14px 0; color: #bebebe;\">This message is from <b>BlueKing JOB</b></td>\r\n												</tr>\r\n\r\n												<tr class=\"email-information\">\r\n													<td class=\"table-info\">\r\n														<table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"font-size: 14px; mso-table-lspace: 0pt; mso-table-rspace: 0pt;\">\r\n															<tr class=\"table-title\">\r\n																<td style=\"padding-top: 36px; padding-bottom: 14px; color: #707070;\">The Cron task below will <font style=\"font-weight:bold;\">launch automatically</font> after <font style=\"font-weight:bold;color:red;\">{{ notify_time }}</font> mins, please be noted!</td>\r\n															</tr>\r\n															<tr>\r\n																<td>\r\n																	<table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"font-size: 14px; mso-table-lspace: 0pt; mso-table-rspace: 0pt; border: 1px solid #e6e6e6; border-collapse: collapse;\">\r\n																		<tbody style=\"color: #707070;\">\r\n																			<tr>\r\n																				<td width=\"30%\" style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: right; font-weight: bold; background-color: #f3f3f3;\">TASK NAME</td>\r\n																				<td width=\"70%\" style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;\"><a href=\"{{BASE_HOST}}{{cron_uri}}\" style=\"color: #3c96ff\"> {{ cron_name }}</a></td>\r\n																			</tr>\r\n																			<tr>\r\n																				<td width=\"30%\" style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: right; font-weight: bold; background-color: #f3f3f3;\">BUSINESS</td>\r\n																				<td width=\"70%\" style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;\">[{{ APP_ID }}] {{ APP_NAME }}</td>\r\n																			</tr>\r\n																			<tr>\r\n																				<td width=\"30%\" style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: right; font-weight: bold; background-color: #f3f3f3;\">OPERATOR</td>\r\n																				<td width=\"70%\" style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;\"> {{ cron_updater }}</td>\r\n																			</tr>\r\n																			<tr>\r\n																				<td width=\"30%\" style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: right; font-weight: bold; background-color: #f3f3f3;\">REPEAT FREQUENCY</td>\r\n																				<td width=\"70%\" style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;\"> {{ cron_type }}</td>\r\n																			</tr>\r\n																			<tr>\r\n																				<td width=\"30%\" style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: right; font-weight: bold; background-color: #f3f3f3;\">TIME SET</td>\r\n																				<td width=\"70%\" style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;\"> {{ cron_rule }}</td>\r\n																			</tr>\r\n																		</tbody>\r\n																	</table>\r\n																</td>\r\n															</tr>\r\n														</table>\r\n													</td>\r\n												</tr>\r\n\r\n												<tr class=\"prompt-tips\">\r\n													<td style=\"padding-top: 32px; padding-bottom: 10px; color: #707070;\">If You Have Any Questions or Concerns, Please Contact BK Assistant.</td>\r\n												</tr>\r\n												<tr class=\"info-remark\">\r\n													<td style=\"padding: 20px 0; text-align: right; line-height: 24px; color: #707070;\">\r\n														<div>{{ triggerTime }}</div>\r\n													</td>\r\n												</tr>\r\n											</table>\r\n										</td>\r\n									</tr>\r\n									<tr class=\"email-footer\">\r\n										<td style=\" padding: 20px 0 20px 36px; border-top: 1px solid #e6e6e6; background: #fff; color: #c7c7c7;\">Your name is in \"Notify To\" field of this Cron settings, that\'s why you received this message.</td>\r\n									</tr>\r\n								</table>\r\n							</td>\r\n						</tr>\r\n					</tbody>\r\n			   </table>\r\n			</td>\r\n		</tr>\r\n	</tbody>\r\n</table>',
        'admin', 'admin', 0, 0);
REPLACE INTO `notify_template`(`id`, `code`, `name`, `channel`, `title`, `content`, `title_en`, `content_en`, `creator`,
                              `last_modify_user`, `create_time`, `last_modify_time`)
VALUES (5, 'beforeCronJobEnd', '定时任务结束前通知模板', 'mail', '【蓝鲸作业平台】定时任务提醒消息',
        '<table class=\"template-table\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"font-size: 14px; min-width: auto; mso-table-lspace: 0pt; mso-table-rspace: 0pt; background-color: #fff; background: #fff;\">\r\n	<tbody>\r\n		<tr>\r\n			<td align=\"center\" valign=\"top\" width=\"100%\" style=\"padding: 16px;\">\r\n			   <table class=\"template-table\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"956\" style=\"font-size: 14px; min-width: auto; mso-table-lspace: 0pt; mso-table-rspace: 0pt; padding-top:30px;\">\r\n					<tbody>\r\n						<tr>\r\n							<td valign=\"top\" align=\"center\" style=\"padding: 24px;\" bgcolor=\"#f9f8f6\">\r\n								<table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"font-size: 14px; mso-table-lspace: 0pt; mso-table-rspace: 0pt; border: 1px solid #e6e6e6;\">\r\n									<tr>\r\n										<td class=\"email-title\" style=\"padding: 20px 36px; line-height: 1.5; border-bottom: 1px solid #e6e6e6; background: #fff; font-size: 22px;\">【蓝鲸作业平台】定时任务提醒通知</td>\r\n									</tr>\r\n									<tr>\r\n										<td class=\"email-content\" style=\"padding: 0 36px; background: #fff;\">\r\n											<table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"font-size: 14px; mso-table-lspace: 0pt; mso-table-rspace: 0pt;\">\r\n												<tr>\r\n													<td class=\"email-source\" style=\"padding: 14px 0; color: #bebebe;\">来自 蓝鲸作业平台 的消息推送</td>\r\n												</tr>\r\n\r\n												<tr class=\"email-information\">\r\n													<td class=\"table-info\">\r\n														<table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"font-size: 14px; mso-table-lspace: 0pt; mso-table-rspace: 0pt;\">\r\n															<tr class=\"table-title\">\r\n																<td style=\"padding-top: 36px; padding-bottom: 14px; color: #707070;\">下列定时任务将在 <font style=\"font-weight:bold;color:red;\">{{ notify_time }}</font> 分钟后 <font style=\"font-weight:bold;\">结束并关闭</font>，请知悉：</td>\r\n															</tr>\r\n															<tr>\r\n																<td>\r\n																	<table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"font-size: 14px; mso-table-lspace: 0pt; mso-table-rspace: 0pt; border: 1px solid #e6e6e6; border-collapse: collapse;\">\r\n																		<tbody style=\"color: #707070;\">\r\n																			<tr>\r\n																				<td width=\"30%\" style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: right; font-weight: bold; background-color: #f3f3f3;\">任务名称</td>\r\n																				<td width=\"70%\" style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;\"><a href=\"{{BASE_HOST}}{{cron_uri}}\" style=\"color: #3c96ff\"> {{ cron_name }}</a></td>\r\n																			</tr>\r\n																			<tr>\r\n																				<td width=\"30%\" style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: right; font-weight: bold; background-color: #f3f3f3;\">所属业务</td>\r\n																				<td width=\"70%\" style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;\">[{{ APP_ID }}] {{ APP_NAME }}</td>\r\n																			</tr>\r\n																			<tr>\r\n																				<td width=\"30%\" style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: right; font-weight: bold; background-color: #f3f3f3;\">设置人</td>\r\n																				<td width=\"70%\" style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;\"> {{ cron_updater }}</td>\r\n																			</tr>\r\n																			<tr>\r\n																				<td width=\"30%\" style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: right; font-weight: bold; background-color: #f3f3f3;\">执行策略</td>\r\n																				<td width=\"70%\" style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;\"> {{ cron_type }}</td>\r\n																			</tr>\r\n																			<tr>\r\n																				<td width=\"30%\" style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: right; font-weight: bold; background-color: #f3f3f3;\">策略详情</td>\r\n																				<td width=\"70%\" style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;\"> {{ cron_rule }}</td>\r\n																			</tr>\r\n																		</tbody>\r\n																	</table>\r\n																</td>\r\n															</tr>\r\n														</table>\r\n													</td>\r\n												</tr>\r\n\r\n												<tr class=\"prompt-tips\">\r\n													<td style=\"padding-top: 32px; padding-bottom: 10px; color: #707070;\">如有任何疑问，请随时联系蓝鲸助手。</td>\r\n												</tr>\r\n												<tr class=\"info-remark\">\r\n													<td style=\"padding: 20px 0; text-align: right; line-height: 24px; color: #707070;\">\r\n														<div>{{ triggerTime }}</div>\r\n													</td>\r\n												</tr>\r\n											</table>\r\n										</td>\r\n									</tr>\r\n									<tr class=\"email-footer\">\r\n										<td style=\" padding: 20px 0 20px 36px; border-top: 1px solid #e6e6e6; background: #fff; color: #c7c7c7;\">你收到此邮件，是因为开启了该定时任务的结束前消息通知。</td>\r\n									</tr>\r\n								</table>\r\n							</td>\r\n						</tr>\r\n					</tbody>\r\n			   </table>\r\n			</td>\r\n		</tr>\r\n	</tbody>\r\n</table>\r\n',
        '【BlueKing JOB】Cron End-time Notification',
        '<table class=\"template-table\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"font-size: 14px; min-width: auto; mso-table-lspace: 0pt; mso-table-rspace: 0pt; background-color: #fff; background: #fff;\">\r\n	<tbody>\r\n		<tr>\r\n			<td align=\"center\" valign=\"top\" width=\"100%\" style=\"padding: 16px;\">\r\n			   <table class=\"template-table\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"956\" style=\"font-size: 14px; min-width: auto; mso-table-lspace: 0pt; mso-table-rspace: 0pt; padding-top:30px;\">\r\n					<tbody>\r\n						<tr>\r\n							<td valign=\"top\" align=\"center\" style=\"padding: 24px;\" bgcolor=\"#f9f8f6\">\r\n								<table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"font-size: 14px; mso-table-lspace: 0pt; mso-table-rspace: 0pt; border: 1px solid #e6e6e6;\">\r\n									<tr>\r\n											<td class=\"email-title\" style=\"padding: 20px 36px; line-height: 1.5; border-bottom: 1px solid #e6e6e6; background: #fff; font-size: 22px;\">【BlueKing JOB】Cron End-time Notification</td>\r\n									</tr>\r\n									<tr>\r\n										<td class=\"email-content\" style=\"padding: 0 36px; background: #fff;\">\r\n											<table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"font-size: 14px; mso-table-lspace: 0pt; mso-table-rspace: 0pt;\">\r\n												<tr>\r\n													<td class=\"email-source\" style=\"padding: 14px 0; color: #bebebe;\">This message is from <b>BlueKing JOB</b></td>\r\n												</tr>\r\n\r\n												<tr class=\"email-information\">\r\n													<td class=\"table-info\">\r\n														<table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"font-size: 14px; mso-table-lspace: 0pt; mso-table-rspace: 0pt;\">\r\n															<tr class=\"table-title\">\r\n																<td style=\"padding-top: 36px; padding-bottom: 14px; color: #707070;\">The Cron task below will <font style=\"font-weight:bold;\">Turn-Off</font> automatically after <font style=\"font-weight:bold;color:red;\">{{ notify_time }}</font> mins, please be noted!</td>\r\n															</tr>\r\n															<tr>\r\n																<td>\r\n																	<table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"font-size: 14px; mso-table-lspace: 0pt; mso-table-rspace: 0pt; border: 1px solid #e6e6e6; border-collapse: collapse;\">\r\n																		<tbody style=\"color: #707070;\">\r\n																			<tr>\r\n																				<td width=\"30%\" style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: right; font-weight: bold; background-color: #f3f3f3;\">TASK NAME</td>\r\n																				<td width=\"70%\" style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;\"><a href=\"{{BASE_HOST}}{{cron_uri}}\" style=\"color: #3c96ff\"> {{ cron_name }}</a></td>\r\n																			</tr>\r\n																			<tr>\r\n																				<td width=\"30%\" style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: right; font-weight: bold; background-color: #f3f3f3;\">BUSINESS</td>\r\n																				<td width=\"70%\" style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;\">[{{ APP_ID }}] {{ APP_NAME }}</td>\r\n																			</tr>\r\n																			<tr>\r\n																				<td width=\"30%\" style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: right; font-weight: bold; background-color: #f3f3f3;\">OPERATOR</td>\r\n																				<td width=\"70%\" style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;\"> {{ cron_updater }}</td>\r\n																			</tr>\r\n																			<tr>\r\n																				<td width=\"30%\" style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: right; font-weight: bold; background-color: #f3f3f3;\">REPEAT FREQUENCY</td>\r\n																				<td width=\"70%\" style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;\"> {{ cron_type }}</td>\r\n																			</tr>\r\n																			<tr>\r\n																				<td width=\"30%\" style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: right; font-weight: bold; background-color: #f3f3f3;\">TIME SET</td>\r\n																				<td width=\"70%\" style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;\"> {{ cron_rule }}</td>\r\n																			</tr>\r\n																		</tbody>\r\n																	</table>\r\n																</td>\r\n															</tr>\r\n														</table>\r\n													</td>\r\n												</tr>\r\n\r\n												<tr class=\"prompt-tips\">\r\n													<td style=\"padding-top: 32px; padding-bottom: 10px; color: #707070;\">If You Have Any Questions or Concerns, Please Contact BK Assistant.</td>\r\n												</tr>\r\n												<tr class=\"info-remark\">\r\n													<td style=\"padding: 20px 0; text-align: right; line-height: 24px; color: #707070;\">\r\n														<div>{{ triggerTime }}</div>\r\n													</td>\r\n												</tr>\r\n											</table>\r\n										</td>\r\n									</tr>\r\n									<tr class=\"email-footer\">\r\n										<td style=\" padding: 20px 0 20px 36px; border-top: 1px solid #e6e6e6; background: #fff; color: #c7c7c7;\">Your name is in \"Notify To\" field of this Cron settings, that\'s why you received this message.</td>\r\n									</tr>\r\n								</table>\r\n							</td>\r\n						</tr>\r\n					</tbody>\r\n			   </table>\r\n			</td>\r\n		</tr>\r\n	</tbody>\r\n</table>\r\n',
        'admin', 'admin', 0, 0);
REPLACE INTO `notify_template`(`id`, `code`, `name`, `channel`, `title`, `content`, `title_en`, `content_en`, `creator`,
                              `last_modify_user`, `create_time`, `last_modify_time`)
VALUES (2, 'beforeCronJobExecute', '定时任务执行前通知模板', 'rtx', '【蓝鲸作业平台】定时任务执行前提醒',
        ' 下列定时任务将在{{ notify_time }}分钟后执行\r\n任务名称：{{ cron_name }}\r\n所属业务：{{ APP_NAME }}\r\n　设置人：{{ cron_updater }}\r\n执行策略：{{ cron_type }}\r\n策略详情：{{ cron_rule }}\r\n查看详情：{{BASE_HOST}}{{cron_uri}}',
        '【BlueKing JOB】Cron Pre-Launch Notification',
        'The task will launch automatically after {{ notify_time }} mins, please be noted!\r\n    Task Name: {{ cron_name }}\r\n        Business: {{ APP_NAME }}\r\n       Operator: {{ cron_updater }}\r\nREPEAT FREQ: {{ cron_type }}\r\n       Time Set: {{ cron_rule }}\r\n          Details: {{BASE_HOST}}{{cron_uri}}',
        'admin', 'admin', 0, 0);
REPLACE INTO `notify_template`(`id`, `code`, `name`, `channel`, `title`, `content`, `title_en`, `content_en`, `creator`,
                              `last_modify_user`, `create_time`, `last_modify_time`)
VALUES (6, 'beforeCronJobEnd', '定时任务结束前通知模板', 'rtx', '【蓝鲸作业平台】定时任务结束前提醒',
        '下列定时任务将在{{ notify_time }}分钟后结束并关闭\r\n任务名称：{{ cron_name }}\r\n所属业务：{{ APP_NAME }}\r\n　设置人：{{ cron_updater }}\r\n执行策略：{{ cron_type }}\r\n策略详情：{{ cron_rule }}\r\n查看详情：{{BASE_HOST}}{{cron_uri}}',
        '【BlueKing JOB】Cron End-time Notification',
        'The task will Turn-Off automatically after {{ notify_time }} mins, please be noted!\r\n    Task Name: {{ cron_name }}\r\n        Business: {{ APP_NAME }}\r\n       Operator: {{ cron_updater }}\r\nREPEAT FREQ: {{ cron_type }}\r\n       Time Set: {{ cron_rule }}\r\n          Details: {{BASE_HOST}}{{cron_uri}}',
        'admin', 'admin', 0, 0);
REPLACE INTO `notify_template`(`id`, `code`, `name`, `channel`, `title`, `content`, `title_en`, `content_en`, `creator`,
                              `last_modify_user`, `create_time`, `last_modify_time`)
VALUES (3, 'beforeCronJobExecute', '定时任务执行前通知模板', 'sms',
        '定时任务执行前提醒：您的业务[{{ APP_NAME }}]有一个定时任务[{{ cron_name }}]将在{{ notify_time }}分钟后执行，请知悉。【蓝鲸作业平台】',
        '定时任务执行前提醒：您的业务[{{ APP_NAME }}]有一个定时任务[{{ cron_name }}]将在{{ notify_time }}分钟后执行，请知悉。【蓝鲸作业平台】',
        'Cron Pre-Launch Notification: There\'s a task of [{{ APP_NAME }}] will launch automatically after {{ notify_time }} mins, more details: {{BASE_HOST}}{{cron_uri}}【BlueKing JOB】',
        'Cron Pre-Launch Notification: There\'s a task of [{{ APP_NAME }}] will launch automatically after {{ notify_time }} mins, more details: {{BASE_HOST}}{{cron_uri}}【BlueKing JOB】',
        'admin', 'admin', 0, 0);
REPLACE INTO `notify_template`(`id`, `code`, `name`, `channel`, `title`, `content`, `title_en`, `content_en`, `creator`,
                              `last_modify_user`, `create_time`, `last_modify_time`)
VALUES (7, 'beforeCronJobEnd', '定时任务结束前通知模板', 'sms',
        '定时任务结束前提醒：您的业务[{{ APP_NAME }}]有一个定时任务[{{ cron_name }}]将在{{ notify_time }}分钟后结束并关闭，请知悉。【蓝鲸作业平台】',
        '定时任务结束前提醒：您的业务[{{ APP_NAME }}]有一个定时任务[{{ cron_name }}]将在{{ notify_time }}分钟后结束并关闭，请知悉。【蓝鲸作业平台】',
        'Cron End-time Notification: There\'s a task of [{{ APP_NAME }}] will Turn-Off automatically after {{ notify_time }} mins, more details: {{BASE_HOST}}{{cron_uri}}【BlueKing JOB】',
        'Cron End-time Notification: There\'s a task of [{{ APP_NAME }}] will Turn-Off automatically after {{ notify_time }} mins, more details: {{BASE_HOST}}{{cron_uri}}【BlueKing JOB】',
        'admin', 'admin', 0, 0);
REPLACE INTO `notify_template`(`id`, `code`, `name`, `channel`, `title`, `content`, `title_en`, `content_en`, `creator`,
                              `last_modify_user`, `create_time`, `last_modify_time`)
VALUES (4, 'beforeCronJobExecute', '定时任务执行前通知模板', 'weixin', '【蓝鲸作业平台】定时任务执行前提醒',
        ' 下列定时任务将在{{ notify_time }}分钟后执行\r\n任务名称：{{ cron_name }}\r\n所属业务：{{ APP_NAME }}\r\n　设置人：{{ cron_updater }}\r\n执行策略：{{ cron_type }}\r\n策略详情：{{ cron_rule }}\r\n查看详情：{{BASE_HOST}}{{cron_uri}}',
        '【BlueKing JOB】Cron Pre-Launch Notification',
        'The task will launch automatically after {{ notify_time }} mins, please be noted!\r\n    Task Name: {{ cron_name }}\r\n        Business: {{ APP_NAME }}\r\n       Operator: {{ cron_updater }}\r\nREPEAT FREQ: {{ cron_type }}\r\n       Time Set: {{ cron_rule }}\r\n          Details: {{BASE_HOST}}{{cron_uri}}',
        'admin', 'admin', 0, 0);
REPLACE INTO `notify_template`(`id`, `code`, `name`, `channel`, `title`, `content`, `title_en`, `content_en`, `creator`,
                              `last_modify_user`, `create_time`, `last_modify_time`)
VALUES (8, 'beforeCronJobEnd', '定时任务结束前通知模板', 'weixin', '【蓝鲸作业平台】定时任务结束前提醒',
        '下列定时任务将在{{ notify_time }}分钟后结束并关闭\r\n任务名称：{{ cron_name }}\r\n所属业务：{{ APP_NAME }}\r\n　设置人：{{ cron_updater }}\r\n执行策略：{{ cron_type }}\r\n策略详情：{{ cron_rule }}\r\n查看详情：{{BASE_HOST}}{{cron_uri}}',
        '【BlueKing JOB】Cron End-time Notification',
        'The task will Turn-Off automatically after {{ notify_time }} mins, please be noted!\r\n    Task Name: {{ cron_name }}\r\n        Business: {{ APP_NAME }}\r\n       Operator: {{ cron_updater }}\r\nREPEAT FREQ: {{ cron_type }}\r\n       Time Set: {{ cron_rule }}\r\n          Details: {{BASE_HOST}}{{cron_uri}}',
        'admin', 'admin', 0, 0);
