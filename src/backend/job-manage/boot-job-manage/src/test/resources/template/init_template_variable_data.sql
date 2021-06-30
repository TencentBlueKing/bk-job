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
TRUNCATE TABLE `task_template_variable`;
INSERT INTO `task_template_variable` (id, template_id, name, type, default_value, description, is_changeable, is_required)
    VALUES (1, 10000, '测试1', 1, 'test1', '这是一个测试变量1', true, true);
INSERT INTO `task_template_variable` (id, template_id, name, type, default_value, description, is_changeable, is_required)
    VALUES (2, 10000, '测试2', 2, 'test2', '这是一个测试变量2', false, false);
INSERT INTO `task_template_variable` (id, template_id, name, type, default_value, description, is_changeable, is_required)
    VALUES (3, 10000, '测试3', 3, 'test3', '这是一个测试变量3', true, false);
INSERT INTO `task_template_variable` (id, template_id, name, type, default_value, description, is_changeable, is_required)
    VALUES (4, 10000, '测试4', 4, 'test4', '这是一个测试变量4', false, true);
INSERT INTO `task_template_variable` (id, template_id, name, type, default_value, description, is_changeable, is_required)
    VALUES (5, 20000, '测试5', 1, 'test5', '这是一个测试变量5', true, true);
INSERT INTO `task_template_variable` (id, template_id, name, type, default_value, description, is_changeable, is_required)
    VALUES (6, 20000, '测试6', 2, 'test6', '这是一个测试变量6', false, false);
INSERT INTO `task_template_variable` (id, template_id, name, type, default_value, description, is_changeable, is_required)
    VALUES (7, 20000, '测试7', 3, 'test7', '这是一个测试变量7', true, false);
INSERT INTO `task_template_variable` (id, template_id, name, type, default_value, description, is_changeable, is_required)
    VALUES (8, 20000, '测试8', 4, 'test8', '这是一个测试变量8', false, true);
