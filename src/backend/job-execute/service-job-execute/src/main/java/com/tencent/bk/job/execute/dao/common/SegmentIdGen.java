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

import com.tencent.bk.job.common.sharding.mysql.SegmentIdKeys;
import com.tencent.devops.leaf.service.SegmentService;

public class SegmentIdGen implements IdGen {

    private final SegmentService segmentService;

    public SegmentIdGen(SegmentService segmentServices) {
        this.segmentService = segmentServices;
    }

    private Long gen(String key) {
        return segmentService.getId(key).getId();
    }

    public Long genTaskInstanceId() {
        return gen(SegmentIdKeys.KEY_JOB_EXECUTE_TASK_INSTANCE);
    }

    public Long genStepInstanceId() {
        return gen(SegmentIdKeys.KEY_JOB_EXECUTE_STEP_INSTANCE);
    }

    public Long genGseTaskId() {
        return gen(SegmentIdKeys.KEY_JOB_EXECUTE_GSE_TASK);
    }

    public Long genOperationLogId() {
        return gen(SegmentIdKeys.KEY_JOB_EXECUTE_OPERATION_LOG);
    }

    public Long genFileSourceTaskLogId() {
        return gen(SegmentIdKeys.KEY_JOB_EXECUTE_FILE_SOURCE_TASK_LOG);
    }

    public Long genGseFileExecuteObjTaskId() {
        return gen(SegmentIdKeys.KEY_JOB_EXECUTE_GSE_FILE_EXECUTE_OBJ_TASK);
    }

    public Long genGseScriptExecuteObjTaskId() {
        return gen(SegmentIdKeys.KEY_JOB_EXECUTE_GSE_SCRIPT_EXECUTE_OBJ_TASK);
    }

    public Long genRollingConfigId() {
        return gen(SegmentIdKeys.KEY_JOB_EXECUTE_ROLLING_CONFIG);
    }

    public Long genStepInstanceRollingTaskId() {
        return gen(SegmentIdKeys.KEY_JOB_EXECUTE_STEP_INSTANCE_ROLLING_TASK);
    }

    public Long genStepInstanceVariableId() {
        return gen(SegmentIdKeys.KEY_JOB_EXECUTE_STEP_INSTANCE_VARIABLE);
    }

    public Long genTaskInstanceVariableId() {
        return gen(SegmentIdKeys.KEY_JOB_EXECUTE_TASK_INSTANCE_VARIABLE);
    }
}
