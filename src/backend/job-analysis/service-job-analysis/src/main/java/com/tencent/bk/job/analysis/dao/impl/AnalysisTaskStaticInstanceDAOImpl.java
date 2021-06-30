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

package com.tencent.bk.job.analysis.dao.impl;

import com.tencent.bk.job.analysis.dao.AnalysisTaskStaticInstanceDAO;
import com.tencent.bk.job.analysis.model.dto.AnalysisTaskStaticInstanceDTO;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import lombok.var;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.jooq.conf.ParamType;
import org.jooq.generated.tables.AnalysisTask;
import org.jooq.generated.tables.AnalysisTaskStaticInstance;
import org.jooq.generated.tables.records.AnalysisTaskStaticInstanceRecord;
import org.jooq.types.ULong;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * @Description
 * @Date 2020/3/6
 * @Version 1.0
 */
@Repository
@Slf4j
public class AnalysisTaskStaticInstanceDAOImpl implements AnalysisTaskStaticInstanceDAO {

    private static final AnalysisTaskStaticInstance defaultTable =
        AnalysisTaskStaticInstance.ANALYSIS_TASK_STATIC_INSTANCE;
    private static final AnalysisTask tableAnalysisTask = AnalysisTask.ANALYSIS_TASK;

    @Override
    public Long insert(DSLContext dslContext,
                       AnalysisTaskStaticInstanceDTO analysisTaskStaticInstanceDTO) {
        val query = dslContext.insertInto(defaultTable,
            defaultTable.ID,
            defaultTable.APP_ID,
            defaultTable.TASK_ID,
            defaultTable.STATUS,
            defaultTable.RESULT_DATA,
            defaultTable.RESULT_DATA_EN,
            defaultTable.PRIORITY,
            defaultTable.ACTIVE,
            defaultTable.CREATOR,
            defaultTable.CREATE_TIME,
            defaultTable.LAST_MODIFY_USER,
            defaultTable.LAST_MODIFY_TIME
        ).values(
            null,
            analysisTaskStaticInstanceDTO.getAppId(),
            analysisTaskStaticInstanceDTO.getTaskId(),
            analysisTaskStaticInstanceDTO.getStatus(),
            analysisTaskStaticInstanceDTO.getResultData(),
            analysisTaskStaticInstanceDTO.getResultDataEn(),
            analysisTaskStaticInstanceDTO.getPriority(),
            analysisTaskStaticInstanceDTO.isActive(),
            analysisTaskStaticInstanceDTO.getCreator(),
            ULong.valueOf(analysisTaskStaticInstanceDTO.getCreateTime()),
            analysisTaskStaticInstanceDTO.getLastModifier(),
            ULong.valueOf(analysisTaskStaticInstanceDTO.getLastModifyTime())
        ).returning(defaultTable.ID);
        val sql = query.getSQL(ParamType.INLINED);
        try {
            return query.fetchOne().getId();
        } catch (Exception e) {
            log.error(sql);
            throw e;
        }
    }

    @Override
    public int updateById(DSLContext dslContext,
                          AnalysisTaskStaticInstanceDTO analysisTaskInstanceDTO) {
        val query = dslContext.update(defaultTable)
            .set(defaultTable.APP_ID, analysisTaskInstanceDTO.getAppId())
            .set(defaultTable.TASK_ID, analysisTaskInstanceDTO.getTaskId())
            .set(defaultTable.STATUS, analysisTaskInstanceDTO.getStatus())
            .set(defaultTable.RESULT_DATA, analysisTaskInstanceDTO.getResultData())
            .set(defaultTable.RESULT_DATA_EN, analysisTaskInstanceDTO.getResultDataEn())
            .set(defaultTable.PRIORITY, analysisTaskInstanceDTO.getPriority())
            .set(defaultTable.ACTIVE, analysisTaskInstanceDTO.isActive())
            .set(defaultTable.LAST_MODIFY_USER, analysisTaskInstanceDTO.getLastModifier())
            .set(defaultTable.LAST_MODIFY_TIME, ULong.valueOf(analysisTaskInstanceDTO.getLastModifyTime()))
            .where(defaultTable.ID.eq(analysisTaskInstanceDTO.getId()));
        val sql = query.getSQL(ParamType.INLINED);
        try {
            return query.execute();
        } catch (Exception e) {
            log.error(sql);
            throw e;
        }
    }


    @Override
    public List<AnalysisTaskStaticInstanceDTO> listActiveInstance(DSLContext dslContext,
                                                                  Long offset, Long limit) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(defaultTable.ACTIVE.eq(true));
        var query = dslContext.selectFrom(defaultTable)
            .where(
                conditions
                //默认按照优先级排序
            ).orderBy(defaultTable.PRIORITY);
        Result<AnalysisTaskStaticInstanceRecord> records;
        val sql = query.getSQL(ParamType.INLINED);
        try {
            if (offset != null && offset >= 0 && limit != null && limit > 0) {
                records = query.limit(offset, limit).fetch();
            } else {
                records = query.fetch();
            }
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

    @Override
    public int deleteById(DSLContext dslContext, Long id) {
        return dslContext.deleteFrom(defaultTable).where(
            defaultTable.ID.eq(id)
        ).execute();
    }

    @Override
    public AnalysisTaskStaticInstanceDTO getById(DSLContext dslContext, Long id) {
        val record = dslContext.selectFrom(defaultTable).where(
            defaultTable.ID.eq(id)
        ).fetchOne();
        if (record == null) {
            return null;
        } else {
            return convert(record);
        }
    }

    private AnalysisTaskStaticInstanceDTO convert(AnalysisTaskStaticInstanceRecord record) {
        return new AnalysisTaskStaticInstanceDTO(
            record.getId(),
            record.getAppId(),
            record.getTaskId(),
            record.getStatus(),
            record.getResultData(),
            record.getResultDataEn(),
            record.getPriority(),
            record.getActive(),
            record.getCreator(),
            record.getCreateTime().longValue(),
            record.getLastModifyUser(),
            record.getLastModifyTime().longValue()
        );
    }
}
