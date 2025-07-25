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

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.common.mysql.util.JooqDataTypeUtil;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.execute.dao.StepInstanceFileBatchDAO;
import com.tencent.bk.job.execute.dao.common.DSLContextProviderFactory;
import com.tencent.bk.job.execute.model.FileSourceDTO;
import com.tencent.bk.job.execute.model.StepInstanceFileBatchDTO;
import com.tencent.bk.job.execute.model.tables.StepInstanceFileBatch;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jooq.Condition;
import org.jooq.Record;
import org.jooq.TableField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Repository
public class StepInstanceFileBatchDAOImpl extends BaseDAO implements StepInstanceFileBatchDAO {

    private static final StepInstanceFileBatch TABLE = StepInstanceFileBatch.STEP_INSTANCE_FILE_BATCH;

    private static final TableField<?, ?>[] ALL_FIELDS = {
        TABLE.ID,
        TABLE.TASK_INSTANCE_ID,
        TABLE.STEP_INSTANCE_ID,
        TABLE.BATCH,
        TABLE.FILE_SOURCE
    };

    public static final String BATCH_INSERT_SQL =
        "insert into step_instance_file_batch (id,task_instance_id,step_instance_id,batch,file_source)"
            + " values (?,?,?,?,?)";

    @Autowired
    public StepInstanceFileBatchDAOImpl(DSLContextProviderFactory dslContextProviderFactory) {
        super(dslContextProviderFactory, TABLE.getName());
    }

    @Override
    public int batchInsert(List<StepInstanceFileBatchDTO> stepInstanceFileBatchDTOList) {
        Object[][] params = new Object[stepInstanceFileBatchDTOList.size()][5];
        int batchCount = 0;
        for (StepInstanceFileBatchDTO stepInstanceFileBatchDTO : stepInstanceFileBatchDTOList) {
            Object[] param = new Object[5];
            param[0] = stepInstanceFileBatchDTO.getId();
            param[1] = stepInstanceFileBatchDTO.getTaskInstanceId();
            param[2] = stepInstanceFileBatchDTO.getStepInstanceId();
            param[3] = stepInstanceFileBatchDTO.getBatch();
            param[4] = JsonUtils.toJson(stepInstanceFileBatchDTO.getFileSourceList());
            params[batchCount++] = param;
        }
        return Arrays.stream(dsl().batch(BATCH_INSERT_SQL, params).execute()).sum();
    }

    @Override
    public Integer getMaxBatch(long taskInstanceId, long stepInstanceId) {
        Record record = dsl().select(TABLE.BATCH)
            .from(TABLE)
            .where(TABLE.STEP_INSTANCE_ID.eq(stepInstanceId))
            .and(buildTaskInstanceIdQueryCondition(taskInstanceId))
            .orderBy(TABLE.BATCH.desc())
            .limit(1)
            .fetchOne();
        if (record == null) {
            return 0;
        }
        return record.getValue(TABLE.BATCH, Integer.class);
    }

    @Override
    public StepInstanceFileBatchDTO get(long taskInstanceId, long stepInstanceId, int batch) {
        Record record = dsl().select(ALL_FIELDS)
            .from(TABLE)
            .where(TABLE.STEP_INSTANCE_ID.eq(stepInstanceId))
            .and(TABLE.BATCH.eq(JooqDataTypeUtil.getShortFromInteger(batch)))
            .and(buildTaskInstanceIdQueryCondition(taskInstanceId))
            .fetchOne();
        return extract(record);
    }

    @Override
    public List<StepInstanceFileBatchDTO> list(long taskInstanceId, long stepInstanceId) {
        val result = dsl().select(ALL_FIELDS)
            .from(TABLE)
            .where(TABLE.STEP_INSTANCE_ID.eq(stepInstanceId))
            .and(buildTaskInstanceIdQueryCondition(taskInstanceId))
            .orderBy(TABLE.BATCH.asc())
            .fetch();
        return result.map(this::extract);
    }

    @Override
    public int updateResolvedSourceFile(long taskInstanceId,
                                        long stepInstanceId,
                                        int batch,
                                        List<FileSourceDTO> resolvedFileSources) {
        return dsl().update(TABLE)
            .set(TABLE.FILE_SOURCE, JsonUtils.toJson(resolvedFileSources))
            .where(buildTaskInstanceIdQueryCondition(taskInstanceId))
            .and(TABLE.STEP_INSTANCE_ID.eq(stepInstanceId))
            .and(TABLE.BATCH.eq(JooqDataTypeUtil.getShortFromInteger(batch)))
            .execute();
    }

    private Condition buildTaskInstanceIdQueryCondition(Long taskInstanceId) {
        return TaskInstanceIdDynamicCondition.build(
            taskInstanceId,
            TABLE.TASK_INSTANCE_ID::eq
        );
    }

    private StepInstanceFileBatchDTO extract(Record record) {
        if (record == null) {
            return null;
        }
        StepInstanceFileBatchDTO stepInstanceFileBatchDTO = new StepInstanceFileBatchDTO();
        stepInstanceFileBatchDTO.setId(record.getValue(TABLE.ID));
        stepInstanceFileBatchDTO.setTaskInstanceId(record.getValue(TABLE.TASK_INSTANCE_ID));
        stepInstanceFileBatchDTO.setStepInstanceId(record.getValue(TABLE.STEP_INSTANCE_ID));
        stepInstanceFileBatchDTO.setBatch(record.getValue(TABLE.BATCH));
        stepInstanceFileBatchDTO.setFileSourceList(
            JsonUtils.fromJson(record.getValue(TABLE.FILE_SOURCE), new TypeReference<List<FileSourceDTO>>() {
            })
        );
        return stepInstanceFileBatchDTO;
    }
}
