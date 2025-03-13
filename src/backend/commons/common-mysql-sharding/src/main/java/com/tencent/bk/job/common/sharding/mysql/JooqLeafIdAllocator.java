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

package com.tencent.bk.job.common.sharding.mysql;

import com.tencent.bk.job.common.sharding.mysql.jooq.model.tables.TLeafAlloc;
import com.tencent.bk.job.common.sharding.mysql.jooq.model.tables.records.TLeafAllocRecord;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.devops.leaf.segment.dao.IDAllocDao;
import com.tencent.devops.leaf.segment.model.LeafAlloc;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.impl.DSL;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于 JOOQ+美团 Leaf分布式 ID 组件，生成分布式 ID
 */
public class JooqLeafIdAllocator implements IDAllocDao {
    private final DSLContext dslContext;

    private static final TLeafAlloc T_LEAF_ALLOC = TLeafAlloc.T_LEAF_ALLOC;


    public JooqLeafIdAllocator(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    @Override
    public List<LeafAlloc> getAllLeafAllocs() {
        Result<TLeafAllocRecord> leafAllocRecords = dslContext.selectFrom(T_LEAF_ALLOC).fetch();
        List<LeafAlloc> leafAllocs = new ArrayList<>();
        leafAllocRecords.forEach(leafAllocRecord -> {
            LeafAlloc leafAlloc = generateLeafAlloc(leafAllocRecord);
            leafAllocs.add(leafAlloc);
        });
        return leafAllocs;
    }

    private LeafAlloc generateLeafAlloc(TLeafAllocRecord leafAllocRecord) {
        LeafAlloc leafAlloc = new LeafAlloc();
        leafAlloc.setKey(leafAllocRecord.getBizTag());
        leafAlloc.setMaxId(leafAllocRecord.getMaxId());
        leafAlloc.setStep(leafAllocRecord.getStep());
        leafAlloc.setUpdateTime(DateUtils.defaultLocalDateTime(leafAllocRecord.getUpdateTime()));
        return leafAlloc;
    }

    @Override
    public LeafAlloc updateMaxIdAndGetLeafAlloc(String tag) {
        TLeafAllocRecord leafAllocRecord;
        leafAllocRecord = dslContext.transactionResult(t -> {
            DSLContext context = DSL.using(t);
            context.update(T_LEAF_ALLOC)
                .set(T_LEAF_ALLOC.MAX_ID, T_LEAF_ALLOC.MAX_ID.add(T_LEAF_ALLOC.STEP))
                .set(T_LEAF_ALLOC.UPDATE_TIME, LocalDateTime.now())
                .where(T_LEAF_ALLOC.BIZ_TAG.eq(tag))
                .execute();
            return context.selectFrom(T_LEAF_ALLOC)
                .where(T_LEAF_ALLOC.BIZ_TAG
                    .eq(tag))
                .fetchOne();
        });
        return generateLeafAlloc(leafAllocRecord);
    }

    @Override
    public LeafAlloc updateMaxIdByCustomStepAndGetLeafAlloc(LeafAlloc leafAlloc) {
        TLeafAllocRecord leafAllocRecord;
        leafAllocRecord = dslContext.transactionResult(t -> {
            DSLContext context = DSL.using(t);
            context.update(T_LEAF_ALLOC)
                .set(T_LEAF_ALLOC.MAX_ID, T_LEAF_ALLOC.MAX_ID.add(leafAlloc.getStep()))
                .set(T_LEAF_ALLOC.UPDATE_TIME, LocalDateTime.now())
                .where(T_LEAF_ALLOC.BIZ_TAG.eq(leafAlloc.getKey()))
                .execute();
            return context.selectFrom(T_LEAF_ALLOC)
                .where(T_LEAF_ALLOC.BIZ_TAG.eq(leafAlloc.getKey()))
                .fetchOne();
        });
        return generateLeafAlloc(leafAllocRecord);
    }

    @Override
    public List<String> getAllTags() {
        Result<Record1<String>> tagRecords = dslContext
            .select(T_LEAF_ALLOC.BIZ_TAG)
            .from(T_LEAF_ALLOC)
            .fetch();
        List<String> tags = new ArrayList<>();
        tagRecords.forEach(record -> tags.add(record.value1()));
        return tags;
    }
}
