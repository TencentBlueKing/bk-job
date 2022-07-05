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

package com.tencent.bk.job.execute.dao.impl;

import com.tencent.bk.job.execute.common.util.JooqDataTypeUtil;
import com.tencent.bk.job.execute.dao.TaskInstanceVariableDAO;
import com.tencent.bk.job.execute.engine.model.TaskVariableDTO;
import org.jooq.DSLContext;
import org.jooq.InsertValuesStep5;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.generated.tables.TaskInstanceVariable;
import org.jooq.generated.tables.records.TaskInstanceVariableRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * 作业实例全局变量DAO
 */
@Repository
public class TaskInstanceVariableDAOImpl implements TaskInstanceVariableDAO {
    private static final TaskInstanceVariable TABLE = TaskInstanceVariable.TASK_INSTANCE_VARIABLE;
    private DSLContext ctx;

    @Autowired
    public TaskInstanceVariableDAOImpl(@Qualifier("job-execute-dsl-context") DSLContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public List<TaskVariableDTO> getByTaskInstanceId(long taskInstanceId) {
        Result result = ctx.select(TABLE.ID, TABLE.TASK_INSTANCE_ID, TABLE.NAME, TABLE.TYPE, TABLE.IS_CHANGEABLE,
            TABLE.VALUE)
            .from(TABLE)
            .where(TABLE.TASK_INSTANCE_ID.eq(taskInstanceId))
            .fetch();
        List<TaskVariableDTO> taskVariables = new ArrayList<>();
        if (result.size() > 0) {
            result.into(record -> taskVariables.add(extract((record))));
        }
        return taskVariables;
    }

    private TaskVariableDTO extract(Record record) {
        TaskVariableDTO taskVariable = new TaskVariableDTO();
        taskVariable.setId(record.get(TABLE.ID));
        taskVariable.setTaskInstanceId(record.get(TABLE.TASK_INSTANCE_ID));
        taskVariable.setName(record.get(TABLE.NAME));
        taskVariable.setType(JooqDataTypeUtil.toInteger(record.get(TABLE.TYPE)));
        taskVariable.setValue(record.get(TABLE.VALUE));
        taskVariable.setChangeable(JooqDataTypeUtil.toInteger(record.get(TABLE.IS_CHANGEABLE)).equals(1));
        return taskVariable;
    }

    @Override
    public void batchDeleteByTaskInstanceIds(List<Long> taskInstanceIdList) {

    }

    @Override
    public void deleteByTaskInstanceId(long taskInstanceId) {

    }

    @Override
    public void saveTaskInstanceVariables(List<TaskVariableDTO> taskVarList) {
        InsertValuesStep5<TaskInstanceVariableRecord, Long, String, Byte, String, Byte> insertStep =
            ctx.insertInto(TABLE).columns(TABLE.TASK_INSTANCE_ID, TABLE.NAME, TABLE.TYPE, TABLE.VALUE,
                TABLE.IS_CHANGEABLE);

        taskVarList.forEach(taskVar -> insertStep.values(taskVar.getTaskInstanceId(),
            taskVar.getName(), JooqDataTypeUtil.toByte(taskVar.getType()),
            taskVar.getValue(), JooqDataTypeUtil.toByte(taskVar.isChangeable() ? 1 : 0)));

        insertStep.execute();
    }
}
