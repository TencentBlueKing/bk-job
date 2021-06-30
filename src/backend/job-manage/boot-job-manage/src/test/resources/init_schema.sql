/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 * --------------------------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

CREATE SCHEMA IF NOT EXISTS job_manage;
USE job_manage;
SET NAMES UTF8MB4;

-- ----------------------------
-- Table structure for script
-- ----------------------------
CREATE TABLE `script`
(
    `id`               VARCHAR(32)         NOT NULL,
    `row_create_time`  DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `row_update_time`  DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `name`             VARCHAR(512)        NOT NULL,
    `app_id`           BIGINT(20) UNSIGNED NOT NULL,
    `type`             TINYINT(4) UNSIGNED NOT NULL DEFAULT '1',
    `is_public`        TINYINT(1) UNSIGNED          DEFAULT '0',
    `creator`          VARCHAR(128)        NOT NULL,
    `create_time`      BIGINT(20) UNSIGNED NOT NULL DEFAULT '0',
    `last_modify_user` VARCHAR(128)                 DEFAULT NULL,
    `last_modify_time` BIGINT(20) UNSIGNED NOT NULL DEFAULT '0',
    `category`         TINYINT(1) UNSIGNED          DEFAULT '1',
    `description`      LONGTEXT,
    `is_deleted`       TINYINT(1) UNSIGNED          DEFAULT '0',
    `tags`             VARCHAR(512)                 DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY (`app_id`),
    KEY (`app_id`, `name`)
) ENGINE = INNODB
  DEFAULT CHARSET = UTF8MB4;

-- ----------------------------
-- Table structure for script_version
-- ----------------------------
CREATE TABLE `script_version`
(
    `id`               BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    `row_create_time`  DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `row_update_time`  DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `script_id`        VARCHAR(32)                  DEFAULT NULL,
    `content`          LONGTEXT            NOT NULL,
    `creator`          VARCHAR(128)        NOT NULL,
    `create_time`      BIGINT(20) UNSIGNED NOT NULL DEFAULT '0',
    `last_modify_user` VARCHAR(128)                 DEFAULT NULL,
    `last_modify_time` BIGINT(20) UNSIGNED NOT NULL DEFAULT '0',
    `version`          VARCHAR(100)                 DEFAULT NULL,
    `is_deleted`       TINYINT(1) UNSIGNED          DEFAULT '0',
    `status`           TINYINT(1) UNSIGNED          DEFAULT '0',
    `version_desc`     LONGTEXT,
    PRIMARY KEY (`id`),
    KEY (`script_id`)
) ENGINE = INNODB
  DEFAULT CHARSET = UTF8MB4;

-- ----------------------------
-- Table structure for tag
-- ----------------------------
CREATE TABLE `tag`
(
    `id`               BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    `row_create_time`  DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `row_update_time`  DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `app_id`           BIGINT(20) UNSIGNED NOT NULL,
    `name`             VARCHAR(512)        NOT NULL,
    `creator`          VARCHAR(128)        NOT NULL,
    `last_modify_user` VARCHAR(128)        NOT NULL,
    `is_deleted`       TINYINT(1) UNSIGNED NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    UNIQUE KEY (`app_id`, `name`)
) ENGINE = INNODB
  DEFAULT CHARSET = UTF8MB4;

-- ----------------------------
-- Table structure for task_favorite_plan
-- ----------------------------
CREATE TABLE `task_favorite_plan`
(
    `id`              BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    `row_create_time` DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `row_update_time` DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `app_id`          BIGINT(20) UNSIGNED NOT NULL,
    `username`        VARCHAR(128)        NOT NULL,
    `plan_id`         BIGINT(20) UNSIGNED NOT NULL,
    `is_deleted`      TINYINT(1) UNSIGNED NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    UNIQUE KEY (`app_id`, `username`, `plan_id`) USING BTREE,
    KEY (`username`) USING BTREE
) ENGINE = INNODB
  DEFAULT CHARSET = UTF8MB4;

