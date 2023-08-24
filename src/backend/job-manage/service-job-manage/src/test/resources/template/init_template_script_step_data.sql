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

USE `job_manage`;
TRUNCATE TABLE `task_template_step_script`;
INSERT INTO `task_template_step_script` (id, template_id, step_id, script_type, script_id, script_version_id,
                                         content, language, script_param, script_timeout, execute_account,
                                         destination_host_list, is_secure_param, status, ignore_error)
VALUES (1, 100000, 1000, 1, '1000', 1000, null, 1, 'a=a', 600, 1, null, 0, 1, 0),
       (2, 100000, 2000, 2, '2000', 2000, 'this is a sample content', 1, null, 600, 2, null, 1, 0, 1),
       (3, 100000, 3000, 1, '3000', 3000, null, 1, 'c=c', 600, 3, null, 1, 1, 0),
       (4, 200000, 4000, 1, '1000', 1000, null, 1, 'a=a', 600, 1, null, 0, 0, 0);
