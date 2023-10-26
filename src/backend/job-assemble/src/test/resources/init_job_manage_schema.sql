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
CREATE TABLE IF NOT EXISTS `global_setting`
(
    `key`        varchar(255) NOT NULL,
    `value`      text         NULL,
    `decription` varchar(255) NULL DEFAULT NULL
) ENGINE = InnoDB;

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
    `bk_scope_type`       VARCHAR(32)                  DEFAULT '',
    `bk_scope_id`         VARCHAR(32)                  DEFAULT '',
    `is_deleted`           TINYINT(1) UNSIGNED NOT NULL DEFAULT '0',
    `attrs`               TEXT                         DEFAULT NULL,
    PRIMARY KEY (`app_id`),
    KEY (`app_type`)
) ENGINE = INNODB
  DEFAULT CHARSET = UTF8MB4;
