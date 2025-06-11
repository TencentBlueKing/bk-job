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

CREATE SCHEMA IF NOT EXISTS job_crontab;
USE job_crontab;
SET NAMES utf8mb4;

CREATE TABLE `cron_job`
(
    `id`                  BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    `row_create_time`     DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `row_update_time`     DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `app_id`              BIGINT(20) UNSIGNED NOT NULL,
    `name`                VARCHAR(512)        NOT NULL,
    `creator`             VARCHAR(128)        NOT NULL,
    `task_template_id`    BIGINT UNSIGNED              DEFAULT NULL,
    `task_plan_id`        BIGINT(20) UNSIGNED          DEFAULT NULL,
    `script_id`           CHAR(32)                     DEFAULT NULL,
    `script_version_id`   BIGINT(20) UNSIGNED          DEFAULT NULL,
    `cron_expression`     VARCHAR(512)                 DEFAULT NULL,
    `execute_time`        BIGINT(20) UNSIGNED          DEFAULT NULL,
    `variable_value`      LONGTEXT            NOT NULL,
    `last_execute_status` TINYINT(2) UNSIGNED NOT NULL DEFAULT '0',
    `is_enable`           TINYINT(1) UNSIGNED NOT NULL DEFAULT '0',
    `is_deleted`          TINYINT(1) UNSIGNED NOT NULL DEFAULT '0',
    `create_time`         BIGINT(20) UNSIGNED NOT NULL,
    `last_modify_user`    VARCHAR(128)        NOT NULL,
    `last_modify_time`    BIGINT(20) UNSIGNED NOT NULL DEFAULT '0',
    `end_time`            BIGINT(20) UNSIGNED NOT NULL DEFAULT '0',
    `notify_offset`       BIGINT(20) UNSIGNED NOT NULL DEFAULT '0',
    `notify_user`         VARCHAR(2048)                DEFAULT NULL,
    `notify_channel`      varchar(1024)                DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_name` (`name`) USING BTREE,
    KEY `idx_creator` (`creator`) USING BTREE,
    KEY `idx_app_id` (`app_id`) USING BTREE,
    KEY `idx_task_plan_id` (`task_plan_id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