-- ----------------------------
-- Table structure for task_favorite_template
-- ----------------------------
CREATE TABLE `task_favorite_template`
(
    `id`              BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    `row_create_time` DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `row_update_time` DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `app_id`          BIGINT(20) UNSIGNED NOT NULL,
    `username`        VARCHAR(128)        NOT NULL,
    `template_id`     BIGINT(20) UNSIGNED NOT NULL,
    `is_deleted`      TINYINT(1) UNSIGNED NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    UNIQUE KEY (`app_id`, `username`, `template_id`) USING BTREE,
    KEY (`username`) USING BTREE
) ENGINE = INNODB
  DEFAULT CHARSET = UTF8MB4;

-- ----------------------------
-- Table structure for task_plan
-- ----------------------------
CREATE TABLE `task_plan`
(
    `id`                BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    `row_create_time`   DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `row_update_time`   DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `app_id`            BIGINT(20) UNSIGNED NOT NULL,
    `template_id`       CHAR(40)            NOT NULL,
    `name`              VARCHAR(512)        NOT NULL,
    `creator`           VARCHAR(128)        NOT NULL,
    `is_deleted`        TINYINT(1) UNSIGNED NOT NULL DEFAULT '0',
    `create_time`       BIGINT(20) UNSIGNED NOT NULL,
    `last_modify_user`  VARCHAR(128)        NOT NULL,
    `last_modify_time`  BIGINT(20) UNSIGNED NOT NULL DEFAULT '0',
    `first_step_id`     BIGINT(20) UNSIGNED          DEFAULT NULL,
    `last_step_id`      BIGINT(20) UNSIGNED          DEFAULT NULL,
    `version`           CHAR(64)            NOT NULL,
    `is_latest_version` TINYINT(1) UNSIGNED NOT NULL DEFAULT '1',
    PRIMARY KEY (`id`),
    KEY (`template_id`) USING BTREE,
    KEY (`app_id`) USING BTREE,
    KEY (`creator`) USING BTREE
) ENGINE = INNODB
  DEFAULT CHARSET = UTF8MB4;

-- ----------------------------
-- Table structure for task_plan_step
-- ----------------------------
CREATE TABLE `task_plan_step`
(
    `id`               BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    `row_create_time`  DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `row_update_time`  DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `plan_id`          BIGINT(20) UNSIGNED NOT NULL,
    `type`             TINYINT(2) UNSIGNED NOT NULL,
    `name`             VARCHAR(512)        NOT NULL,
    `previous_step_id` BIGINT(20) UNSIGNED NOT NULL,
    `next_step_id`     BIGINT(20) UNSIGNED NOT NULL,
    `is_deleted`       TINYINT(1) UNSIGNED NOT NULL DEFAULT '0',
    `script_step_id`   BIGINT(20) UNSIGNED          DEFAULT NULL,
    `file_step_id`     BIGINT(20) UNSIGNED          DEFAULT NULL,
    `approval_step_id` BIGINT(20) UNSIGNED          DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY (`plan_id`) USING BTREE
) ENGINE = INNODB
  DEFAULT CHARSET = UTF8MB4;

-- ----------------------------
-- Table structure for task_plan_step_approval
-- ----------------------------
CREATE TABLE `task_plan_step_approval`
(
    `id`               BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    `row_create_time`  DATETIME                     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    `row_update_time`  DATETIME                     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    `step_id`          BIGINT(20) UNSIGNED NOT NULL,
    `approval_type`    TINYINT(2) UNSIGNED NOT NULL DEFAULT '0',
    `approval_user`    VARCHAR(255)        NOT NULL,
    `approval_message` VARCHAR(2048)       NOT NULL,
    `notify_channel`   VARCHAR(1024)       NOT NULL,
    PRIMARY KEY (`id`),
    KEY (`step_id`) USING BTREE
) ENGINE = INNODB
  DEFAULT CHARSET = UTF8MB4;

