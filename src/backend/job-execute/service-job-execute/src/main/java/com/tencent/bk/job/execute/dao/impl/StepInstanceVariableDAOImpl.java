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

import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.execute.common.util.JooqDataTypeUtil;
import com.tencent.bk.job.execute.constants.VariableValueTypeEnum;
import com.tencent.bk.job.execute.dao.StepInstanceVariableDAO;
import com.tencent.bk.job.execute.model.StepInstanceVariableValuesDTO;
import org.apache.commons.lang3.StringUtils;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.TableField;
import org.jooq.generated.tables.StepInstanceVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class StepInstanceVariableDAOImpl implements StepInstanceVariableDAO {
    private static final StepInstanceVariable TABLE = StepInstanceVariable.STEP_INSTANCE_VARIABLE;
    private DSLContext ctx;
    private TableField[] FIELDS = {TABLE.TASK_INSTANCE_ID, TABLE.STEP_INSTANCE_ID,
            TABLE.EXECUTE_COUNT, TABLE.TYPE, TABLE.PARAM_VALUES};


    @Autowired
    public StepInstanceVariableDAOImpl(@Qualifier("job-execute-dsl-context") DSLContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void saveVariableValues(StepInstanceVariableValuesDTO variableValues) {
        ctx.insertInto(TABLE, FIELDS)
                .values(variableValues.getTaskInstanceId(), variableValues.getStepInstanceId(),
                        variableValues.getExecuteCount(), JooqDataTypeUtil.getByteFromInteger(variableValues.getType()),
                        JsonUtils.toJson(variableValues))
                .execute();
    }

    @Override
    public StepInstanceVariableValuesDTO getStepVariableValues(long stepInstanceId, int executeCount,
                                                               VariableValueTypeEnum variableValueType) {
        Record record = ctx.select(FIELDS)
                .from(TABLE)
                .where(TABLE.STEP_INSTANCE_ID.eq(stepInstanceId))
                .and(TABLE.EXECUTE_COUNT.eq(executeCount))
                .and(TABLE.TYPE.eq(JooqDataTypeUtil.getByteFromInteger(variableValueType.getValue())))
                .limit(1)
                .fetchOne();
        return extract(record);
    }

    private StepInstanceVariableValuesDTO extract(Record record) {
        if (record == null || record.size() == 0) {
            return null;
        }

        StepInstanceVariableValuesDTO stepInstanceVariableValues = new StepInstanceVariableValuesDTO();
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
    public List<StepInstanceVariableValuesDTO> listStepOutputVariableValuesByTaskInstanceId(long taskInstanceId) {
        Result result = ctx.select(FIELDS)
            .from(TABLE)
            .where(TABLE.TASK_INSTANCE_ID.eq(taskInstanceId))
            .and(TABLE.TYPE.eq(JooqDataTypeUtil.getByteFromInteger(VariableValueTypeEnum.OUTPUT.getValue())))
            .orderBy(TABLE.STEP_INSTANCE_ID.asc(), TABLE.EXECUTE_COUNT.asc())
            .fetch();

        List<StepInstanceVariableValuesDTO> varList = new ArrayList<>();
        if (!result.isEmpty()) {
            result.into(record -> varList.add(extract(record)));
        }
        return varList;
    }

    @Override
    public List<StepInstanceVariableValuesDTO> listSortedPreStepOutputVariableValues(long taskInstanceId,
                                                                                     long stepInstanceId) {
        Result result = ctx.select(FIELDS)
            .from(TABLE)
            .where(TABLE.TASK_INSTANCE_ID.eq(taskInstanceId))
            .and(TABLE.STEP_INSTANCE_ID.lt(stepInstanceId))
            .and(TABLE.TYPE.eq(JooqDataTypeUtil.getByteFromInteger(VariableValueTypeEnum.OUTPUT.getValue())))
            .orderBy(TABLE.STEP_INSTANCE_ID.asc(), TABLE.EXECUTE_COUNT.asc())
            .fetch();

        List<StepInstanceVariableValuesDTO> varList = new ArrayList<>();
        if (!result.isEmpty()) {
            result.into(record -> varList.add(extract(record)));
        }
        return varList;
    }
}
