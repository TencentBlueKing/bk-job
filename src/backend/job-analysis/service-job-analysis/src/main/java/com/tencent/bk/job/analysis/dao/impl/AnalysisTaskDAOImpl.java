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

import com.tencent.bk.job.analysis.dao.AnalysisTaskDAO;
import com.tencent.bk.job.analysis.model.dto.AnalysisTaskDTO;
import com.tencent.bk.job.common.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import lombok.var;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.jooq.conf.ParamType;
import org.jooq.generated.tables.AnalysisTask;
import org.jooq.generated.tables.records.AnalysisTaskRecord;
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
public class AnalysisTaskDAOImpl implements AnalysisTaskDAO {

    private static final AnalysisTask defaultTable = AnalysisTask.ANALYSIS_TASK;

    @Override
    public Long insertAnalysisTask(DSLContext dslContext, AnalysisTaskDTO analysisTaskDTO) {
        String appIdsStr = StringUtil.listToStr(analysisTaskDTO.getAppIdList(), ",");
        val query = dslContext.insertInto(defaultTable,
            defaultTable.ID,
            defaultTable.CODE,
            defaultTable.APP_IDS,
            defaultTable.RESULT_DESCRIPTION_TEMPLATE,
            defaultTable.RESULT_DESCRIPTION_TEMPLATE_EN,
            defaultTable.RESULT_ITEM_TEMPLATE,
            defaultTable.RESULT_ITEM_TEMPLATE_EN,
            defaultTable.PRIORITY,
            defaultTable.ACTIVE,
            defaultTable.PERIOD_SECONDS,
            defaultTable.CREATOR,
            defaultTable.CREATE_TIME,
            defaultTable.LAST_MODIFY_USER,
            defaultTable.LAST_MODIFY_TIME
        ).values(
            null,
            analysisTaskDTO.getCode(),
            appIdsStr,
            analysisTaskDTO.getResultDescriptionTemplate(),
            analysisTaskDTO.getResultDescriptionTemplateEn(),
            analysisTaskDTO.getResultItemTemplate(),
            analysisTaskDTO.getResultItemTemplateEn(),
            analysisTaskDTO.getPriority(),
            analysisTaskDTO.isActive(),
            analysisTaskDTO.getPeriodSeconds(),
            analysisTaskDTO.getCreator(),
            ULong.valueOf(analysisTaskDTO.getCreateTime()),
            analysisTaskDTO.getLastModifier(),
            ULong.valueOf(analysisTaskDTO.getLastModifyTime())
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
    public int updateAnalysisTaskById(DSLContext dslContext, AnalysisTaskDTO analysisTaskDTO) {
        val query = dslContext.update(defaultTable)
            .set(defaultTable.CODE, analysisTaskDTO.getCode())
            .set(defaultTable.APP_IDS, StringUtil.listToStr(analysisTaskDTO.getAppIdList(), ","))
            .set(defaultTable.RESULT_DESCRIPTION_TEMPLATE, analysisTaskDTO.getResultDescriptionTemplate())
            .set(defaultTable.RESULT_DESCRIPTION_TEMPLATE_EN, analysisTaskDTO.getResultDescriptionTemplateEn())
            .set(defaultTable.RESULT_ITEM_TEMPLATE, analysisTaskDTO.getResultItemTemplate())
            .set(defaultTable.RESULT_ITEM_TEMPLATE_EN, analysisTaskDTO.getResultItemTemplateEn())
            .set(defaultTable.PRIORITY, analysisTaskDTO.getPriority())
            .set(defaultTable.ACTIVE, analysisTaskDTO.isActive())
            .set(defaultTable.PERIOD_SECONDS, analysisTaskDTO.getPeriodSeconds())
            .set(defaultTable.LAST_MODIFY_USER, analysisTaskDTO.getLastModifier())
            .set(defaultTable.LAST_MODIFY_TIME, ULong.valueOf(analysisTaskDTO.getLastModifyTime()))
            .where(defaultTable.ID.eq(analysisTaskDTO.getId()));
        val sql = query.getSQL(ParamType.INLINED);
        try {
            return query.execute();
        } catch (Exception e) {
            log.error(sql);
            throw e;
        }
    }

    @Override
    public int deleteAnalysisTaskById(DSLContext dslContext, Long id) {
        return dslContext.deleteFrom(defaultTable).where(
            defaultTable.ID.eq(id)
        ).execute();
    }

    @Override
    public AnalysisTaskDTO getAnalysisTaskById(DSLContext dslContext, Long id) {
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
    public AnalysisTaskDTO getAnalysisTaskByCode(DSLContext dslContext, String code) {
        val record = dslContext.selectFrom(defaultTable).where(
            defaultTable.CODE.eq(code)
        ).fetchOne();
        if (record == null) {
            return null;
        } else {
            return convert(record);
        }
    }

    @Override
    public List<AnalysisTaskDTO> listAllAnalysisTask(DSLContext dslContext) {
        return listAnalysisTaskWithConditions(dslContext, Collections.emptyList());
    }

    @Override
    public List<AnalysisTaskDTO> listActiveAnalysisTask(DSLContext dslContext) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(defaultTable.ACTIVE.eq(true));
        return listAnalysisTaskWithConditions(dslContext, conditions);
    }

    private List<AnalysisTaskDTO> listAnalysisTaskWithConditions(DSLContext dslContext,
                                                                 Collection<Condition> conditions) {
        var query = dslContext.selectFrom(defaultTable).where(
            conditions
            //默认按照优先级排序
        ).orderBy(defaultTable.PRIORITY);
        Result<AnalysisTaskRecord> records;
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

    private AnalysisTaskDTO convert(AnalysisTaskRecord record) {
        return new AnalysisTaskDTO(
            record.getId(),
            record.getCode(),
            StringUtil.strToList(record.getAppIds(), Long.class, ","),
            record.getResultDescriptionTemplate(),
            record.getResultDescriptionTemplateEn(),
            record.getResultItemTemplate(),
            record.getResultItemTemplateEn(),
            record.getPriority(),
            record.getActive(),
            record.getPeriodSeconds(),
            record.getCreator(),
            record.getCreateTime().longValue(),
            record.getLastModifyUser(),
            record.getLastModifyTime().longValue()
        );
    }

}
