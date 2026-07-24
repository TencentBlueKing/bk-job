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

truncate table step_instance;
truncate table step_instance_script;
truncate table step_instance_file;
truncate table step_instance_confirm;

insert into job_execute.step_instance (
id,app_id,task_instance_id,step_id,name,type,target_servers,operator,status,execute_count,start_time,end_time,total_time,create_time,step_num,step_order)
values
 (1,2,1,1,'task1-step1',1,'{"ipList":[{"cloudAreaId":0,"ip":"127.0.0.1"}]}','admin',3,0,1572868800000,1572868801000,1111,1572868800000,2,1),
 (2,2,1,2,'task1-step2',2,'{"ipList":[{"cloudAreaId":0,"ip":"127.0.0.1"}]}','admin',3,0,1572868801000,1572868802000,1112,1572868800000,2,2),
 (3,2,2,-1,'fast_execute_task_name',1,'{"ipList":[{"cloudAreaId":0,"ip":"127.0.0.1"}]}','admin',3,0,1572868800000,1572868801000,1111,1572868800000,1,1),
 (4,2,3,-1,'fast_execute_task_name',1,'{"ipList":[{"cloudAreaId":0,"ip":"127.0.0.1"}]}','admin',3,0,null,1572868801000,0,null,1,1),
 -- 兼容性回归：containerFilters 仅含 v4 形态的 clusterFilter/namespaceFilter（字符串 UID/名称），
 -- 无 propConditions、无 Web 入口形态的 clusters/namespaces/workloads 对象。
 -- 出库后下游 propConditions 必须 null、Web 拓扑对象集合必须 null，hasContainerExecuteObject = true。
 (20,2,20,-1,'legacy_container_target',1,'{"containerFilters":[{"clusterFilter":{"clusterUIDs":["BCS-K8S-00001"]},"namespaceFilter":{"namespaces":["default"]}}]}','admin',3,0,1572868800000,1572868801000,1111,1572868800000,1,1),
 -- 新特性回归：containerFilters 携带 Web 入口形态的拓扑对象（id+name）与 propConditions，
 -- 模拟 Web 入口的「动态条件作为执行目标」典型写入形态。
 (21,2,21,-1,'new_container_condition_target',1,'{"containerFilters":[{"kubeTopoList":[{"cluster":{"id":1000},"namespace":{"id":10000}}],"propConditions":[{"field":"container_container_uid","operator":"equal","value":"docker://nginx-1-24"},{"field":"pod_name","operator":"equal","value":"pod-a"}]}]}','admin',3,0,1572868800000,1572868801000,1111,1572868800000,1,1);

insert into job_execute.step_instance_script(step_instance_id,task_instance_id,script_content,script_type,script_param,resolved_script_param,execution_timeout,system_account_id,system_account,
                                             db_account_id,db_type,db_account,db_password,db_port,script_source,script_id,script_version_id,is_secure_param,windows_interpreter) values (1,1,'script_content',1,'${var1}','var1',1000,1,'root',11,1,'root','ESKsXn+pF9hACG3BSYG38ZnUjQQ8bUcOylREiEnDTPU=',3306,1,NULL,NULL,1,NULL);

insert into job_execute.step_instance_file(step_instance_id,task_instance_id,file_source,resolved_file_source,file_target_path,file_target_name,resolved_file_target_path,file_upload_speed_limit,file_download_speed_limit,
                                           file_duplicate_handle,not_exist_path_handler,execution_timeout,system_account_id,system_account) values (2,1,'[{"files":[{ "filePath":"/${log_dir}/1.log" }],"localUpload":false}]',
                                                                                                                                                    '[{"files":[{ "resolvedFilePath":"/tmp/1.log", "filePath":"/${log_dir}/1.log" }],"localUpload":false}]','/${log_dir}/','2.log','/tmp/',
                                                                                                                                                    100,100,1,1,1000,1,'root');

insert into job_execute.step_instance_confirm(step_instance_id,task_instance_id,confirm_message,confirm_users,confirm_roles,notify_channels,confirm_reason) values (
                                                                                                                                                      15,13,'confirm_message','admin,test','JOB_RESOURCE_TRIGGER_USER','weixin','confirm_reason');