-- ----------------------------
-- Table structure for task_plan_step_file
-- ----------------------------
CREATE TABLE `task_plan_step_file`
(
    `id`                        BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    `row_create_time`           DATETIME                     DEFAULT CURRENT_TIMESTAMP,
    `row_update_time`           DATETIME                     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `step_id`                   BIGINT(20) UNSIGNED NOT NULL,
    `destination_file_location` VARCHAR(512)        NOT NULL,
    `execute_account`           BIGINT(20)          NOT NULL,
    `destination_host_list`     LONGTEXT                     DEFAULT NULL,
    `timeout`                   BIGINT(20) UNSIGNED NOT NULL DEFAULT '0',
    `origin_speed_limit`        BIGINT(20) UNSIGNED NULL     DEFAULT NULL,
    `target_speed_limit`        BIGINT(20) UNSIGNED NULL     DEFAULT NULL,
    `ignore_error`              TINYINT(1) UNSIGNED NOT NULL,
    `duplicate_handler`         TINYINT(2) UNSIGNED NOT NULL DEFAULT '1',
    PRIMARY KEY (`id`),
    KEY (`step_id`) USING BTREE
) ENGINE = INNODB
  DEFAULT CHARSET = UTF8MB4;

-- ----------------------------
-- Table structure for task_plan_step_file_list
-- ----------------------------
CREATE TABLE `task_plan_step_file_list`
(
    `id`              BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    `row_create_time` DATETIME                     DEFAULT CURRENT_TIMESTAMP,
    `row_update_time` DATETIME                     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `step_id`         BIGINT(20) UNSIGNED NOT NULL,
    `file_step_id`    BIGINT(20) UNSIGNED NOT NULL,
    `file_type`       TINYINT(2)          NOT NULL DEFAULT '0',
    `file_location`   VARCHAR(512)        NOT NULL,
    `file_size`       BIGINT(20) UNSIGNED          DEFAULT NULL,
    `file_hash`       CHAR(64)                     DEFAULT NULL,
    `host`            LONGTEXT                     DEFAULT NULL,
    `host_account`    BIGINT(20)                   DEFAULT NULL,
    `file_source_id`  INT(11)                      DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY (`step_id`) USING BTREE
) ENGINE = INNODB
  DEFAULT CHARSET = UTF8MB4;

-- ----------------------------
-- Table structure for task_plan_step_script
-- ----------------------------
CREATE TABLE `task_plan_step_script`
(
    `id`                    BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    `row_create_time`       DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `row_update_time`       DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `plan_id`               BIGINT(20) UNSIGNED NOT NULL,
    `step_id`               BIGINT(20) UNSIGNED NOT NULL,
    `script_type`           TINYINT(2) UNSIGNED NOT NULL DEFAULT '0',
    `script_id`             CHAR(32)            NOT NULL,
    `script_version_id`     BIGINT(20) UNSIGNED NOT NULL,
    `content`               LONGTEXT            NULL,
    `language`              TINYINT(5) UNSIGNED NOT NULL,
    `script_param`          VARCHAR(512)        NULL,
    `script_timeout`        BIGINT(20) UNSIGNED NOT NULL,
    `execute_account`       BIGINT(20)          NOT NULL,
    `destination_host_list` LONGTEXT            NULL,
    `is_secure_param`       TINYINT(1) UNSIGNED NOT NULL DEFAULT '0',
    `is_latest_version`     TINYINT(1) UNSIGNED NOT NULL DEFAULT '1',
    `ignore_error`          TINYINT(1) UNSIGNED NOT NULL,
    PRIMARY KEY (`id`),
    KEY (`step_id`) USING BTREE,
    KEY (`script_id`) USING BTREE,
    KEY (`plan_id`) USING BTREE
) ENGINE = INNODB
  DEFAULT CHARSET = UTF8MB4;

-- ----------------------------
-- Table structure for task_plan_variable
-- ----------------------------
CREATE TABLE `task_plan_variable`
(
    `id`                   BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    `row_create_time`      DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `row_update_time`      DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `plan_id`              BIGINT(20) UNSIGNED NOT NULL,
    `name`                 VARCHAR(255)        NOT NULL,
    `template_variable_id` BIGINT(20) UNSIGNED NOT NULL,
    `type`                 TINYINT(2) UNSIGNED NOT NULL,
    `default_value`        LONGTEXT                     DEFAULT NULL,
    `description`          VARCHAR(512)        NOT NULL,
    `is_changeable`        TINYINT(1) UNSIGNED NOT NULL,
    `is_required`          TINYINT(1) UNSIGNED NOT NULL,
    PRIMARY KEY (`id`),
    KEY (`plan_id`) USING BTREE
) ENGINE = INNODB
  DEFAULT CHARSET = UTF8MB4;

