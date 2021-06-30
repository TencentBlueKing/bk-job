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
TRUNCATE TABLE `task_template_step_file_list`;
INSERT INTO `task_template_step_file_list` (id, step_id, file_type,
                                            file_location, file_size, file_hash, host, host_account)
VALUES (1, 1000, 1, '["/tmp/files/file_one"]', null, null, '{"hostNodeList":{"hostList":[{"ip":"1.1.1.1"}]}}', 1),
       (2, 1000, 2, '["/data/storage/upload/file_two"]', 1000,
        'bb13fc8fc2a72856189b0f4eb7882248a0d5e05b53ded0fb2a34d41d81dbe85b', null, null),
       (3, 2000, 2, '["/home/userC/file/file_three"]', 1000, null,
        '{"hostNodeList":{"hostList":[{"ip":"2.2.2.2"}]}}', 2);
