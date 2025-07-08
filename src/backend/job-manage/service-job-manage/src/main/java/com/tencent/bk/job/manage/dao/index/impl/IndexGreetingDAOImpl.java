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

package com.tencent.bk.job.manage.dao.index.impl;

import com.tencent.bk.job.manage.dao.index.IndexGreetingDAO;
import com.tencent.bk.job.manage.model.dto.index.IndexGreetingDTO;
import com.tencent.bk.job.manage.model.tables.IndexGreeting;
import com.tencent.bk.job.manage.model.tables.records.IndexGreetingRecord;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import lombok.var;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.jooq.conf.ParamType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Repository
@Slf4j
public class IndexGreetingDAOImpl implements IndexGreetingDAO {

    private static final IndexGreeting defaultTable = IndexGreeting.INDEX_GREETING;

    private final DSLContext dslContext;

    @Autowired
    public IndexGreetingDAOImpl(@Qualifier("job-manage-dsl-context") DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    @Override
    public List<IndexGreetingDTO> listActiveIndexGreeting(int currentTimeSeconds) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(defaultTable.ACTIVE.eq(true));
        conditions.add(defaultTable.START_SECONDS.lessOrEqual(currentTimeSeconds));
        conditions.add(defaultTable.END_SECONDS.greaterOrEqual(currentTimeSeconds));
        return listIndexGreetingWithConditions(conditions);
    }

    private List<IndexGreetingDTO> listIndexGreetingWithConditions(Collection<Condition> conditions) {
        var query = dslContext.selectFrom(defaultTable).where(
            conditions
            //默认按照优先级排序
        ).orderBy(defaultTable.PRIORITY);
        Result<IndexGreetingRecord> records;
        val sql = query.getSQL(ParamType.INLINED);
        try {
            records = query.fetch();
        } catch (Exception e) {
            log.error(sql);
            throw e;
        }
        if (records.isEmpty()) {
            return Collections.emptyList();
        } else {
            return records.map(this::convert);
        }
    }

    private IndexGreetingDTO convert(IndexGreetingRecord record) {
        return new IndexGreetingDTO(
            record.getId(),
            record.getStartSeconds(),
            record.getEndSeconds(),
            record.getContent(),
            record.getContentEn(),
            record.getPriority(),
            record.getActive(),
            record.getCreator(),
            record.getCreateTime().longValue(),
            record.getLastModifyUser(),
            record.getLastModifyTime().longValue()
        );
    }
}
