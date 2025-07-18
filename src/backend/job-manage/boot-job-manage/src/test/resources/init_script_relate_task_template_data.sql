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

truncate table task_template_step;
truncate table task_template_step_script;
truncate table task_template;

INSERT INTO `task_template` (id, app_id, name, creator, create_time, last_modify_user, last_modify_time, version, is_deleted)
VALUES
(1, 2, 'job_template_1', 'admin', 1598356556, 'admin', 1598356556, '1.0', 0),
(2, 2, 'job_template_2', 'admin', 1598356556, 'admin', 1598356556, '1.0', 0),
(3, 2, 'job_template_3', 'admin', 1598356556, 'admin', 1598356556, '1.0', 1);

INSERT INTO `task_template_step` (id, template_id, name, type, script_step_id, is_deleted)
VALUES
(1, 1, 'job_template_1_step_1', 1, 1, 0),
(2, 1, 'job_template_2_step_1', 1, 2, 0),
(3, 2, 'job_template_2_step_2', 1, 3, 0),
(4, 3, 'job_template_3_step_1', 1, 4, 1);

INSERT INTO `task_template_step_script` (id, template_id, step_id, script_type, language, script_id, script_version_id, script_timeout, execute_account)
VALUES
(1, 1, 1, 2, 1, 'scriptid1', 1, 1000, 1),
(2, 2, 2, 2, 1, 'scriptid1', 2, 1000, 1),
(3, 2, 3, 2, 1, 'scriptid2', 3, 1000, 1),
(4, 3, 4, 2, 1, 'scriptid1', 1, 1000, 1);
