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

package com.tencent.bk.job.backup.passwordrotation;

import com.tencent.bk.job.backup.model.tables.ExportJob;
import com.tencent.bk.job.common.crypto.passwordrotation.FieldBatchRow;
import com.tencent.bk.job.common.crypto.passwordrotation.FieldRewriter;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Record2;
import org.jooq.Result;
import org.jooq.SelectConditionStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * export_job 表 password 字段密码轮换。
 * 主键为复合主键 (id, app_id)，id 为 UUID(varchar 36)，已足够唯一标识一行；游标使用 id 字典序。
 */
@Slf4j
@Component
@Qualifier("jobBackupPasswordRotationRewriter")
public class ExportJobPasswordFieldRewriter implements FieldRewriter {

    private static final ExportJob TB_EXPORT_JOB = ExportJob.EXPORT_JOB;

    private final DSLContext ctx;

    @Autowired
    public ExportJobPasswordFieldRewriter(@Qualifier("job-backup-dsl-context") DSLContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public String tableName() {
        return TB_EXPORT_JOB.getName();
    }

    @Override
    public String fieldName() {
        return TB_EXPORT_JOB.PASSWORD.getName();
    }

    @Override
    public List<FieldBatchRow> fetchBatch(String lastProcessedPkCursor, int batchSize) {
        SelectConditionStep<Record2<String, String>> query = ctx
            .select(TB_EXPORT_JOB.ID, TB_EXPORT_JOB.PASSWORD)
            .from(TB_EXPORT_JOB)
            .where(TB_EXPORT_JOB.PASSWORD.isNotNull());
        if (lastProcessedPkCursor != null) {
            query = query.and(TB_EXPORT_JOB.ID.lt(lastProcessedPkCursor));
        }
        Result<Record2<String, String>> records = query
            .orderBy(TB_EXPORT_JOB.ID.desc())
            .limit(batchSize)
            .fetch();

        List<FieldBatchRow> rows = new ArrayList<>(records.size());
        for (Record2<String, String> r : records) {
            rows.add(new FieldBatchRow(r.get(TB_EXPORT_JOB.ID), r.get(TB_EXPORT_JOB.PASSWORD)));
        }
        return rows;
    }

    @Override
    public long countRemaining() {
        // 与 fetchBatch 同样的过滤条件，统计本表本字段在主密钥指纹下需要迁移的总行数
        Long count = ctx.selectCount()
            .from(TB_EXPORT_JOB)
            .where(TB_EXPORT_JOB.PASSWORD.isNotNull())
            .fetchOne(0, Long.class);
        return count == null ? 0L : count;
    }

    @Override
    public int updateRow(String pkCursor, String oldCipher, String newCipher) {
        return ctx.update(TB_EXPORT_JOB)
            .set(TB_EXPORT_JOB.PASSWORD, newCipher)
            .where(TB_EXPORT_JOB.ID.eq(pkCursor))
            .and(TB_EXPORT_JOB.PASSWORD.eq(oldCipher))
            .execute();
    }
}
