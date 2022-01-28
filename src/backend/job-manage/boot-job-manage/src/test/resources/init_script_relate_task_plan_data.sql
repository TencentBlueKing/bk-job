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

truncate table task_plan;
truncate table task_plan_step_script;
truncate table script_version;

INSERT INTO `task_plan_step_script` (id, plan_id, step_id, script_type, script_id, script_version_id,
                                         content, language, script_param, script_timeout, execute_account,
                                         destination_host_list, is_secure_param, is_latest_version)
VALUES (1, 1, 1, 1, '1000', 1000, null, 1, 'a=a', 600, 1, null, 0, 1),
       (2, 2, 2, 2, '2000', 2001, null, 1, null, 600, 2, null, 1, 0),
       (3, 3, 3, 1, '2000', 2001, null, 1, 'c=c', 600, 3, null, 1, 1),
       (4, 3, 4, 1, '1000', 1001, null, 1, 'a=a', 600, 1, null, 0, 0),
       (5, 3, 4, 1, '2000', 2001, null, 1, 'a=a', 600, 1, null, 0, 0);

INSERT INTO `task_plan` (id, app_id, template_id, name, creator, last_modify_user)
VALUES (1, 2, 1, 'plan1', 'admin', 'admin'),
(2, 2, 2, 'plan2', 'admin', 'admin'),
(3, 2, 3, 'plan3', 'admin', 'admin'),
(4, 2, 4, 'plan4', 'admin', 'admin');

INSERT INTO `job_manage`.`script_version`(`id`, `row_create_time`, `row_update_time`, `script_id`, `content`, `creator`, `create_time`, `last_modify_user`, `last_modify_time`, `version`, `is_deleted`, `status`, `version_desc`) VALUES (1000, '2019-12-27 12:05:15', '2020-05-06 14:20:03', '1000', '', 'admin', 1577390716, 'admin', 1577390716, 'v1.0.0', 0, 1, NULL);
INSERT INTO `job_manage`.`script_version`(`id`, `row_create_time`, `row_update_time`, `script_id`, `content`, `creator`, `create_time`, `last_modify_user`, `last_modify_time`, `version`, `is_deleted`, `status`, `version_desc`) VALUES (1001, '2019-12-27 12:05:15', '2020-05-06 14:20:03', '1000', '', 'admin', 1577390716, 'admin', 1577390716, 'v1.0.0', 0, 1, NULL);
INSERT INTO `job_manage`.`script_version`(`id`, `row_create_time`, `row_update_time`, `script_id`, `content`, `creator`, `create_time`, `last_modify_user`, `last_modify_time`, `version`, `is_deleted`, `status`, `version_desc`) VALUES (2001, '2019-12-27 12:10:06', '2020-04-14 10:17:10', '2000', '', 'admin', 1577391007, 'admin', 1577391007, '127.0.0.0.1', 1, 2, NULL);
