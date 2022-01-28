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

truncate table task_instance_rolling_config;
insert into job_execute.task_instance_rolling_config (id,task_instance_id,config_name,config) values (1,1,'config1', '{"name":"config1","includeStepInstanceIdList":[100,101,102,103],"batchRollingStepInstanceIdList":[100,102,103],"allRollingStepInstanceIdList":[101],"mode":1,"expr":"1 10% 100%","serverBatchList":[{"batch":1,"servers":[{"cloudAreaId":0,"ip":"127.0.0.1"}]},{"batch":2,"servers":[{"cloudAreaId":0,"ip":"127.0.0.2"}]},{"batch":3,"servers":[{"cloudAreaId":0,"ip":"127.0.0.3"},{"cloudAreaId":0,"ip":"127.0.0.4"}]}]}');

