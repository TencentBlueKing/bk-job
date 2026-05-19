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

package com.tencent.bk.job.execute.passwordrotation;

import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.common.crypto.passwordrotation.FieldBatchRow;
import com.tencent.bk.job.common.crypto.passwordrotation.FieldRewriter;
import com.tencent.bk.job.execute.model.tables.TaskInstanceVariable;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Record2;
import org.jooq.Result;
import org.jooq.SelectConditionStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * task_instance_variable 表 value 字段密码轮换：仅处理 type=4 (CIPHER) 的敏感变量。
 * 仅当 job.encrypt.oldDataPasswordRotation.includeExecutionHistoryTables=true 时启用。
 */
@Slf4j
@Component
@ConditionalOnProperty(
    prefix = "job.encrypt.old-data-password-rotation",
    name = "include-execution-history-tables",
    havingValue = "true"
)
@Qualifier("jobExecutePasswordRotationRewriter")
public class TaskInstanceVariableValueFieldRewriter implements FieldRewriter {

    private static final TaskInstanceVariable TB = TaskInstanceVariable.TASK_INSTANCE_VARIABLE;

    private final DSLContext ctx;

    @Autowired
    public TaskInstanceVariableValueFieldRewriter(@Qualifier("job-execute-dsl-context") DSLContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public String tableName() {
        return TB.getName();
    }

    @Override
    public String fieldName() {
        return TB.VALUE.getName();
    }

    @Override
    public List<FieldBatchRow> fetchBatch(String lastProcessedPkCursor, int batchSize) {
        SelectConditionStep<Record2<Long, String>> query = ctx
            .select(TB.ID, TB.VALUE)
            .from(TB)
            .where(TB.TYPE.eq((byte) TaskVariableTypeEnum.CIPHER.getType()))
            .and(TB.VALUE.isNotNull());
        if (lastProcessedPkCursor != null) {
            query = query.and(TB.ID.lt(Long.parseLong(lastProcessedPkCursor)));
        }
        Result<Record2<Long, String>> records = query
            .orderBy(TB.ID.desc())
            .limit(batchSize)
            .fetch();

        List<FieldBatchRow> rows = new ArrayList<>(records.size());
        for (Record2<Long, String> r : records) {
            rows.add(new FieldBatchRow(String.valueOf(r.get(TB.ID)), r.get(TB.VALUE)));
        }
        return rows;
    }

    @Override
    public int updateRow(String pkCursor, String oldCipher, String newCipher) {
        long pk = Long.parseLong(pkCursor);
        return ctx.update(TB)
            .set(TB.VALUE, newCipher)
            .where(TB.ID.eq(pk))
            .and(TB.VALUE.eq(oldCipher))
            .execute();
    }

    @Override
    public long countRemaining() {
        // 与 fetchBatch 同样的过滤条件，统计本表本字段在主密钥指纹下需要迁移的总行数
        Long count = ctx.selectCount()
            .from(TB)
            .where(TB.TYPE.eq((byte) TaskVariableTypeEnum.CIPHER.getType()))
            .and(TB.VALUE.isNotNull())
            .fetchOne(0, Long.class);
        return count == null ? 0L : count;
    }
}
