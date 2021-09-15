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

import com.tencent.bk.job.manage.dao.ScriptRelateJobTemplateDAO;
import com.tencent.bk.job.manage.model.dto.ScriptSyncTemplateStepDTO;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.Table;
import org.jooq.generated.tables.TaskTemplate;
import org.jooq.generated.tables.TaskTemplateStep;
import org.jooq.generated.tables.TaskTemplateStepScript;
import org.jooq.types.UByte;
import org.jooq.types.ULong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository
public class ScriptRelateJobTemplateDAOImpl implements ScriptRelateJobTemplateDAO {
    private DSLContext ctx;

    @Autowired
    public ScriptRelateJobTemplateDAOImpl(@Qualifier("job-manage-dsl-context") DSLContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public List<ScriptSyncTemplateStepDTO> listScriptRelatedJobTemplateSteps(String scriptId) {
        TaskTemplateStepScript T_SCRIPT_STEP = TaskTemplateStepScript.TASK_TEMPLATE_STEP_SCRIPT.as("t1");
        TaskTemplateStep T_TASK_TEMPLATE_STEP = TaskTemplateStep.TASK_TEMPLATE_STEP.as("t3");
        TaskTemplate T_TASK_TEMPLATE = TaskTemplate.TASK_TEMPLATE.as("t4");

        Table<?> NESTED_T_TASK_TEMPLATE_STEP_SCRIPT = ctx.select(T_SCRIPT_STEP.TEMPLATE_ID, T_SCRIPT_STEP.STEP_ID,
            T_SCRIPT_STEP.SCRIPT_ID, T_SCRIPT_STEP.SCRIPT_VERSION_ID).from(T_SCRIPT_STEP)
            .where(T_SCRIPT_STEP.SCRIPT_ID.eq(scriptId))
            .and(T_SCRIPT_STEP.TEMPLATE_ID.isNotNull())
            .asTable("t2");
        Result result = ctx.select(NESTED_T_TASK_TEMPLATE_STEP_SCRIPT.fields())
            .select(T_TASK_TEMPLATE_STEP.NAME.as("step_name"), T_TASK_TEMPLATE.NAME.as("template_name"),
                T_TASK_TEMPLATE.APP_ID)
            .from(NESTED_T_TASK_TEMPLATE_STEP_SCRIPT)
            .join(T_TASK_TEMPLATE_STEP).on(T_TASK_TEMPLATE_STEP.ID.eq(NESTED_T_TASK_TEMPLATE_STEP_SCRIPT.field(
                "step_id", ULong.class)))
            .join(T_TASK_TEMPLATE).on(T_TASK_TEMPLATE.ID.eq(NESTED_T_TASK_TEMPLATE_STEP_SCRIPT.field("template_id",
                ULong.class)))
            .where(T_TASK_TEMPLATE_STEP.IS_DELETED.eq(UByte.valueOf(0)))
            .and(T_TASK_TEMPLATE.IS_DELETED.eq(UByte.valueOf(0)))
            .orderBy(NESTED_T_TASK_TEMPLATE_STEP_SCRIPT.field("template_id").desc(),
                NESTED_T_TASK_TEMPLATE_STEP_SCRIPT.field("step_id").desc())
            .fetch();

        List<ScriptSyncTemplateStepDTO> templateSteps = new ArrayList<>();
        result.into(record -> {
            ScriptSyncTemplateStepDTO templateStep = extract(record);
            if (templateStep != null) {
                templateSteps.add(templateStep);
            }
        });
        return templateSteps;
    }

    private ScriptSyncTemplateStepDTO extract(Record record) {
        if (record == null) {
            return null;
        }
        ScriptSyncTemplateStepDTO templateStep = new ScriptSyncTemplateStepDTO();
        templateStep.setScriptId(record.get(TaskTemplateStepScript.TASK_TEMPLATE_STEP_SCRIPT.SCRIPT_ID));
        templateStep.setScriptVersionId(record.get(TaskTemplateStepScript.TASK_TEMPLATE_STEP_SCRIPT.SCRIPT_VERSION_ID).longValue());
        templateStep.setTemplateId(record.get(TaskTemplateStepScript.TASK_TEMPLATE_STEP_SCRIPT.TEMPLATE_ID).longValue());
        templateStep.setStepId(record.get(TaskTemplateStepScript.TASK_TEMPLATE_STEP_SCRIPT.STEP_ID).longValue());
        templateStep.setStepName(record.get("step_name", String.class));
        templateStep.setTemplateName(record.get("template_name", String.class));
        templateStep.setAppId(record.get(TaskTemplate.TASK_TEMPLATE.APP_ID).longValue());
        return templateStep;
    }
}
