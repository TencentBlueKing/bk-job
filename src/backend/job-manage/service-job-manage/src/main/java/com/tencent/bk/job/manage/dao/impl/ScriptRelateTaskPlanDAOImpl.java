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

package com.tencent.bk.job.manage.dao.impl;

import com.tencent.bk.job.manage.common.consts.JobResourceStatusEnum;
import com.tencent.bk.job.manage.common.consts.task.TaskPlanTypeEnum;
import com.tencent.bk.job.manage.common.util.JooqDataTypeUtil;
import com.tencent.bk.job.manage.dao.ScriptRelateTaskPlanDAO;
import com.tencent.bk.job.manage.model.dto.ScriptRelatedTaskPlanDTO;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.conf.ParamType;
import org.jooq.generated.tables.ScriptVersion;
import org.jooq.generated.tables.TaskPlan;
import org.jooq.generated.tables.TaskPlanStepScript;
import org.jooq.impl.DSL;
import org.jooq.types.UByte;
import org.jooq.types.ULong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Repository
public class ScriptRelateTaskPlanDAOImpl implements ScriptRelateTaskPlanDAO {

    private DSLContext ctx;

    private TaskPlanStepScript T_TASK_PLAN_STEP_SCRIPT = TaskPlanStepScript.TASK_PLAN_STEP_SCRIPT;
    private ScriptVersion T_SCRIPT_VERSION = ScriptVersion.SCRIPT_VERSION;
    private TaskPlan T_TASK_PLAN = TaskPlan.TASK_PLAN;