-- ----------------------------
-- Table structure for task_template
-- ----------------------------
CREATE TABLE `task_template`
(
    `id`                BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    `row_create_time`   DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `row_update_time`   DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `app_id`            BIGINT(20) UNSIGNED NOT NULL,
    `name`              VARCHAR(512)        NOT NULL,
    `description`       VARCHAR(2048)                DEFAULT NULL,
    `creator`           VARCHAR(128)        NOT NULL,
    `status`            TINYINT(2) UNSIGNED NOT NULL DEFAULT '0',
    `is_deleted`        TINYINT(1) UNSIGNED NOT NULL DEFAULT '0',
    `create_time`       BIGINT(20) UNSIGNED NOT NULL,
    `last_modify_user`  VARCHAR(128)        NOT NULL,
    `last_modify_time`  BIGINT(20) UNSIGNED NOT NULL DEFAULT '0',
    `tags`              VARCHAR(512)                 DEFAULT NULL,
    `first_step_id`     BIGINT(20) UNSIGNED          DEFAULT NULL,
    `last_step_id`      BIGINT(20) UNSIGNED          DEFAULT NULL,
    `version`           CHAR(64)            NOT NULL,
    `is_latest_version` TINYINT(1) UNSIGNED NOT NULL DEFAULT '1',
    PRIMARY KEY (`id`),
    KEY (`name`) USING BTREE,
    KEY (`creator`) USING BTREE,
    KEY (`app_id`) USING BTREE
) ENGINE = INNODB
  DEFAULT CHARSET = UTF8MB4;

-- ----------------------------
-- Table structure for task_template_step
-- ----------------------------
CREATE TABLE `task_template_step`
(
    `id`               BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    `row_create_time`  DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `row_update_time`  DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `template_id`      BIGINT(20) UNSIGNED NOT NULL,
    `type`             TINYINT(2) UNSIGNED NOT NULL DEFAULT '0',
    `name`             VARCHAR(512)        NOT NULL,
    `previous_step_id` BIGINT(20) UNSIGNED NOT NULL DEFAULT '0',
    `next_step_id`     BIGINT(20) UNSIGNED NOT NULL DEFAULT '0',
    `is_deleted`       TINYINT(1) UNSIGNED NOT NULL DEFAULT '0',
    `script_step_id`   BIGINT(20) UNSIGNED          DEFAULT NULL,
    `file_step_id`     BIGINT(20) UNSIGNED          DEFAULT NULL,
    `approval_step_id` BIGINT(20) UNSIGNED          DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY (`template_id`) USING BTREE
) ENGINE = INNODB
  DEFAULT CHARSET = UTF8MB4;

-- ----------------------------
-- Table structure for task_template_step_approval
-- ----------------------------
CREATE TABLE `task_template_step_approval`
(
    `id`               BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    `row_create_time`  DATETIME                     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    `row_update_time`  DATETIME                     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    `step_id`          BIGINT(20) UNSIGNED NOT NULL,
    `approval_type`    TINYINT(2) UNSIGNED NOT NULL DEFAULT '0',
    `approval_user`    VARCHAR(255)        NOT NULL,
    `approval_message` VARCHAR(2048)       NOT NULL,
    `notify_channel`   VARCHAR(1024)       NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY (`step_id`) USING BTREE
) ENGINE = INNODB
  DEFAULT CHARSET = UTF8MB4;

