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

package com.tencent.bk.job.manage.passwordrotation;

import com.tencent.bk.job.common.constant.AccountCategoryEnum;
import com.tencent.bk.job.common.crypto.passwordrotation.FieldBatchRow;
import com.tencent.bk.job.common.crypto.passwordrotation.FieldRewriter;
import com.tencent.bk.job.manage.model.tables.Account;
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
 * account 表 db_password 字段密码轮换：仅处理 DB 类账号（category=2）。
 * 主键 id 是无符号自增 BIGINT，游标使用十进制字符串。
 */
@Slf4j
@Component
@Qualifier("jobManagePasswordRotationRewriter")
public class AccountDbPasswordFieldRewriter implements FieldRewriter {

    private static final Account TB_ACCOUNT = Account.ACCOUNT;

    private final DSLContext ctx;

    @Autowired
    public AccountDbPasswordFieldRewriter(@Qualifier("job-manage-dsl-context") DSLContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public String tableName() {
        return TB_ACCOUNT.getName();
    }

    @Override
    public String fieldName() {
        return TB_ACCOUNT.DB_PASSWORD.getName();
    }

    @Override
    public List<FieldBatchRow> fetchBatch(String lastProcessedPkCursor, int batchSize) {
        SelectConditionStep<Record2<Long, String>> query = ctx
            .select(TB_ACCOUNT.ID, TB_ACCOUNT.DB_PASSWORD)
            .from(TB_ACCOUNT)
            .where(TB_ACCOUNT.CATEGORY.eq(AccountCategoryEnum.DB.getValue().byteValue()))
            .and(TB_ACCOUNT.DB_PASSWORD.isNotNull());
        if (lastProcessedPkCursor != null) {
            query = query.and(TB_ACCOUNT.ID.lt(Long.parseLong(lastProcessedPkCursor)));
        }
        Result<Record2<Long, String>> records = query
            .orderBy(TB_ACCOUNT.ID.desc())
            .limit(batchSize)
            .fetch();

        List<FieldBatchRow> rows = new ArrayList<>(records.size());
        for (Record2<Long, String> r : records) {
            rows.add(new FieldBatchRow(String.valueOf(r.get(TB_ACCOUNT.ID)), r.get(TB_ACCOUNT.DB_PASSWORD)));
        }
        return rows;
    }

    @Override
    public int updateRow(String pkCursor, String oldCipher, String newCipher) {
        long pk = Long.parseLong(pkCursor);
        return ctx.update(TB_ACCOUNT)
            .set(TB_ACCOUNT.DB_PASSWORD, newCipher)
            .where(TB_ACCOUNT.ID.eq(pk))
            .and(TB_ACCOUNT.DB_PASSWORD.eq(oldCipher))
            .execute();
    }

    @Override
    public long countRemaining() {
        // 与 fetchBatch 同样的过滤条件，统计本表本字段在主密钥指纹下需要迁移的总行数
        Long count = ctx.selectCount()
            .from(TB_ACCOUNT)
            .where(TB_ACCOUNT.CATEGORY.eq(AccountCategoryEnum.DB.getValue().byteValue()))
            .and(TB_ACCOUNT.DB_PASSWORD.isNotNull())
            .fetchOne(0, Long.class);
        return count == null ? 0L : count;
    }
}
