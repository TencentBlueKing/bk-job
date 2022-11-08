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

TRUNCATE TABLE job_execute.gse_file_agent_task;

INSERT INTO job_execute.gse_file_agent_task
            (step_instance_id,
             execute_count,
             batch,
             mode,
             host_id,
             agent_id,
             gse_task_id,
             status,
             start_time,
             end_time,
             total_time,
             error_code)
VALUES
    (1,0,1,0,101,'0:127.0.0.1',1,9,1565767148000,1565767149000,1000,0),
    (1,0,1,1,102,'0:127.0.0.2',2,9,1565767148000,1565767149000,1000,0),
    (1,0,2,0,101,'0:127.0.0.1',3,9,1565767150000,1565767151000,1000,0),
    (1,0,2,1,103,'0:127.0.0.3',3,9,1565767150000,1565767151000,1000,0),
    (1,0,2,1,104,'0:127.0.0.4',3,11,1565767150000,1565767151000,1000,99),
    (2,0,0,0,101,'0:127.0.0.1',4,9,1565767148000,1565767149000,1000,0),
    (2,0,0,1,102,'0:127.0.0.2',4,9,1565767148000,1565767149000,1000,0);
