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

truncate table dangerous_record;

insert into job_execute.dangerous_record (id,rule_id,rule_expression,app_id,app_name,operator,script_language,script_content,create_time,startup_mode,client,action,check_result,
ext_data) values (1, 1, 'rm -rf',2,'BlueKing','admin',1,'#!/bin/bash\nrm -rf *',1619748000000,2,'app1',1,'{"results":[{"line":"2","level":3,"description":"rm -rf forbidden"}]}','{"request_param":"aaa"}');
insert into job_execute.dangerous_record (id,rule_id,rule_expression,app_id,app_name,operator,script_language,script_content,create_time,startup_mode,client,action,check_result,
ext_data) values (2, 2,'shutdown',3,'Test','userT',2,'shutdown',1619834400000,3,'job',2,'{"results":[{"line":"1","level":3,"description":"shutdown forbidden"}]}','{"request_param":"bbb"}');



