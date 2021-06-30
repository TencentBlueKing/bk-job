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

USE `job_crontab`;
TRUNCATE TABLE `job_crontab`.`cron_job`;
INSERT INTO `cron_job` (id, app_id, name, creator, task_template_id, task_plan_id, script_id, script_version_id,
                        cron_expression, execute_time, variable_value, last_execute_status, is_enable, is_deleted,
                        create_time, last_modify_user, last_modify_time, end_time, notify_offset, notify_user,
                        notify_channel)
VALUES (1, 2, 'cron_job_1', 'userC', 1, 100, null, null, '* * * * *', null,
        '[{"id":null,"name":"a","type":3,"value":"b","server":null},{"id":null,"name":"b","type":4,"value":"c","server":null}]',
        null, 1, 0, '1546272000', 'userT', '1546272000', 0, 600,
        '{"userList":["userC", "userJ"], "roleList":["JOB_ROLE_1", "JOB_ROLE_2"]}', '["wechat", "email"]'),
       (2, 2, 'cron_job_2', 'userT', 2, 200, null, null, null, '1546272000',
        '[{"id":null,"name":"a","type":3,"value":"b","server":null},{"id":null,"name":"b","type":4,"value":"c","server":null}]',
        1, 0, 1, '1546272000', 'userT', '1546272000', 0, 600,
        '{"userList":["userT", "userJ"], "roleList":["JOB_ROLE_3", "JOB_ROLE_4"]}', '["email"]'),
       (3, 2, 'cron_job_3', 'userC', 3, 300, null, null, '* * * * *', null,
        '[{"id":null,"name":"a","type":3,"value":"b","server":null},{"id":null,"name":"b","type":4,"value":"c","server":null}]',
        0, 1, 0, '1546272000', 'userC', '1546272000', 1577808000, 0, null, null),
       (4, 2, 'cron_job_4', 'userT', 4, 400, null, null, null, '1546272000',
        '[{"id":null,"name":"a","type":3,"value":"b","server":null},{"id":null,"name":"b","type":4,"value":"c","server":null}]',
        null, 1, 0, '1546272000', 'userC', '1546272000', 0, 0, null, null),
       (5, 2, 'cron_job_5', 'userC', 5, 500, null, null, '* * * * *', null,
        '[{"id":null,"name":"a","type":3,"value":"b","server":null},{"id":null,"name":"b","type":4,"value":"c","server":null}]',
        1, 1, 0, '1546272000', 'userT', '1546272000', 0, 0, null, null),
       (6, 2, 'cron_job_6', 'userT', null, null, 'aaaa', 1, null, '1546272000',
        '[{"id":null,"name":"a","type":3,"value":"b","server":null},{"id":null,"name":"b","type":4,"value":"c","server":null}]',
        0, 1, 0, '1546272000', 'userT', '1546272000', 0, 0, null, null),
       (7, 2, 'cron_job_7', 'userC', null, null, 'bbbb', 2, '* * * * *', null,
        '[{"id":null,"name":"a","type":3,"value":"b","server":null},{"id":null,"name":"b","type":4,"value":"c","server":null}]',
        null, 0, 1, '1546272000', 'userC', '1546272000', 0, 0, null, null),
       (8, 2, 'cron_job_8', 'userT', null, null, 'cccc', 3, null, '1546272000',
        '[{"id":null,"name":"a","type":3,"value":"b","server":null},{"id":null,"name":"b","type":4,"value":"c","server":null}]',
        1, 1, 0, '1546272000', 'userC', '1546272000', 0, 0, null, null),
       (9, 2, 'cron_job_9', 'userC', null, null, 'vvvv', 4, '* * * * *', null,
        '[{"id":null,"name":"a","type":3,"value":"b","server":null},{"id":null,"name":"b","type":4,"value":"c","server":null}]',
        0, 1, 0, '1546272000', 'userC', '1546272000', 0, 0, null, null)
;
