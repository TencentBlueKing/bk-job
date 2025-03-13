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

package com.tencent.bk.job.execute.dao.common;

import com.tencent.bk.job.common.mysql.dynamic.ds.VerticalShardingDSLContextProvider;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;

import java.util.HashMap;
import java.util.Map;

/**
 * 垂直分库 MySQL Jooq DSLContext 提供者
 */
@Slf4j
public class JobExecuteVerticalShardingDSLContextProvider extends VerticalShardingDSLContextProvider {

    /**
     * 垂直分库，固定分成 3 个分库，每个分库与表的关系也是固定的。
     *
     * @param dslContextA 分库 a
     * @param dslContextB 分库 b
     * @param dslContextC 分库 c
     */
    public JobExecuteVerticalShardingDSLContextProvider(DSLContext dslContextA,
                                                        DSLContext dslContextB,
                                                        DSLContext dslContextC) {
        super();
        Map<String, DSLContext> tableNameAndDslContextMap = new HashMap<>();
        tableNameAndDslContextMap.put("task_instance", dslContextA);
        tableNameAndDslContextMap.put("task_instance_host", dslContextA);
        tableNameAndDslContextMap.put("gse_script_agent_task", dslContextB);
        tableNameAndDslContextMap.put("gse_script_execute_obj_task", dslContextB);
        tableNameAndDslContextMap.put("dangerous_record", dslContextC);
        tableNameAndDslContextMap.put("file_source_task_log", dslContextC);
        tableNameAndDslContextMap.put("gse_file_agent_task", dslContextC);
        tableNameAndDslContextMap.put("gse_file_execute_obj_task", dslContextC);
        tableNameAndDslContextMap.put("gse_task", dslContextC);
        tableNameAndDslContextMap.put("gse_task_ip_log", dslContextC);
        tableNameAndDslContextMap.put("gse_task_log", dslContextC);
        tableNameAndDslContextMap.put("operation_log", dslContextC);
        tableNameAndDslContextMap.put("rolling_config", dslContextC);
        tableNameAndDslContextMap.put("statistics", dslContextC);
        tableNameAndDslContextMap.put("step_instance", dslContextC);
        tableNameAndDslContextMap.put("step_instance_confirm", dslContextC);
        tableNameAndDslContextMap.put("step_instance_file", dslContextC);
        tableNameAndDslContextMap.put("step_instance_rolling_task", dslContextC);
        tableNameAndDslContextMap.put("step_instance_script", dslContextC);
        tableNameAndDslContextMap.put("step_instance_variable", dslContextC);
        tableNameAndDslContextMap.put("task_instance_variable", dslContextC);
        super.initTableNameAndDslContextMap(tableNameAndDslContextMap);
    }
}
