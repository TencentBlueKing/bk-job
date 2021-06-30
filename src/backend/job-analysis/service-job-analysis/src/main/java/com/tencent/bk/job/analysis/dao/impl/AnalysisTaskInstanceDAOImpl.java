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

import com.tencent.bk.job.analysis.dao.AnalysisTaskInstanceDAO;
import com.tencent.bk.job.analysis.model.dto.AnalysisTaskInstanceDTO;
import com.tencent.bk.job.analysis.model.dto.AnalysisTaskInstanceWithTpl;
import com.tencent.bk.job.analysis.task.analysis.AnalysisTaskStatusEnum;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import lombok.var;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record8;
import org.jooq.Result;
import org.jooq.conf.ParamType;
import org.jooq.generated.tables.AnalysisTask;
import org.jooq.generated.tables.AnalysisTaskInstance;
import org.jooq.generated.tables.records.AnalysisTaskInstanceRecord;
import org.jooq.impl.DSL;
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
public class AnalysisTaskInstanceDAOImpl implements AnalysisTaskInstanceDAO {

    private static final AnalysisTaskInstance defaultTable = AnalysisTaskInstance.ANALYSIS_TASK_INSTANCE;
    private static final AnalysisTask tableAnalysisTask = AnalysisTask.ANALYSIS_TASK;

