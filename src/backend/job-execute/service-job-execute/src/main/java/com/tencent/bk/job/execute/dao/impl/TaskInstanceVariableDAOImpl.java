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

package com.tencent.bk.job.execute.dao.impl;

import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.common.crypto.scenario.CipherVariableCryptoService;
import com.tencent.bk.job.common.mysql.dynamic.ds.DbOperationEnum;
import com.tencent.bk.job.common.mysql.dynamic.ds.MySQLOperation;
import com.tencent.bk.job.common.mysql.jooq.JooqDataTypeUtil;
import com.tencent.bk.job.execute.dao.TaskInstanceVariableDAO;
import com.tencent.bk.job.execute.dao.common.DSLContextProviderFactory;
import com.tencent.bk.job.execute.engine.model.TaskVariableDTO;
import com.tencent.bk.job.execute.model.tables.TaskInstanceVariable;
import com.tencent.bk.job.execute.model.tables.records.TaskInstanceVariableRecord;
import org.jooq.InsertValuesStep6;
import org.jooq.Record;
import org.jooq.Record6;
import org.jooq.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * 作业实例全局变量DAO
 */
@Repository
public class TaskInstanceVariableDAOImpl extends BaseDAO implements TaskInstanceVariableDAO {
    private static final TaskInstanceVariable TABLE = TaskInstanceVariable.TASK_INSTANCE_VARIABLE;

    private final CipherVariableCryptoService cipherVariableCryptoService;

    @Autowired
    public TaskInstanceVariableDAOImpl(DSLContextProviderFactory dslContextProviderFactory,
                                       CipherVariableCryptoService cipherVariableCryptoService) {
        super(dslContextProviderFactory, TABLE.getName());
        this.cipherVariableCryptoService = cipherVariableCryptoService;
    }

    @Override
    @MySQLOperation(table = "task_instance_variable", op = DbOperationEnum.READ)
    public List<TaskVariableDTO> getByTaskInstanceId(long taskInstanceId) {
        Result<Record6<Long, Long, String, Byte, Byte, String>> result = dsl().select(
                TABLE.ID,
                TABLE.TASK_INSTANCE_ID,
                TABLE.NAME,
                TABLE.TYPE,
                TABLE.IS_CHANGEABLE,
                TABLE.VALUE
            ).from(TABLE)
            .where(TABLE.TASK_INSTANCE_ID.eq(taskInstanceId))
            .fetch();
        List<TaskVariableDTO> taskVariables = new ArrayList<>();
        if (!result.isEmpty()) {
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
        TaskVariableTypeEnum taskVarType = TaskVariableTypeEnum.valOf(taskVariable.getType());
        String encryptedValue = record.get(TABLE.VALUE);
        String value = cipherVariableCryptoService.decryptTaskVariableIfNeeded(taskVarType, encryptedValue);
        taskVariable.setValue(value);
        taskVariable.setChangeable(JooqDataTypeUtil.toInteger(record.get(TABLE.IS_CHANGEABLE)).equals(1));
        return taskVariable;
    }

    @Override
    @MySQLOperation(table = "task_instance_variable", op = DbOperationEnum.WRITE)
    public void batchDeleteByTaskInstanceIds(List<Long> taskInstanceIdList) {

    }

    @Override
    @MySQLOperation(table = "task_instance_variable", op = DbOperationEnum.WRITE)
    public void deleteByTaskInstanceId(long taskInstanceId) {

    }

    @Override
    @MySQLOperation(table = "task_instance_variable", op = DbOperationEnum.WRITE)
    public void saveTaskInstanceVariables(List<TaskVariableDTO> taskVarList) {
        InsertValuesStep6<TaskInstanceVariableRecord, Long, Long, String, Byte, String, Byte> insertStep =
            dsl().insertInto(TABLE)
                .columns(
                    TABLE.ID,
                    TABLE.TASK_INSTANCE_ID,
                    TABLE.NAME,
                    TABLE.TYPE,
                    TABLE.VALUE,
                    TABLE.IS_CHANGEABLE
                );

        taskVarList.forEach(taskVar -> {
            TaskVariableTypeEnum taskVarType = TaskVariableTypeEnum.valOf(taskVar.getType());
            insertStep.values(
                taskVar.getId(),
                taskVar.getTaskInstanceId(),
                taskVar.getName(),
                JooqDataTypeUtil.toByte(taskVar.getType()),
                cipherVariableCryptoService.encryptTaskVariableIfNeeded(taskVarType, taskVar.getValue()),
                JooqDataTypeUtil.toByte(taskVar.isChangeable() ? 1 : 0)
            );
        });

        insertStep.execute();
    }
}
