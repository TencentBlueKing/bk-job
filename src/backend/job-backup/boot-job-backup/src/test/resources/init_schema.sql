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
CREATE SCHEMA IF NOT EXISTS job_backup;
USE job_backup;

CREATE TABLE IF NOT EXISTS `archive_task` (
  `row_create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `row_update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `task_type` tinyint(2) DEFAULT NULL,
  `data_node` varchar(128) NOT NULL,
  `db_node` varchar(64) NOT NULL,
  `day` int(8) DEFAULT NULL,
  `hour` tinyint(2) DEFAULT NULL,
  `from_timestamp` bigint(20) NOT NULL,
  `to_timestamp` bigint(20) NOT NULL,
  `process` varchar(256) DEFAULT NULL,
  `status` tinyint(2) NOT NULL DEFAULT '0',
  `create_time` bigint(20) NOT NULL,
  `last_update_time` bigint(20) NOT NULL,
  `task_start_time` bigint(20) DEFAULT NULL,
  `task_end_time` bigint(20) DEFAULT NULL,
  `task_cost` bigint(20) DEFAULT NULL,
  `detail` text,
  PRIMARY KEY (`id`),
  UNIQUE KEY (`task_type`,`data_node`,`day`,`hour`),
  KEY (`task_type`,`status`,`db_node`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


