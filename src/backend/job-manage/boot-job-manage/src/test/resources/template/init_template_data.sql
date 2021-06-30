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
TRUNCATE TABLE `task_template`;
INSERT INTO `task_template` (id, app_id, name, description, creator, status, is_deleted, create_time, last_modify_user,
                             last_modify_time, tags, first_step_id, last_step_id, version, script_status)
VALUES (1, 1000, '测试模版1', '这是一个测试模版。这段描述是随便写的，没什么意义。1', 'userC', 0, 0, 1569859200, 'userC',
        1569859200, '<1>,<2>,<3>', 1000, 2000, 'abcd1234', 1),
       (2, 1000, '测试模版2', '这是一个测试模版。这段描述是随便写的，没什么意义。2', 'userC', 1, 0, 1569859200, 'userT',
        1569945600, '<2>,<3>,<4>', 2000, 3000, 'abcd1234', 0),
       (3, 1000, '测试模版3', '这是一个测试模版。这段描述是随便写的，没什么意义。3', 'userC', 2, 0, 1569859200, 'userC',
        1570464000, '<3>,<4>,<5>', 3000, 4000, 'abcd1234', 1),
       (4, 1000, '测试模版4', '这是一个测试模版。这段描述是随便写的，没什么意义。4', 'userT', 3, 0, 1569859200, 'userT',
        1570118400, '<1>,<3>,<5>', 4000, 5000, 'abcd1234', 0),
       (5, 1000, '测试模版5', '这是一个测试模版。这段描述是随便写的，没什么意义。5', 'userT', 2, 0, 1569859200, 'userC',
        1570204800, '<2>,<4>,<6>', 5000, 6000, 'abcd1234', 1),
       (6, 1000, '测试模版6', '这是一个测试模版。这段描述是随便写的，没什么意义。6', 'userT', 1, 0, 1569859200, 'userT',
        1570035600, '<1>,<4>,<7>', 6000, 7000, 'abcd1234', 0),
       (7, 2000, '测试模版7', '这是一个测试模版。这段描述是随便写的，没什么意义。7', 'userT', 0, 0, 1569859200, 'userC',
        1569949200, '<2>,<5>,<8>', 7000, 8000, 'abcd1234', 1),
       (8, 2000, '测试模版8', '这是一个测试模版。这段描述是随便写的，没什么意义。8', 'userC', 2, 0, 1569859200, 'userT',
        1570291200, '<3>,<6>,<9>', 8000, 9000, 'abcd1234', 0),
       (9, 1000, '测试模版9', '这是一个测试模版。这段描述是随便写的，没什么意义。9', 'userT', 1, 1, 1569859200, 'userC',
        1569859200, '<1>,<6>,<9>', 9000, 9000, 'abcd1234', 1);