-- ----------------------------
-- Table structure for task_template_step_file
-- ----------------------------
CREATE TABLE `task_template_step_file`
(
    `id`                        BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    `row_create_time`           DATETIME                     DEFAULT CURRENT_TIMESTAMP,
    `row_update_time`           DATETIME                     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `step_id`                   BIGINT(20) UNSIGNED NOT NULL,
    `destination_file_location` VARCHAR(512)        NOT NULL,
    `execute_account`           VARCHAR(255)        NOT NULL,
    `destination_host_list`     LONGTEXT                     DEFAULT NULL,
    `timeout`                   BIGINT(20) UNSIGNED NOT NULL DEFAULT '0',
    `origin_speed_limit`        BIGINT(20) UNSIGNED NULL     DEFAULT NULL,
    `target_speed_limit`        BIGINT(20) UNSIGNED NULL     DEFAULT NULL,
    `ignore_error`              TINYINT(1) UNSIGNED NOT NULL,
    `duplicate_handler`         TINYINT(2) UNSIGNED NOT NULL DEFAULT '1',
    `not_exist_path_handler`    TINYINT(2) UNSIGNED NOT NULL DEFAULT '1',
    PRIMARY KEY (`id`),
    UNIQUE KEY (`step_id`) USING BTREE
) ENGINE = INNODB
  DEFAULT CHARSET = UTF8MB4;

-- ----------------------------
-- Table structure for task_template_step_file_list
-- ----------------------------
CREATE TABLE `task_template_step_file_list`
(
    `id`              BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    `row_create_time` DATETIME                     DEFAULT CURRENT_TIMESTAMP,
    `row_update_time` DATETIME                     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `step_id`         BIGINT(20) UNSIGNED NOT NULL,
    `file_type`       TINYINT(2)          NOT NULL DEFAULT '0',
    `file_size`       BIGINT(20) UNSIGNED          DEFAULT NULL,
    `file_location`   VARCHAR(512)        NOT NULL,
    `file_hash`       CHAR(64)                     DEFAULT NULL,
    `host`            LONGTEXT                     DEFAULT NULL,
    `host_account`    VARCHAR(512)                 DEFAULT NULL,
    `file_source_id`  INT(11)                      DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY (`step_id`) USING BTREE
) ENGINE = INNODB
  DEFAULT CHARSET = UTF8MB4;

-- ----------------------------
-- Table structure for task_template_step_script
-- ----------------------------
CREATE TABLE `task_template_step_script`
(
    `id`                    BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    `row_create_time`       DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `row_update_time`       DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `template_id`           BIGINT(20) UNSIGNED NOT NULL,
    `step_id`               BIGINT(20) UNSIGNED NOT NULL,
    `script_type`           TINYINT(2) UNSIGNED NOT NULL DEFAULT '0',
    `script_id`             CHAR(32)            NOT NULL,
    `script_version_id`     BIGINT(20) UNSIGNED NOT NULL,
    `content`               LONGTEXT            NULL,
    `language`              TINYINT(5) UNSIGNED NOT NULL,
    `script_param`          VARCHAR(512)        NULL,
    `script_timeout`        BIGINT(20) UNSIGNED NOT NULL,
    `execute_account`       BIGINT(20)          NOT NULL,
    `destination_host_list` LONGTEXT            NULL,
    `is_secure_param`       TINYINT(1) UNSIGNED NOT NULL DEFAULT '0',
    `is_latest_version`     TINYINT(1) UNSIGNED NOT NULL DEFAULT '1',
    `ignore_error`          TINYINT(1) UNSIGNED NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY (`step_id`) USING BTREE,
    KEY (`script_id`) USING BTREE,
    KEY (`template_id`) USING BTREE
) ENGINE = INNODB
  DEFAULT CHARSET = UTF8MB4;

-- ----------------------------
-- Table structure for task_template_variable
-- ----------------------------
CREATE TABLE `task_template_variable`
(
    `id`              BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    `row_create_time` DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `row_update_time` DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `template_id`     BIGINT(20) UNSIGNED NOT NULL,
    `name`            VARCHAR(255)        NOT NULL,
    `type`            TINYINT(2) UNSIGNED NOT NULL,
    `default_value`   LONGTEXT                     DEFAULT NULL,
    `description`     VARCHAR(512)        NOT NULL,
    `is_changeable`   TINYINT(1) UNSIGNED NOT NULL,
    `is_required`     TINYINT(1) UNSIGNED NOT NULL,
    PRIMARY KEY (`id`),
    KEY (`template_id`) USING BTREE
) ENGINE = INNODB
  DEFAULT CHARSET = UTF8MB4;