    @Override
    public Long insertAnalysisTaskInstance(DSLContext dslContext, AnalysisTaskInstanceDTO analysisTaskInstanceDTO) {
        val query = dslContext.insertInto(defaultTable,
            defaultTable.ID,
            defaultTable.APP_ID,
            defaultTable.TASK_ID,
            defaultTable.STATUS,
            defaultTable.RESULT_DATA,
            defaultTable.PRIORITY,
            defaultTable.ACTIVE,
            defaultTable.CREATOR,
            defaultTable.CREATE_TIME,
            defaultTable.LAST_MODIFY_USER,
            defaultTable.LAST_MODIFY_TIME
        ).values(
            null,
            analysisTaskInstanceDTO.getAppId(),
            analysisTaskInstanceDTO.getTaskId(),
            analysisTaskInstanceDTO.getStatus(),
            analysisTaskInstanceDTO.getResultData(),
            analysisTaskInstanceDTO.getPriority(),
            analysisTaskInstanceDTO.isActive(),
            analysisTaskInstanceDTO.getCreator(),
            ULong.valueOf(analysisTaskInstanceDTO.getCreateTime()),
            analysisTaskInstanceDTO.getLastModifier(),
            ULong.valueOf(analysisTaskInstanceDTO.getLastModifyTime())
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
    public int updateAnalysisTaskInstanceById(DSLContext dslContext, AnalysisTaskInstanceDTO analysisTaskInstanceDTO) {
        val query = dslContext.update(defaultTable)
            .set(defaultTable.APP_ID, analysisTaskInstanceDTO.getAppId())
            .set(defaultTable.TASK_ID, analysisTaskInstanceDTO.getTaskId())
            .set(defaultTable.STATUS, analysisTaskInstanceDTO.getStatus())
            .set(defaultTable.RESULT_DATA, analysisTaskInstanceDTO.getResultData())
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
    public int deleteHistoryAnalysisTaskInstance(DSLContext dslContext, Long appId, Long taskId) {
        // 状态不为Running的全部删除
        int notRunningCount = dslContext.deleteFrom(defaultTable).where(
            defaultTable.APP_ID.eq(appId)
                .and(defaultTable.TASK_ID.eq(taskId))
                .and(defaultTable.STATUS.notEqual(AnalysisTaskStatusEnum.RUNNING.getValue()))
        ).execute();
        // 状态为Running的只留下最新的一个
        val record = dslContext.select(DSL.max(defaultTable.ID)).from(defaultTable).where(
            defaultTable.APP_ID.eq(appId)
                .and(defaultTable.TASK_ID.eq(taskId))
                .and(defaultTable.STATUS.eq(AnalysisTaskStatusEnum.RUNNING.getValue()))
        ).fetchOne();
        int runningCount = 0;
        if (record != null && record.size() > 0) {
            Long maxId = (Long) record.get(0);
            runningCount = dslContext.deleteFrom(defaultTable).where(
                defaultTable.ID.notEqual(maxId)
                    .and(defaultTable.APP_ID.eq(appId))
                    .and(defaultTable.TASK_ID.eq(taskId))
                    .and(defaultTable.STATUS.eq(AnalysisTaskStatusEnum.RUNNING.getValue()))
            ).execute();
        }
        return notRunningCount + runningCount;
    }

    @Override
    public int deleteAnalysisTaskInstanceById(DSLContext dslContext, Long id) {
        return dslContext.deleteFrom(defaultTable).where(
            defaultTable.ID.eq(id)
        ).execute();
    }

    @Override
    public AnalysisTaskInstanceDTO getAnalysisTaskInstanceById(DSLContext dslContext, Long id) {
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
    public List<AnalysisTaskInstanceWithTpl> listAllAnalysisTaskInstance(DSLContext dslContext) {
        return listAnalysisTaskInstanceWithConditions(dslContext, Collections.emptyList(), null, null);
    }

    @Override
    public List<AnalysisTaskInstanceWithTpl> listActiveAnalysisTaskInstance(DSLContext dslContext, Long appId,
                                                                            Long limit) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(defaultTable.STATUS.eq(AnalysisTaskStatusEnum.SUCCESS.getValue()));
        conditions.add(defaultTable.ACTIVE.eq(true));
        conditions.add(defaultTable.APP_ID.eq(appId));
        return listAnalysisTaskInstanceWithConditions(dslContext, conditions, 0L, limit);
    }

    @Override
    public List<AnalysisTaskInstanceWithTpl> listNewestActiveInstance(DSLContext dslContext, Long appId,
                                                                      Long limit) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(defaultTable.STATUS.eq(AnalysisTaskStatusEnum.SUCCESS.getValue()));
        conditions.add(defaultTable.ACTIVE.eq(true));
        conditions.add(defaultTable.APP_ID.eq(appId));
        //查出最新记录的时间
        var t = dslContext.select(
            defaultTable.TASK_ID.as("TASK_ID"),
            DSL.max(defaultTable.LAST_MODIFY_TIME).as("LAST_MODIFY_TIME")
        ).from(defaultTable).where(defaultTable.APP_ID.eq(appId).and(defaultTable.STATUS.eq(AnalysisTaskStatusEnum.SUCCESS.getValue()))).groupBy(defaultTable.TASK_ID).asTable("t");
        //把对应的任务也查出来
        var query = dslContext.select(
            defaultTable.ID,
            tableAnalysisTask.CODE,
            tableAnalysisTask.RESULT_DESCRIPTION_TEMPLATE,
            tableAnalysisTask.RESULT_DESCRIPTION_TEMPLATE_EN,
            tableAnalysisTask.RESULT_ITEM_TEMPLATE,
            tableAnalysisTask.RESULT_ITEM_TEMPLATE_EN,
            defaultTable.RESULT_DATA,
            defaultTable.PRIORITY
        ).from(defaultTable)
            .join(tableAnalysisTask)
            .on(defaultTable.TASK_ID.eq(tableAnalysisTask.ID))
            .join(t).on(defaultTable.TASK_ID.eq(t.field("TASK_ID", Long.class)).and(defaultTable.LAST_MODIFY_TIME.eq(t.field("LAST_MODIFY_TIME", ULong.class))))
            .where(
                conditions
                //默认按照优先级排序
            ).orderBy(defaultTable.PRIORITY);
        Result<Record8<Long, String, String, String, String, String, String, Integer>> records;
        val sql = query.getSQL(ParamType.INLINED);
        try {
            if (limit != null && limit > 0) {
                records = query.limit(limit).fetch();
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
            return records.map(this::convertToAnalysisTaskInstanceWithTpl);
        }
    }

    private List<AnalysisTaskInstanceWithTpl> listAnalysisTaskInstanceWithConditions(DSLContext dslContext,
                                                                                     Collection<Condition> conditions
        , Long offset, Long limit) {
        //把对应的任务也查出来
        var query = dslContext.select(
            defaultTable.ID,
            tableAnalysisTask.CODE,
            tableAnalysisTask.RESULT_DESCRIPTION_TEMPLATE,
            tableAnalysisTask.RESULT_DESCRIPTION_TEMPLATE_EN,
            tableAnalysisTask.RESULT_ITEM_TEMPLATE,
            tableAnalysisTask.RESULT_ITEM_TEMPLATE_EN,
            defaultTable.RESULT_DATA,
            defaultTable.PRIORITY
        ).from(defaultTable)
            .join(tableAnalysisTask)
            .on(defaultTable.TASK_ID.eq(tableAnalysisTask.ID))
            .where(
                conditions
                //默认按照优先级排序
            ).orderBy(defaultTable.PRIORITY);
        Result<Record8<Long, String, String, String, String, String, String, Integer>> records;
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
            return records.map(this::convertToAnalysisTaskInstanceWithTpl);
        }
    }

    private AnalysisTaskInstanceWithTpl convertToAnalysisTaskInstanceWithTpl(Record8<Long, String, String, String,
        String, String, String, Integer> record) {
        Long id = record.component1();
        String taskCode = record.component2();
        String resultDescriptionTemplate = record.component3();
        String resultDescriptionTemplateEn = record.component4();
        String resultItemTemplate = record.component5();
        String resultItemTemplateEn = record.component6();
        String resultData = record.component7();
        Integer priority = record.component8();
        return new AnalysisTaskInstanceWithTpl(
            id,
            taskCode,
            resultDescriptionTemplate,
            resultDescriptionTemplateEn,
            resultItemTemplate,
            resultItemTemplateEn,
            resultData,
            priority
        );
    }


    private AnalysisTaskInstanceDTO convert(AnalysisTaskInstanceRecord record) {
        return new AnalysisTaskInstanceDTO(
            record.getId(),
            record.getAppId(),
            record.getTaskId(),
            record.getStatus(),
            record.getResultData(),
            record.getPriority(),
            record.getActive(),
            record.getCreator(),
            record.getCreateTime().longValue(),
            record.getLastModifyUser(),
            record.getLastModifyTime().longValue()
        );
    }
}
