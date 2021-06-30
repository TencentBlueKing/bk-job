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
CREATE SCHEMA IF NOT EXISTS job_backup;

CREATE TABLE IF NOT EXISTS `job_execute.task_instance`
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

  USE `job_backup`;

SET NAMES utf8mb4;

CREATE TABLE `job_backup.db_archive_progress` (
  `row_create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `row_update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `table_name` varchar(256) NOT NULL,
  `progress` text,
  `last_modify_time` BIGINT(20) NOT NULL DEFAULT '0',
  PRIMARY KEY (`table_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


