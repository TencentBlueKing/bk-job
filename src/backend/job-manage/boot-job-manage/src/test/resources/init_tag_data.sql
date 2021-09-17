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

USE job_manage;
TRUNCATE TABLE `tag`;
INSERT INTO `tag` (id, app_id, name, description, creator, last_modify_user, create_time, last_modify_time) VALUES (1, 1, 'test1', 'test1-desc', 'userC', 'userT', 1630648088, 1630648088);
INSERT INTO `tag` (id, app_id, name, description, creator, last_modify_user, create_time, last_modify_time) VALUES (2, 1, 'test2', 'test2-desc', 'userC', 'userT', 1630648089, 1630648089);
INSERT INTO `tag` (id, app_id, name, description, creator, last_modify_user, create_time, last_modify_time) VALUES (3, 2, 'test1', 'test1-desc', 'userC', 'userT', 1630648090, 1630648090);
INSERT INTO `tag` (id, app_id, name, description, creator, last_modify_user, create_time, last_modify_time) VALUES (4, 2, 'test2', 'test2-desc', 'userC', 'userT', 1630648091, 1630648091);
INSERT INTO `tag` (id, app_id, name, description, creator, last_modify_user, create_time, last_modify_time) VALUES (5, 2, 'Test3', 'Test3-desc', 'userC', 'userT', 1630648092, 1630648092);
