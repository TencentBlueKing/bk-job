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

truncate table account;

insert into job_manage.account (id,account,alias,category,type,app_id,grantee,remark,os,password,db_password,db_port,db_system_account_id,
creator,create_time,last_modify_user,last_modify_time)
values (1,'root','root',1,1,2,'user1,user2','root-linux','Linux',NULL,NULL,NULL,NULL,'admin', 1569550210000, 'admin', 1569550210000);
insert into job_manage.account (id,account,alias,category,type,app_id,grantee,remark,os,password,db_password,db_port,db_system_account_id,
creator,create_time,last_modify_user,last_modify_time)
values (2,'system','system',1,2,2,'user1,user2','system-window','Windows','mypassword',NULL,NULL,NULL,'admin', 1569550210000, 'admin', 1569550210000);
insert into job_manage.account (id,account,alias,category,type,app_id,grantee,remark,os,password,db_password,db_port,db_system_account_id,
creator,create_time,last_modify_user,last_modify_time)
values (3,'job','job',2,9,2,'user1,user2','db-mysql-job',NULL,NULL,'dbpassword',3600,1,'admin', 1569550210000, 'admin', 1569550210000);
insert into job_manage.account (id,account,alias,category,type,app_id,grantee,remark,os,password,db_password,db_port,db_system_account_id,
creator,create_time,last_modify_user,last_modify_time)
values (4,'root','root-v2',1,1,2,'user1,user2','root-linux','Linux',NULL,NULL,NULL,NULL,'admin', 1569550210000, 'admin', 1569636611000);
