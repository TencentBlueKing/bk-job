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

TRUNCATE TABLE job_execute.gse_script_agent_task;

INSERT INTO job_execute.gse_script_agent_task
            (step_instance_id,
             execute_count,
             batch,
             host_id,
             agent_id,
             gse_task_id,
             status,
             start_time,
             end_time,
             total_time,
             error_code,
             exit_code,
             tag,
             log_offset)
VALUES
    (1,0,1,101,'0:127.0.0.1',1,9,1565767148000,1565767149000,1316,0,0,'succ',0),
    (1,0,2,102,'0:127.0.0.2',2,9,1565767148000,1565767149000,1211,0,0,'succ',0),
    (1,0,3,103,'0:127.0.0.3',3,9,1565767148000,1565767149000,1211,0,0,'succ',0),
    (1,0,3,104,'0:127.0.0.4',3,9,1565767148000,1565767149000,1211,0,0,'succ',0),
    (1,0,3,105,'0:127.0.0.5',3,11,1565767148000,1565767149000,1211,0,0,'fail',0),
    (2,0,0,101,'0:127.0.0.1',4,9,1565767148000,1565767209000,1211,0,0,'succ',0),
    (2,1,0,101,'0:127.0.0.1',4,9,1565766610000,1565767211000,1215,0,0,'succ',0),
    (3,0,0,101,'0:127.0.0.1',5,9,1565766610000,1565767211000,1215,0,0,'succ',0),
    (3,0,0,102,'0:127.0.0.2',5,9,1565766610000,1565767211000,1215,0,0,'succ',0);
