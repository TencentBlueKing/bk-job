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

truncate table task_instance;
truncate table step_instance;
truncate table gse_task_ip_log;

insert into job_execute.task_instance (id,app_id,task_id,cron_task_id,task_template_id,is_debug_task,name,type,operator,create_time,status,current_step_id,start_time,end_time,total_time,startup_mode,callback_url,app_code) values (1,2,1,NULL,1,0,'task1',1,'admin',1572868800000,3,11,1572868800000,1572868801000,1111,1,'http://bkjob.com','bk_monitor');
insert into job_execute.task_instance (id,app_id,task_id,cron_task_id,task_template_id,is_debug_task,name,type,operator,create_time,status,current_step_id,start_time,end_time,total_time,startup_mode,callback_url,app_code) values (2,2,1,NULL,1,0,'task1',1,'admin',1572868860000,3,12,1572868800000,1572868861000,1222,1,'http://bkjob.com',NULL);
insert into job_execute.task_instance (id,app_id,task_id,cron_task_id,task_template_id,is_debug_task,name,type,operator,create_time,status,current_step_id,start_time,end_time,total_time,startup_mode,callback_url,app_code) values (3,2,2,NULL,2,0,'task2',1,'admin',1572868860000,3,13,1572868800000,1572868861000,1322,1,'http://bkjob.com',NULL);

insert into job_execute.step_instance (id,app_id,task_instance_id,step_id,name,type,target_servers,operator,status,execute_count,start_time,end_time,total_time,
create_time,step_num,step_order) values (1,2,1,1,'task1-step1',1,'{"ipList":[{"cloudAreaId":0,"ip":"10.0.0.1"},{"cloudAreaId":0,"ip":"10.0.0.2"}]}','admin',3,0,1572868800000,1572868801000,1111,1572868800000,2,1);
insert into job_execute.step_instance (id,app_id,task_instance_id,step_id,name,type,target_servers,operator,status,execute_count,start_time,end_time,total_time,
create_time,step_num,step_order) values (2,2,2,1,'task2-step1',1,'{"ipList":[{"cloudAreaId":0,"ip":"10.0.0.2"}]}','admin',3,0,1572868800000,1572868801000,1111,1572868800000,2,1);
insert into job_execute.step_instance (id,app_id,task_instance_id,step_id,name,type,target_servers,operator,status,execute_count,start_time,end_time,total_time,
create_time,step_num,step_order) values (3,2,3,1,'task2-step1',1,'{"ipList":[{"cloudAreaId":0,"ip":"10.0.0.2"}]}','admin',3,0,1572868800000,1572868801000,1111,1572868800000,2,1);

insert into job_execute.gse_task_ip_log (step_instance_id,execute_count,ip,status,start_time,end_time,total_time,error_code,exit_code,tag,log_offset,display_ip,is_target,is_source) values (1,0,'0:10.0.0.1',9,1565767148000,1565767149000,1316,0,0,'succ',0,'10.0.0.1',1,0);
insert into job_execute.gse_task_ip_log (step_instance_id,execute_count,ip,status,start_time,end_time,total_time,error_code,exit_code,tag,log_offset,display_ip,is_target,is_source) values (1,0,'0:10.0.0.2',9,1565767148000,1565767149000,1316,0,0,'succ',0,'10.0.0.2',1,0);
insert into job_execute.gse_task_ip_log (step_instance_id,execute_count,ip,status,start_time,end_time,total_time,error_code,exit_code,tag,log_offset,display_ip,is_target,is_source) values (2,0,'0:10.0.0.2',9,1565767148000,1565767149000,1316,0,0,'succ',0,'10.0.0.2',1,0);
insert into job_execute.gse_task_ip_log (step_instance_id,execute_count,ip,status,start_time,end_time,total_time,error_code,exit_code,tag,log_offset,display_ip,is_target,is_source) values (3,0,'0:10.0.0.3',9,1565767148000,1565767149000,1316,0,0,'succ',0,'10.0.0.3',1,0);