CREATE TABLE `host`
(
    `host_id`         BIGINT(20) UNSIGNED NOT NULL,
    `row_create_time` DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `row_update_time` DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `app_id`          BIGINT(20) UNSIGNED NOT NULL,
    `ip`              VARCHAR(15)         NOT NULL,
    `ip_desc`         VARCHAR(2000)                DEFAULT NULL,
    `set_ids`         varchar(2048)                DEFAULT NULL,
    `module_ids`      varchar(2048)                DEFAULT NULL,
    `cloud_area_id`   BIGINT(20) UNSIGNED NOT NULL,
    `display_ip`      VARCHAR(1024)       NOT NULL,
    `os`              VARCHAR(512)                 DEFAULT '',
    `os_type`         VARCHAR(32)                  DEFAULT NULL,
    `module_type`     VARCHAR(64)                  DEFAULT '1',
    `is_agent_alive`  TINYINT(4) UNSIGNED          DEFAULT '1',
    PRIMARY KEY (`host_id`),
    KEY `idx_app_ip_cloud_area_ip` (`app_id`, `ip`, `cloud_area_id`) USING BTREE,
    KEY `idx_ip_cloud_area_id` (`ip`, `cloud_area_id`) USING BTREE
) ENGINE = INNODB
  DEFAULT CHARSET = UTF8MB4;

CREATE TABLE `application`
(
    `app_id`              BIGINT(20) UNSIGNED NOT NULL,
    `row_create_time`     DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `row_update_time`     DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `app_name`            VARCHAR(128)                 DEFAULT NULL,
    `maintainers`         VARCHAR(8192)                DEFAULT NULL,
    `bk_supplier_account` VARCHAR(128)        NOT NULL DEFAULT '0',
    `app_type`            TINYINT(4)          NOT NULL DEFAULT '1',
    `sub_app_ids`         TEXT                         DEFAULT NULL,
    `timezone`            VARCHAR(128)                 DEFAULT 'Asia/Shanghai',
    `bk_operate_dept_id`  BIGINT(20)          NULL,
    `language`            VARCHAR(20)                  DEFAULT NULL,
    PRIMARY KEY (`app_id`),
    KEY (`app_type`)
) ENGINE = INNODB
  DEFAULT CHARSET = UTF8MB4;

CREATE TABLE `account`
(
    `id`                   BIGINT(20)          NOT NULL AUTO_INCREMENT,
    `row_create_time`      DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `row_update_time`      DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `account`              VARCHAR(255)        NOT NULL,
    `alias`                VARCHAR(255)                 DEFAULT NULL,
    `category`             TINYINT(4)          NOT NULL,
    `type`                 TINYINT(4)          NOT NULL,
    `app_id`               BIGINT(20)          NOT NULL,
    `grantee`              LONGTEXT                     DEFAULT NULL,
    `remark`               VARCHAR(1024)                DEFAULT NULL,
    `os`                   VARCHAR(32)                  DEFAULT 'Linux',
    `password`             VARCHAR(255)                 DEFAULT NULL,
    `db_password`          VARCHAR(255)                 DEFAULT NULL,
    `db_port`              INT(5)                       DEFAULT NULL,
    `db_system_account_id` BIGINT(20)                   DEFAULT NULL,
    `creator`              VARCHAR(128)        NOT NULL,
    `create_time`          BIGINT(20) UNSIGNED NOT NULL DEFAULT '0',
    `last_modify_user`     VARCHAR(128)                 DEFAULT NULL,
    `last_modify_time`     BIGINT(20) UNSIGNED NOT NULL DEFAULT '0',
    `is_deleted`           TINYINT(1) UNSIGNED NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    UNIQUE KEY (`app_id`, `category`, `alias`),
    KEY (`app_id`, `account`)
) ENGINE = INNODB
  DEFAULT CHARSET = UTF8MB4;

