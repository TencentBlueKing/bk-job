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

USE job_manage;
TRUNCATE TABLE `dangerous_rule`;
INSERT INTO `dangerous_rule` (expression, script_type, description, priority, creator, last_modify_user, create_time,
                              last_modify_time, action, status,tenant_id)
VALUES ('shell',1,'Shell-1',1,'test1','test1','1607439270294','1607439270294',0,1,'tencent'),
       ('shell,bat,perl',7,'Shell-1,Bat-2,Perl-3',1,'test1','test1','1607439270294','1607439270294',1,1,'tencent'),
       ('shell,bat',3,'Shell-1,Bat-2',1,'test1','test1','1607439270294','1607439270294',1,1,'tencent'),
       ('shell,bat,perl,python',15,'Shell-1,Bat-2,Perl-3,Python-4',1,'test1','test1','1607439270294','1607439270294',1,1,'tencent'),
       ('shell,bat,perl,python,powershell',31,'Shell-1,Bat-2,Perl-3,Python-4,Powershell-5',1,'test1','test1','1607439270294','1607439270294',0,1,'tencent'),
       ('shell,bat,perl,python,powershell,sql',63,'Shell-1,Bat-2,Perl-3,Python-4,Powershell-5,SQL-6',1,'test1','test1','1607439270294','1607439270294',1,1,'tencent'),
       ('sql',32,'SQL-6',1,'test1','test1','1607439270294','1607439270294',1,1,'tencent'),
       ('shell,sql',33,'Shell-1,SQL-6',1,'test1','test1','1607439270294','1607439270294',1,1,'tencent'),
       ('shell,python',9,'Shell-1,Python-4',1,'test1','test1','1607439270294','1607439270294',1,1,'tencent'),
       ('python',8,'Python-4',1,'test1','test1','1607439270294','1607439270294',0,1,'tencent');
