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

CREATE SCHEMA IF NOT EXISTS job_analysis;
USE job_analysis;
SET NAMES UTF8MB4;

DROP TABLE IF EXISTS `analysis_task_instance`;
CREATE TABLE `analysis_task_instance`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `app_id` bigint(20) NOT NULL COMMENT '业务id',
  `task_id` bigint(255) NOT NULL COMMENT '任务id',
  `status` int(10) NOT NULL COMMENT '任务状态',
  `result_data` text NOT NULL COMMENT '任务结果数据，各任务自定义格式',
  `priority` int(10) NOT NULL COMMENT '优先级',
  `active` bit(1) NOT NULL COMMENT '是否启用',
  `creator` varchar(128) NOT NULL COMMENT '创建者',
  `last_modify_user` varchar(128) NULL DEFAULT NULL COMMENT '更新者',
  `create_time` bigint(20) UNSIGNED NULL DEFAULT NULL COMMENT '创建时间',
  `last_modify_time` bigint(20) UNSIGNED NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE = InnoDB AUTO_INCREMENT = 961;

DROP TABLE IF EXISTS `analysis_task`;
CREATE TABLE `analysis_task`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `code` varchar(255) NOT NULL COMMENT '任务代码，用于匹配处理器',
  `app_ids` text NOT NULL COMMENT '生效的appId，null为全部生效',
  `result_description_template` TEXT COMMENT '任务结果总体描述模板',
  `result_item_template` text NOT NULL COMMENT '每条任务结果描述模板',
  `result_description_template_en` TEXT COMMENT '任务结果总体描述模板英文版',
  `result_item_template_en` text NOT NULL COMMENT '每条任务结果描述模板英文版',
  `priority` int(10) NOT NULL COMMENT '优先级',
  `active` bit(1) NOT NULL COMMENT '是否启用',
  `period_seconds` bigint(20) NOT NULL COMMENT '触发周期',
  `creator` varchar(128) NOT NULL COMMENT '创建者',
  `last_modify_user` varchar(128) NULL DEFAULT NULL COMMENT '更新者',
  `create_time` bigint(20) UNSIGNED NULL DEFAULT NULL COMMENT '创建时间',
  `last_modify_time` bigint(20) UNSIGNED NULL DEFAULT NULL COMMENT '更新时间',
  `description` TEXT COMMENT '对任务的描述',
  PRIMARY KEY (`id`)
) ENGINE = InnoDB AUTO_INCREMENT = 3;