DROP TABLE IF EXISTS `analysis_task_instance`;
CREATE TABLE `analysis_task_instance`
(
    `id`               bigint(20)          NOT NULL AUTO_INCREMENT,
    `app_id`           bigint(20)          NOT NULL COMMENT '业务id',
    `task_id`          bigint(255)         NOT NULL COMMENT '任务id',
    `status`           int(10)             NOT NULL COMMENT '任务状态',
    `result_data`      text                NOT NULL COMMENT '任务结果数据，各任务自定义格式',
    `priority`         int(10)             NOT NULL COMMENT '优先级',
    `active`           bit(1)              NOT NULL COMMENT '是否启用',
    `creator`          varchar(128)        NOT NULL COMMENT '创建者',
    `last_modify_user` varchar(128)        NULL DEFAULT NULL COMMENT '更新者',
    `create_time`      bigint(20) UNSIGNED NULL DEFAULT NULL COMMENT '创建时间',
    `last_modify_time` bigint(20) UNSIGNED NULL DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 961;

DROP TABLE IF EXISTS `analysis_task`;
CREATE TABLE `analysis_task`
(
    `id`                             bigint(20)          NOT NULL AUTO_INCREMENT,
    `code`                           varchar(255)        NOT NULL COMMENT '任务代码，用于匹配处理器',
    `app_ids`                        text                NOT NULL COMMENT '生效的appId，null为全部生效',
    `result_description_template`    TEXT COMMENT '任务结果总体描述模板',
    `result_item_template`           text                NOT NULL COMMENT '每条任务结果描述模板',
    `result_description_template_en` TEXT COMMENT '任务结果总体描述模板英文版',
    `result_item_template_en`        text                NOT NULL COMMENT '每条任务结果描述模板英文版',
    `priority`                       int(10)             NOT NULL COMMENT '优先级',
    `active`                         bit(1)              NOT NULL COMMENT '是否启用',
    `period_seconds`                 bigint(20)          NOT NULL COMMENT '触发周期',
    `creator`                        varchar(128)        NOT NULL COMMENT '创建者',
    `last_modify_user`               varchar(128)        NULL DEFAULT NULL COMMENT '更新者',
    `create_time`                    bigint(20) UNSIGNED NULL DEFAULT NULL COMMENT '创建时间',
    `last_modify_time`               bigint(20) UNSIGNED NULL DEFAULT NULL COMMENT '更新时间',
    `description`                    TEXT COMMENT '对任务的描述',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 3;


ALTER TABLE `job_manage`.`task_template`
    ADD COLUMN `script_status` tinyint(2) UNSIGNED NOT NULL DEFAULT 0 AFTER `is_latest_version`;
ALTER TABLE `job_manage`.`task_template_step_script`
    ADD COLUMN `status` tinyint(2) UNSIGNED NOT NULL DEFAULT 0 AFTER `is_latest_version`;

ALTER TABLE `job_manage`.`task_template`
    DROP COLUMN `is_latest_version`;
ALTER TABLE `job_manage`.`task_template_step_script`
    DROP COLUMN `is_latest_version`;
CREATE TABLE IF NOT EXISTS `global_setting`
(
    `key`        varchar(255) NOT NULL,
    `value`      text         NULL,
    `decription` varchar(255) NULL DEFAULT NULL
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS `available_esb_channel`
(
    `type`             varchar(255) NOT NULL,
    `enable`           bit(1)       NOT NULL,
    `creator`          varchar(255) NULL,
    `last_modify_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`type`)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS `dangerous_rule`
(
    `id`               bigint(20)          NOT NULL AUTO_INCREMENT,
    `expression`       varchar(255)        NOT NULL COMMENT '表达式',
    `script_type`      int(10)             NOT NULL DEFAULT 1 COMMENT '脚本类型',
    `description`      text                NULL COMMENT '描述',
    `priority`         int(11)             NULL     DEFAULT NULL COMMENT '优先级',
    `action`           tinyint(4)          NOT NULL DEFAULT 1,
    `status`           tinyint(4)          NOT NULL DEFAULT 1,
    `creator`          varchar(128)        NOT NULL,
    `last_modify_user` varchar(128)        NULL     DEFAULT NULL,
    `create_time`      bigint(20) UNSIGNED NULL     DEFAULT NULL,
    `last_modify_time` bigint(20) UNSIGNED NULL     DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB;

CREATE TABLE `user_custom_script_template`
(
    `row_create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `row_update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `username`        varchar(128) NOT NULL,
    `script_language` tinyint(5)   NOT NULL,
    `script_content`  longtext     NOT NULL,
    PRIMARY KEY (`username`, `script_language`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