    @Autowired
    public ScriptRelateTaskPlanDAOImpl(@Qualifier("job-manage-dsl-context") DSLContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public Integer countScriptRelatedTaskPlan(String scriptId) {
        val query = ctx.select(DSL.countDistinct(T_TASK_PLAN_STEP_SCRIPT.PLAN_ID))
            .from(T_TASK_PLAN_STEP_SCRIPT)
            .join(T_TASK_PLAN)
            .on(T_TASK_PLAN_STEP_SCRIPT.PLAN_ID.eq(T_TASK_PLAN.ID))
            .where(T_TASK_PLAN_STEP_SCRIPT.SCRIPT_ID.eq(scriptId))
            .and(T_TASK_PLAN.IS_DELETED.eq(UByte.valueOf(0)))
            .and(T_TASK_PLAN.TYPE.eq(UByte.valueOf(TaskPlanTypeEnum.NORMAL.getValue())));
        try {
            Result<Record1<Integer>> records = query.fetch();
            if (records != null) {
                return records.get(0).value1();
            }
        } catch (Exception e) {
            log.error(String.format("error SQL=%s", query.getSQL(ParamType.INLINED)), e);
        }
        return null;
    }

    @Override
    public Integer countScriptVersionRelatedTaskPlan(String scriptId, long scriptVersionId) {
        val query = ctx.select(DSL.countDistinct(T_TASK_PLAN_STEP_SCRIPT.PLAN_ID))
            .from(T_TASK_PLAN_STEP_SCRIPT)
            .join(T_TASK_PLAN)
            .on(T_TASK_PLAN_STEP_SCRIPT.PLAN_ID.eq(T_TASK_PLAN.ID))
            .where(T_TASK_PLAN_STEP_SCRIPT.SCRIPT_VERSION_ID.eq(JooqDataTypeUtil.buildULong(scriptVersionId)))
            .and(T_TASK_PLAN_STEP_SCRIPT.SCRIPT_ID.eq(scriptId))
            .and(T_TASK_PLAN.IS_DELETED.eq(UByte.valueOf(0)))
            .and(T_TASK_PLAN.TYPE.eq(UByte.valueOf(TaskPlanTypeEnum.NORMAL.getValue())));
        try {
            Result<Record1<Integer>> records = query.fetch();
            if (records != null) {
                return records.get(0).value1();
            }
        } catch (Exception e) {
            log.error(String.format("error SQL=%s", query.getSQL(ParamType.INLINED)), e);
        }
        return null;
    }

    private List<ScriptRelatedTaskPlanDTO> getDistinctRelatedPlans(List<ScriptRelatedTaskPlanDTO> srcRelatedPlans) {
        List<ScriptRelatedTaskPlanDTO> resultRelatedPlans = new ArrayList<>();
        Set<String> keySet = new HashSet<>();
        for (ScriptRelatedTaskPlanDTO relatedTaskPlanDTO : srcRelatedPlans) {
            String key =
                relatedTaskPlanDTO.getTaskId().toString() + ":" + relatedTaskPlanDTO.getScriptVersionId().toString();
            if (!keySet.contains(key)) {
                resultRelatedPlans.add(relatedTaskPlanDTO);
                keySet.add(key);
            }
        }
        return resultRelatedPlans;
    }

    @Override
    public List<ScriptRelatedTaskPlanDTO> listScriptRelatedTaskPlan(String scriptId) {
        Result result = ctx.select(
            T_TASK_PLAN_STEP_SCRIPT.SCRIPT_ID,
            T_TASK_PLAN_STEP_SCRIPT.SCRIPT_VERSION_ID,
            T_TASK_PLAN_STEP_SCRIPT.PLAN_ID,
            T_TASK_PLAN.APP_ID, T_TASK_PLAN.NAME,
            T_TASK_PLAN.TEMPLATE_ID,
            T_SCRIPT_VERSION.STATUS,
            T_SCRIPT_VERSION.VERSION
        ).from(T_TASK_PLAN_STEP_SCRIPT)
            .join(T_TASK_PLAN)
            .on(T_TASK_PLAN_STEP_SCRIPT.SCRIPT_ID.eq(scriptId))
            .join(T_SCRIPT_VERSION)
            .on(T_TASK_PLAN_STEP_SCRIPT.SCRIPT_VERSION_ID.eq(T_SCRIPT_VERSION.ID))
            .and(T_TASK_PLAN_STEP_SCRIPT.PLAN_ID.eq(T_TASK_PLAN.ID))
            .and(T_TASK_PLAN.IS_DELETED.eq(UByte.valueOf(0)))
            .and(T_TASK_PLAN.TYPE.eq(UByte.valueOf(TaskPlanTypeEnum.NORMAL.getValue())))
            .fetch();
        List<ScriptRelatedTaskPlanDTO> relatedPlans = new ArrayList<>();
        if (result.size() > 0) {
            result.into(record -> relatedPlans.add(extract(record)));
        }
        return getDistinctRelatedPlans(relatedPlans);
    }

    private ScriptRelatedTaskPlanDTO extract(Record record) {
        ScriptRelatedTaskPlanDTO taskPlan = new ScriptRelatedTaskPlanDTO();
        taskPlan.setAppId(record.get(T_TASK_PLAN.APP_ID).longValue());
        taskPlan.setTemplateId(record.get(T_TASK_PLAN.TEMPLATE_ID).longValue());
        taskPlan.setTaskId(record.get(T_TASK_PLAN_STEP_SCRIPT.PLAN_ID).longValue());
        taskPlan.setTaskName(record.get(T_TASK_PLAN.NAME));
        taskPlan.setScriptId(record.get(T_TASK_PLAN_STEP_SCRIPT.SCRIPT_ID));
        taskPlan.setScriptVersionId(record.get(T_TASK_PLAN_STEP_SCRIPT.SCRIPT_VERSION_ID).longValue());
        taskPlan.setScriptVersion(record.get(T_SCRIPT_VERSION.VERSION));
        taskPlan.setScriptStatus(JobResourceStatusEnum.getJobResourceStatus(JooqDataTypeUtil.getIntegerFromByte(record.get(T_SCRIPT_VERSION.STATUS).byteValue())));
        return taskPlan;
    }

    @Override
    public List<ScriptRelatedTaskPlanDTO> listScriptVersionRelatedTaskPlan(String scriptId, long scriptVersionId) {
        Result result = ctx.select(
            T_TASK_PLAN_STEP_SCRIPT.SCRIPT_ID,
            T_TASK_PLAN_STEP_SCRIPT.SCRIPT_VERSION_ID,
            T_TASK_PLAN_STEP_SCRIPT.PLAN_ID,
            T_TASK_PLAN.APP_ID, T_TASK_PLAN.NAME,
            T_TASK_PLAN.TEMPLATE_ID,
            T_SCRIPT_VERSION.STATUS,
            T_SCRIPT_VERSION.VERSION
        ).from(T_TASK_PLAN_STEP_SCRIPT)
            .join(T_TASK_PLAN)
            .on(T_TASK_PLAN_STEP_SCRIPT.SCRIPT_ID.eq(scriptId))
            .join(T_SCRIPT_VERSION)
            .on(T_TASK_PLAN_STEP_SCRIPT.SCRIPT_VERSION_ID.eq(T_SCRIPT_VERSION.ID))
            .and(T_TASK_PLAN_STEP_SCRIPT.SCRIPT_VERSION_ID.eq(ULong.valueOf(scriptVersionId)))
            .and(T_TASK_PLAN_STEP_SCRIPT.PLAN_ID.eq(T_TASK_PLAN.ID))
            .and(T_TASK_PLAN.IS_DELETED.eq(UByte.valueOf(0)))
            .and(T_TASK_PLAN.TYPE.eq(UByte.valueOf(TaskPlanTypeEnum.NORMAL.getValue())))
            .fetch();
        List<ScriptRelatedTaskPlanDTO> relatedPlans = new ArrayList<>();
        if (result.size() > 0) {
            result.into(record -> relatedPlans.add(extract(record)));
        }
        return getDistinctRelatedPlans(relatedPlans);
    }
}
