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

import com.tencent.bk.job.common.mysql.dynamic.ds.DbOperationEnum;
import com.tencent.bk.job.common.mysql.dynamic.ds.MySQLOperation;
import com.tencent.bk.job.common.mysql.jooq.JooqDataTypeUtil;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.execute.constants.VariableValueTypeEnum;
import com.tencent.bk.job.execute.dao.StepInstanceVariableDAO;
import com.tencent.bk.job.execute.dao.common.DSLContextProviderFactory;
import com.tencent.bk.job.execute.model.StepInstanceVariableValuesDTO;
import com.tencent.bk.job.execute.model.tables.StepInstanceVariable;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.TableField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class StepInstanceVariableDAOImpl extends BaseDAO implements StepInstanceVariableDAO {
    private static final StepInstanceVariable TABLE = StepInstanceVariable.STEP_INSTANCE_VARIABLE;

    private final TableField<?, ?>[] FIELDS = {
        TABLE.ID,
        TABLE.TASK_INSTANCE_ID,
        TABLE.STEP_INSTANCE_ID,
        TABLE.EXECUTE_COUNT,
        TABLE.TYPE,
        TABLE.PARAM_VALUES
    };


    @Autowired
    public StepInstanceVariableDAOImpl(DSLContextProviderFactory dslContextProviderFactory) {
        super(dslContextProviderFactory, TABLE.getName());
    }

    @Override
    @MySQLOperation(table = "step_instance_variable", op = DbOperationEnum.WRITE)
    public void saveVariableValues(StepInstanceVariableValuesDTO variableValues) {
        dsl().insertInto(
                TABLE,
                TABLE.ID,
                TABLE.TASK_INSTANCE_ID,
                TABLE.STEP_INSTANCE_ID,
                TABLE.EXECUTE_COUNT,
                TABLE.TYPE,
                TABLE.PARAM_VALUES)
            .values(
                variableValues.getId(),
                variableValues.getTaskInstanceId(),
                variableValues.getStepInstanceId(),
                variableValues.getExecuteCount(),
                JooqDataTypeUtil.toByte(variableValues.getType()),
                JsonUtils.toJson(variableValues))
            .execute();
    }

    @Override
    @MySQLOperation(table = "step_instance_variable", op = DbOperationEnum.READ)
    public StepInstanceVariableValuesDTO getStepVariableValues(Long taskInstanceId,
                                                               long stepInstanceId,
                                                               int executeCount,
                                                               VariableValueTypeEnum variableValueType) {
        Record record = dsl().select(FIELDS)
            .from(TABLE)
            .where(TaskInstanceIdDynamicCondition.build(taskInstanceId,
                TABLE.TASK_INSTANCE_ID::eq))
            .and(TABLE.STEP_INSTANCE_ID.eq(stepInstanceId))
            .and(TABLE.EXECUTE_COUNT.eq(executeCount))
            .and(TABLE.TYPE.eq(JooqDataTypeUtil.toByte(variableValueType.getValue())))
            .limit(1)
            .fetchOne();
        return extract(record);
    }

    private StepInstanceVariableValuesDTO extract(Record record) {
        if (record == null) {
            return null;
        }

        StepInstanceVariableValuesDTO stepInstanceVariableValues = new StepInstanceVariableValuesDTO();
        stepInstanceVariableValues.setId(record.get(TABLE.ID));
        stepInstanceVariableValues.setTaskInstanceId(record.get(TABLE.TASK_INSTANCE_ID));
        stepInstanceVariableValues.setStepInstanceId(record.get(TABLE.STEP_INSTANCE_ID));
        stepInstanceVariableValues.setExecuteCount(record.get(TABLE.EXECUTE_COUNT));
        stepInstanceVariableValues.setType(record.get(TABLE.TYPE).intValue());
        String paramValueJson = record.get(TABLE.PARAM_VALUES);
        if (StringUtils.isNotBlank(paramValueJson)) {
            StepInstanceVariableValuesDTO paramValues = JsonUtils.fromJson(paramValueJson,
                StepInstanceVariableValuesDTO.class);
            stepInstanceVariableValues.setGlobalParams(paramValues.getGlobalParams());
            stepInstanceVariableValues.setNamespaceParams(paramValues.getNamespaceParams());
        }
        return stepInstanceVariableValues;
    }

    @Override
    @MySQLOperation(table = "step_instance_variable", op = DbOperationEnum.READ)
    public List<StepInstanceVariableValuesDTO> listStepOutputVariableValuesByTaskInstanceId(long taskInstanceId) {
        Result result = dsl().select(FIELDS)
            .from(TABLE)
            .where(TABLE.TASK_INSTANCE_ID.eq(taskInstanceId))
            .and(TABLE.TYPE.eq(JooqDataTypeUtil.toByte(VariableValueTypeEnum.OUTPUT.getValue())))
            .orderBy(TABLE.STEP_INSTANCE_ID.asc(), TABLE.EXECUTE_COUNT.asc())
            .fetch();

        List<StepInstanceVariableValuesDTO> varList = new ArrayList<>();
        if (!result.isEmpty()) {
            result.into(record -> varList.add(extract(record)));
        }
        return varList;
    }
}
