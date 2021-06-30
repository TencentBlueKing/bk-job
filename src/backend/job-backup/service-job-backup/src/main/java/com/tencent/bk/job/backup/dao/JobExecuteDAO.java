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

package com.tencent.bk.job.backup.dao;

import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.generated.tables.records.*;

import java.util.List;

public interface JobExecuteDAO {
    Long getMaxNeedArchiveTaskInstanceId(Long endTime);

    Long getMaxNeedArchiveStepInstanceId(Long taskInstanceId);

    <T extends Record> Long getFirstInstanceId(Table<T> table, TableField<T, Long> field);

    List<Field<?>> getFileSourceTaskLogFields();

    List<FileSourceTaskLogRecord> listFileSourceTaskLog(Long start, Long stop);

    List<Field<?>> getGseTaskIpLogFields();

    List<GseTaskIpLogRecord> listGseTaskIpLog(Long start, Long stop);

    List<Field<?>> getGseTaskLogFields();

    List<GseTaskLogRecord> listGseTaskLog(Long start, Long stop);

    List<Field<?>> getStepInstanceFields();

    List<StepInstanceRecord> listStepInstance(Long start, Long stop);

    List<Field<?>> getStepInstanceConfirmFields();

    List<StepInstanceConfirmRecord> listStepInstanceConfirm(Long start, Long stop);

    List<Field<?>> getStepInstanceFileFields();

    List<StepInstanceFileRecord> listStepInstanceFile(Long start, Long stop);

    List<Field<?>> getStepInstanceScriptFields();

    List<StepInstanceScriptRecord> listStepInstanceScript(Long start, Long stop);

    List<Field<?>> getStepInstanceVariableFields();

    List<StepInstanceVariableRecord> listStepInstanceVariable(Long start, Long stop);

    List<Field<?>> getTaskInstanceFields();

    List<TaskInstanceRecord> listTaskInstance(Long start, Long stop);

    List<Field<?>> getTaskInstanceVariableFields();

    List<Field<?>> getOperationLogFields();

    List<TaskInstanceVariableRecord> listTaskInstanceVariable(Long start, Long stop);

    List<OperationLogRecord> listOperationLog(Long lastInstanceId, Long stop);

    int deleteFileSourceTaskLog(Long start, Long stop);

    int deleteGseTaskIpLog(Long start, Long stop);

    int deleteGseTaskLog(Long start, Long stop);

    int deleteOperationLog(Long start, Long stop);

    int deleteStepInstance(Long start, Long stop);

    int deleteStepInstanceConfirm(Long start, Long stop);

    int deleteStepInstanceFile(Long start, Long stop);

    int deleteStepInstanceScript(Long start, Long stop);

    int deleteStepInstanceVariable(Long start, Long stop);

    int deleteTaskInstance(Long start, Long stop);

    int deleteTaskInstanceVariable(Long start, Long stop);
}
