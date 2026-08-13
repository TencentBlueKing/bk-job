/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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

DROP TABLE IF EXISTS `approval_task`;
CREATE TABLE `approval_task`  (
  `id`                    bigint(20)          NOT NULL AUTO_INCREMENT,
  `approval_task_id`      varchar(32)         NOT NULL COMMENT '对外暴露的审批任务ID(32位UUID，无连字符，不可猜测)',
  `tenant_id`             varchar(32)         NOT NULL COMMENT '租户ID',
  `app_id`                bigint(20)          NOT NULL COMMENT '业务ID',
  `operation_type`        varchar(64)         NOT NULL COMMENT '操作类型',
  `operation_params`      mediumtext          NOT NULL COMMENT '操作参数快照(JSON，敏感字段已加密)',
  `resolved_summary`      mediumtext          NULL COMMENT 'dryRun解析出的概要(JSON)',
  `creator`               varchar(128)        NOT NULL COMMENT '发起人',
  `app_code`              varchar(128)        NOT NULL DEFAULT '' COMMENT '发起方appCode',
  `approval_channel`      varchar(32)         NOT NULL COMMENT '审批渠道枚举',
  `approval_ticket_id`    varchar(256)        NULL COMMENT '审批渠道单据ID',
  `ticket_fetched_at`     bigint(20) UNSIGNED NULL COMMENT '审批渠道拉取审批内容的时间(毫秒)',
  `status`                varchar(32)         NOT NULL COMMENT '状态',
  `approver`              varchar(128)        NULL COMMENT '审批人',
  `approved_at`           bigint(20) UNSIGNED NULL COMMENT '审批通过时间(毫秒)',
  `execute_result`        mediumtext          NULL COMMENT '放行后的操作结果(JSON)',
  `expire_at`             bigint(20) UNSIGNED NOT NULL COMMENT '过期时刻(毫秒)',
  `consumed_at`           bigint(20) UNSIGNED NULL COMMENT '被消费(CAS成功)时刻(毫秒)',
  `dispatched_at`         bigint(20) UNSIGNED NULL COMMENT '下发下游执行请求的时刻(毫秒)',
  `create_time`           bigint(20) UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建时间(毫秒)',
  `row_create_time`       datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `row_update_time`       datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY (`approval_task_id`) USING BTREE,
  KEY `idx_app_creator_status` (`app_id`, `creator`, `status`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE,
  KEY `idx_expire_at` (`expire_at`) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

