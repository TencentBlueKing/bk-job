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

package com.tencent.bk.job.manage.dao.index.impl;

import com.tencent.bk.job.manage.dao.index.IndexGreetingDAO;
import com.tencent.bk.job.manage.model.dto.index.IndexGreetingDTO;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import lombok.var;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.jooq.conf.ParamType;
import org.jooq.generated.tables.IndexGreeting;
import org.jooq.generated.tables.records.IndexGreetingRecord;
import org.jooq.types.ULong;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @Description
 * @Date 2020/3/6
 * @Version 1.0
 */
@Repository
@Slf4j
public class IndexGreetingDAOImpl implements IndexGreetingDAO {

    private static final IndexGreeting defaultTable = IndexGreeting.INDEX_GREETING;

    @Override
    public int insertIndexGreeting(DSLContext dslContext, IndexGreetingDTO indexGreetingDTO) {
        val query = dslContext.insertInto(defaultTable,
            defaultTable.ID,
            defaultTable.START_SECONDS,
            defaultTable.END_SECONDS,
            defaultTable.CONTENT,
            defaultTable.PRIORITY,
            defaultTable.ACTIVE,
            defaultTable.CREATOR,
            defaultTable.CREATE_TIME,
            defaultTable.LAST_MODIFY_USER,
            defaultTable.LAST_MODIFY_TIME
        ).values(
            null,
            indexGreetingDTO.getStartSeconds(),
            indexGreetingDTO.getEndSeconds(),
            indexGreetingDTO.getContent(),
            indexGreetingDTO.getPriority(),
            indexGreetingDTO.isActive(),
            indexGreetingDTO.getCreator(),
            ULong.valueOf(indexGreetingDTO.getCreateTime()),
            indexGreetingDTO.getLastModifier(),
            ULong.valueOf(indexGreetingDTO.getLastModifyTime())
        );
        val sql = query.getSQL(ParamType.INLINED);
        try {
            return query.execute();
        } catch (Exception e) {
            log.error(sql);
            throw e;
        }
    }

    @Override
    public int updateIndexGreetingById(DSLContext dslContext, IndexGreetingDTO indexGreetingDTO) {
        val query = dslContext.update(defaultTable)
            .set(defaultTable.START_SECONDS, indexGreetingDTO.getStartSeconds())
            .set(defaultTable.END_SECONDS, indexGreetingDTO.getEndSeconds())
            .set(defaultTable.CONTENT, indexGreetingDTO.getContent())
            .set(defaultTable.PRIORITY, indexGreetingDTO.getEndSeconds())
            .set(defaultTable.ACTIVE, indexGreetingDTO.isActive())
            .set(defaultTable.LAST_MODIFY_USER, indexGreetingDTO.getLastModifier())
            .set(defaultTable.LAST_MODIFY_TIME, ULong.valueOf(indexGreetingDTO.getLastModifyTime()))
            .where(defaultTable.ID.eq(indexGreetingDTO.getId()));
        val sql = query.getSQL(ParamType.INLINED);
        try {
            return query.execute();
        } catch (Exception e) {
            log.error(sql);
            throw e;
        }
    }

    @Override
    public int deleteIndexGreetingById(DSLContext dslContext, Long id) {
        return dslContext.deleteFrom(defaultTable).where(
            defaultTable.ID.eq(id)
        ).execute();
    }

    @Override
    public IndexGreetingDTO getIndexGreetingById(DSLContext dslContext, Long id) {
        val record = dslContext.selectFrom(defaultTable).where(
            defaultTable.ID.eq(id)
        ).fetchOne();
        if (record == null) {
            return null;
        } else {
            return convert(record);
        }
    }

    @Override
    public List<IndexGreetingDTO> listAllIndexGreeting(DSLContext dslContext) {
        return listIndexGreetingWithConditions(dslContext, Collections.emptyList());
    }

    @Override
    public List<IndexGreetingDTO> listActiveIndexGreeting(DSLContext dslContext, int currentTimeSeconds) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(defaultTable.ACTIVE.eq(true));
        conditions.add(defaultTable.START_SECONDS.lessOrEqual(currentTimeSeconds));
        conditions.add(defaultTable.END_SECONDS.greaterOrEqual(currentTimeSeconds));
        return listIndexGreetingWithConditions(dslContext, conditions);
    }

    private List<IndexGreetingDTO> listIndexGreetingWithConditions(DSLContext dslContext,
                                                                   Collection<Condition> conditions) {
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
        if (records == null || records.isEmpty()) {
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
