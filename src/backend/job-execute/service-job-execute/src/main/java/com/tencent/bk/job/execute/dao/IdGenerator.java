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

package com.tencent.bk.job.execute.dao;

import com.tencent.bk.job.common.sharding.mysql.config.ShardingsphereProperties;
import com.tencent.devops.leaf.service.SegmentService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 执行引擎模块 ID 生成
 */
@Component("jobExecuteIdGenerator")
public class IdGenerator {

    private final SegmentService segmentService;

    private volatile boolean shardingEnabled;

    @Autowired
    public IdGenerator(ObjectProvider<ShardingsphereProperties> shardingspherePropertiesObjectProperty,
                       ObjectProvider<SegmentService> segmentServiceObjectProperty) {
        ShardingsphereProperties shardingsphereProperties = shardingspherePropertiesObjectProperty.getIfAvailable();
        this.segmentService = segmentServiceObjectProperty.getIfAvailable();

        if (shardingsphereProperties != null) {
            shardingEnabled = shardingsphereProperties.isEnabled();
        }
    }

    public Long gen(String key) {
        if (!shardingEnabled) {
            // 如果不允许分库分表，那么返回 null，将使用默认的 MySQL AUTO_INCREMENT 生成 ID
            return null;
        }
        return segmentService.getId(key).getId();
    }

    public Long genTaskInstanceId() {
        return gen("job_execute.task_instance");
    }

    public Long genStepInstanceId() {
        return gen("job_execute.step_instance");
    }

    public Long genGseTaskId() {
        return gen("job_execute.gse_task");
    }

    public Long genOperationLogId() {
        return gen("job_execute.operation_log");
    }

    public Long genFileSourceTaskLogId() {
        return gen("job_execute.file_source_task_log");
    }

    public Long genGseFileExecuteObjTaskId() {
        return gen("job_execute.gse_file_execute_obj_task");
    }

    public Long genGseScriptExecuteObjTaskId() {
        return gen("job_execute.gse_script_execute_obj_task");
    }

    public Long genRollingConfigId() {
        return gen("job_execute.rolling_config");
    }

    public Long genStepInstanceRollingTaskId() {
        return gen("job_execute.step_instance_rolling_task");
    }

    public Long genStepInstanceVariableId() {
        return gen("job_execute.step_instance_variable");
    }

    public Long genTaskInstanceVariableId() {
        return gen("job_execute.task_instance_variable");
    }
}
