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

SET NAMES utf8mb4;
CREATE SCHEMA IF NOT EXISTS job_execute;
USE job_execute;

CREATE TABLE IF NOT EXISTS `task_instance`
(
    `id`               bigint(20)   NOT NULL AUTO_INCREMENT,
    `app_id`           bigint(20)   NOT NULL,
    `task_id`          bigint(20)   NOT NULL,
    `task_template_id` bigint(20)   NOT NULL,
    `name`             varchar(512) NOT NULL,
    `type`             tinyint(4)   NOT NULL,
    `operator`         varchar(128) NOT NULL,
    `status`           tinyint(4)   NOT NULL DEFAULT '0',
    `current_step_id`  bigint(20)   NOT NULL DEFAULT '0',
    `startup_mode`     tinyint(4)   NOT NULL,
    `total_time`       bigint(20)            DEFAULT NULL,
    `callback_url`     varchar(1024)         DEFAULT NULL,
    `is_debug_task`    tinyint(4)   NOT NULL DEFAULT '0',
    `cron_task_id`     bigint(20)   NOT NULL DEFAULT '0',
    `create_time`      bigint(20)            DEFAULT NULL,
    `start_time`       bigint(20)            DEFAULT NULL,
    `end_time`         bigint(20)            DEFAULT NULL,
    `app_code`         varchar(128)          DEFAULT NULL,
    `row_create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `row_update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY (`app_id`),
    KEY (`operator`),
    KEY (`task_id`),
    KEY (`status`),
    KEY (`create_time`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1000000
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `step_instance`
(
    `id`                     bigint(20) NOT NULL AUTO_INCREMENT,
    `step_id`                bigint(20) NOT NULL,
    `task_instance_id`       bigint(20) NOT NULL,
    `app_id`                 bigint(20) NOT NULL,
    `name`                   varchar(512)        DEFAULT NULL,
    `type`                   tinyint(4) NOT NULL,
    `operator`               varchar(128)        DEFAULT NULL,
    `status`                 tinyint(4) NOT NULL DEFAULT '1',
    `execute_count`          int(11)    NOT NULL DEFAULT '0',
    `target_servers`         longtext,
    `abnormal_agent_ip_list` longtext,
    `start_time`             bigint(20)          DEFAULT NULL,
    `end_time`               bigint(20)          DEFAULT NULL,
    `total_time`             bigint(20)          DEFAULT NULL,
    `total_ip_num`           int(11)             DEFAULT '0',
    `abnormal_agent_num`     int(11)             DEFAULT '0',
    `run_ip_num`             int(11)             DEFAULT '0',
    `fail_ip_num`            int(11)             DEFAULT '0',
    `success_ip_num`         int(11)             DEFAULT '0',
    `create_time`            bigint(20)          DEFAULT NULL,
    `ignore_error`           tinyint(4) NOT NULL DEFAULT 0,
    `step_num`               int(11)    NOT NULL DEFAULT 0,
    `step_order`             int(11)    NOT NULL DEFAULT 0,
    `row_create_time`        DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `row_update_time`        DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY (`task_instance_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1000000
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `step_instance_script`
(
    `step_instance_id`      bigint(20) NOT NULL,
    `script_content`        mediumtext,
    `script_type`           tinyint(4)          DEFAULT NULL,
    `script_param`          text,
    `resolved_script_param` text,
    `execution_timeout`     int(11)             DEFAULT NULL,
    `system_account_id`     bigint(20)          DEFAULT NULL,
    `system_account`        varchar(256)        DEFAULT NULL,
    `db_account_id`         bigint(20)          DEFAULT NULL,
    `db_type`               tinyint(4)          DEFAULT NULL,
    `db_account`            varchar(256)        DEFAULT NULL,
    `db_password`           varchar(512)        DEFAULT NULL,
    `db_port`               int(5)              DEFAULT NULL,
    `row_create_time`       DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `row_update_time`       DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `script_source`         tinyint(4)          DEFAULT 1,
    `script_id`             varchar(32)         DEFAULT NULL,
    `script_version_id`     bigint(20)          DEFAULT NULL,
    `is_secure_param`       tinyint(1)          DEFAULT 0,
    PRIMARY KEY (`step_instance_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `step_instance_file`
(
    `step_instance_id`          bigint(20) NOT NULL,
    `file_source`               mediumtext NOT NULL,
    `resolved_file_source`      mediumtext          DEFAULT NULL,
    `file_target_path`          varchar(512)        DEFAULT NULL,
    `file_target_name`          varchar(512)        DEFAULT NULL,
    `resolved_file_target_path` varchar(512)        DEFAULT NULL,
    `file_upload_speed_limit`   int(11)             DEFAULT NULL,
    `file_download_speed_limit` int(11)             DEFAULT NULL,
    `file_duplicate_handle`     tinyint(4)          DEFAULT NULL,
    `not_exist_path_handler`    tinyint(4)          DEFAULT NULL,
    `execution_timeout`         int(11)             DEFAULT NULL,
    `system_account_id`         bigint(20)          DEFAULT NULL,
    `system_account`            varchar(256)        DEFAULT NULL,
    `row_create_time`           DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `row_update_time`           DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`step_instance_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `step_instance_confirm`
(
    `step_instance_id` bigint(20) NOT NULL,
    `confirm_message`  text       NOT NULL,
    `confirm_reason`   varchar(256)        DEFAULT NULL,
    `confirm_users`    varchar(1024)       DEFAULT NULL,
    `confirm_roles`    varchar(512)        DEFAULT NULL,
    `notify_channels`  varchar(256)        DEFAULT NULL,
    `row_create_time`  DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `row_update_time`  DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`step_instance_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `gse_task_log`
(
    `step_instance_id` bigint(20) NOT NULL DEFAULT '0',
    `execute_count`    int(11)    NOT NULL DEFAULT '0',
    `start_time`       bigint(20)          DEFAULT NULL,
    `end_time`         bigint(20)          DEFAULT NULL,
    `total_time`       bigint(11)          DEFAULT NULL,
    `status`           tinyint(4)          DEFAULT '1',
    `gse_task_id`      varchar(64)         DEFAULT NULL,
    `row_create_time`  DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `row_update_time`  DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`step_instance_id`, `execute_count`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `gse_task_ip_log`
(
    `step_instance_id` bigint(20)  NOT NULL,
    `execute_count`    int(11)     NOT NULL DEFAULT '0',
    `ip`               varchar(30) NOT NULL,
    `status`           int(11)              DEFAULT '1',
    `start_time`       bigint(20)           DEFAULT NULL,
    `end_time`         bigint(20)           DEFAULT NULL,
    `total_time`       bigint(20)           DEFAULT NULL,
    `error_code`       int(11)              DEFAULT '0',
    `exit_code`        int(11)              DEFAULT NULL,
    `tag`              varchar(256)         DEFAULT '',
    `log_offset`       int(11)     NOT NULL DEFAULT '0',
    `display_ip`       varchar(30) NOT NULL,
    `is_target`        tinyint(1)  NOT NULL default '1',
    `is_source`        tinyint(1)  NOT NULL default '0',
    `row_create_time`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `row_update_time`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`step_instance_id`, `execute_count`, `ip`),
    KEY (`display_ip`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `operation_log`
(
    `id`               bigint(20)   NOT NULL AUTO_INCREMENT,
    `task_instance_id` bigint(20)   NOT NULL,
    `op_code`          tinyint(4)   NOT NULL,
    `operator`         varchar(255) NOT NULL,
    `detail`           text,
    `create_time`      bigint(20)            DEFAULT NULL,
    `row_create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `row_update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY (`task_instance_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `task_instance_variable`
(
    `id`               bigint(20)   NOT NULL AUTO_INCREMENT,
    `task_instance_id` bigint(20)   NOT NULL,
    `name`             varchar(512) NOT NULL,
    `type`             tinyint(4)   NOT NULL,
    `value`            longtext,
    `is_changeable`    tinyint(1)   NOT NULL,
    `row_create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `row_update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY (`task_instance_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `dangerous_record`
(
    `row_create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `row_update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `id`              bigint(20) NOT NULL AUTO_INCREMENT,
    `rule_id`         bigint(20) NOT NULL,
    `rule_expression` varchar(255) NOT NULL,
    `app_id`          bigint(20) NOT NULL,
    `app_name`        varchar(1024) NOT NULL,
    `operator`        varchar(128) NOT NULL,
    `script_language` tinyint(4) NOT NULL,
    `script_content`  longtext NOT NULL,
    `create_time`     bigint(20) NOT NULL,
    `startup_mode`    tinyint(4) NOT NULL,
    `client`          varchar(128) NOT NULL,
    `action`            tinyint(4) NOT NULL,
    `check_result`    text NOT NULL,
    `ext_data` text,
    PRIMARY KEY (`id`),
    KEY `idx_create_time_rule_id` (`create_time`,`rule_id`),
    KEY `idx_create_time_rule_expression` (`create_time`,`rule_expression`),
    KEY `idx_create_time_app_id` (`create_time`,`app_id`),
    KEY `idx_create_time_operator` (`create_time`,`operator`),
    KEY `idx_create_time_startup_mode` (`create_time`,`startup_mode`),
    KEY `idx_create_time_client` (`create_time`,`client`),
    KEY `idx_create_time_mode` (`create_time`,`action`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `step_instance_variable`
(
    `id`               bigint(20) NOT NULL AUTO_INCREMENT,
    `task_instance_id` bigint(20) NOT NULL,
    `step_instance_id` bigint(20) NOT NULL,
    `execute_count`    int(11)    NOT NULL DEFAULT '0',
    `type`             tinyint(4) NOT NULL,
    `param_values`     longtext   NOT NULL,
    `row_create_time`  DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `row_update_time`  DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY (`step_instance_id`, `execute_count`, `type`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

