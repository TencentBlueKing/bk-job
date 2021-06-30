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
import com.tencent.bk.job.manage.common.util.JooqDataTypeUtil;
import com.tencent.bk.job.manage.dao.ScriptCitedTaskTemplateDAO;
import com.tencent.bk.job.manage.model.dto.script.ScriptCitedTaskTemplateDTO;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.conf.ParamType;
import org.jooq.generated.tables.ScriptVersion;
import org.jooq.generated.tables.TaskTemplate;
import org.jooq.generated.tables.TaskTemplateStepScript;
import org.jooq.impl.DSL;
import org.jooq.types.UByte;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Repository
public class ScriptCitedTaskTemplateDAOImpl implements ScriptCitedTaskTemplateDAO {

    private DSLContext ctx;

    private TaskTemplateStepScript T_TASK_TEMPLATE_STEP_SCRIPT = TaskTemplateStepScript.TASK_TEMPLATE_STEP_SCRIPT;
    private ScriptVersion T_SCRIPT_VERSION = ScriptVersion.SCRIPT_VERSION;
    private TaskTemplate T_TASK_TEMPLATE = TaskTemplate.TASK_TEMPLATE;

    @Autowired
    public ScriptCitedTaskTemplateDAOImpl(@Qualifier("job-manage-dsl-context") DSLContext ctx) {
        this.ctx = ctx;
    }

    private List<ScriptCitedTaskTemplateDTO> getDistinctRelatedTemplates(List<ScriptCitedTaskTemplateDTO> srcScriptCitedTaskTemplates) {
        List<ScriptCitedTaskTemplateDTO> resultRelatedTemplates = new ArrayList<>();
        Set<String> keySet = new HashSet<>();
        for (ScriptCitedTaskTemplateDTO citedTaskTemplateDTO : srcScriptCitedTaskTemplates) {
            String key =
                citedTaskTemplateDTO.getTaskTemplateId().toString() + ":" + citedTaskTemplateDTO.getScriptVersionId().toString();
            if (!keySet.contains(key)) {
                resultRelatedTemplates.add(citedTaskTemplateDTO);
                keySet.add(key);
            }
        }
        return resultRelatedTemplates;
    }

    private ScriptCitedTaskTemplateDTO extract(Record record) {
        ScriptCitedTaskTemplateDTO scriptCitedTaskTemplateDTO = new ScriptCitedTaskTemplateDTO();
        scriptCitedTaskTemplateDTO.setAppId(record.get(T_TASK_TEMPLATE.APP_ID).longValue());
        scriptCitedTaskTemplateDTO.setScriptVersionId(record.get(T_TASK_TEMPLATE_STEP_SCRIPT.SCRIPT_VERSION_ID).longValue());
        scriptCitedTaskTemplateDTO.setScriptVersion(record.get(T_SCRIPT_VERSION.VERSION));
        scriptCitedTaskTemplateDTO.setScriptStatus(JobResourceStatusEnum.getJobResourceStatus(JooqDataTypeUtil.getIntegerFromByte(record.get(T_SCRIPT_VERSION.STATUS).byteValue())));
        scriptCitedTaskTemplateDTO.setTaskTemplateId(record.get(T_TASK_TEMPLATE.ID).longValue());
        scriptCitedTaskTemplateDTO.setTaskTemplateName(record.get(T_TASK_TEMPLATE.NAME));
        return scriptCitedTaskTemplateDTO;
    }

    @Override
    public Integer countScriptCitedTaskTemplate(String scriptId) {
        val query = ctx.select(DSL.countDistinct(T_TASK_TEMPLATE_STEP_SCRIPT.TEMPLATE_ID))
            .from(T_TASK_TEMPLATE_STEP_SCRIPT)
            .join(T_TASK_TEMPLATE)
            .on(T_TASK_TEMPLATE_STEP_SCRIPT.TEMPLATE_ID.eq(T_TASK_TEMPLATE.ID))
            .where(T_TASK_TEMPLATE_STEP_SCRIPT.SCRIPT_ID.eq(scriptId))
            .and(T_TASK_TEMPLATE.IS_DELETED.eq(UByte.valueOf(0)));
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
    public Integer countScriptVersionCitedTaskTemplate(String scriptId, long scriptVersionId) {
        val query = ctx.select(DSL.countDistinct(T_TASK_TEMPLATE_STEP_SCRIPT.TEMPLATE_ID))
            .from(T_TASK_TEMPLATE_STEP_SCRIPT)
            .join(T_TASK_TEMPLATE)
            .on(T_TASK_TEMPLATE_STEP_SCRIPT.TEMPLATE_ID.eq(T_TASK_TEMPLATE.ID))
            .where(T_TASK_TEMPLATE_STEP_SCRIPT.SCRIPT_VERSION_ID.eq(JooqDataTypeUtil.buildULong(scriptVersionId)))
            .and(T_TASK_TEMPLATE_STEP_SCRIPT.SCRIPT_ID.eq(scriptId))
            .and(T_TASK_TEMPLATE.IS_DELETED.eq(UByte.valueOf(0)));
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
    public List<ScriptCitedTaskTemplateDTO> listScriptCitedTaskTemplate(String scriptId) {
        val query = ctx.select(
            T_TASK_TEMPLATE.APP_ID,
            T_TASK_TEMPLATE_STEP_SCRIPT.SCRIPT_VERSION_ID,
            T_SCRIPT_VERSION.VERSION,
            T_SCRIPT_VERSION.STATUS,
            T_TASK_TEMPLATE.ID,
            T_TASK_TEMPLATE.NAME
        ).from(T_TASK_TEMPLATE_STEP_SCRIPT)
            .join(T_TASK_TEMPLATE)
            .on(T_TASK_TEMPLATE_STEP_SCRIPT.TEMPLATE_ID.eq(T_TASK_TEMPLATE.ID))
            .join(T_SCRIPT_VERSION)
            .on(T_TASK_TEMPLATE_STEP_SCRIPT.SCRIPT_VERSION_ID.eq(T_SCRIPT_VERSION.ID))
            .where(T_TASK_TEMPLATE_STEP_SCRIPT.SCRIPT_ID.eq(scriptId))
            .and(T_TASK_TEMPLATE.IS_DELETED.eq(UByte.valueOf(0)));
        try {
            Result result = query.fetch();
            List<ScriptCitedTaskTemplateDTO> relatedTemplates = new ArrayList<>();
            if (result.size() > 0) {
                result.into(record -> relatedTemplates.add(extract(record)));
            }
            return getDistinctRelatedTemplates(relatedTemplates);
        } catch (Exception e) {
            log.error(String.format("error SQL=%s", query.getSQL(ParamType.INLINED)), e);
        }
        return null;
    }

    @Override
    public List<ScriptCitedTaskTemplateDTO> listScriptVersionCitedTaskTemplate(String scriptId, long scriptVersionId) {
        val query = ctx.select(
            T_TASK_TEMPLATE.APP_ID,
            T_TASK_TEMPLATE_STEP_SCRIPT.SCRIPT_VERSION_ID,
            T_SCRIPT_VERSION.VERSION,
            T_SCRIPT_VERSION.STATUS,
            T_TASK_TEMPLATE.ID,
            T_TASK_TEMPLATE.NAME
        ).from(T_TASK_TEMPLATE_STEP_SCRIPT)
            .join(T_TASK_TEMPLATE)
            .on(T_TASK_TEMPLATE_STEP_SCRIPT.TEMPLATE_ID.eq(T_TASK_TEMPLATE.ID))
            .join(T_SCRIPT_VERSION)
            .on(T_TASK_TEMPLATE_STEP_SCRIPT.SCRIPT_VERSION_ID.eq(T_SCRIPT_VERSION.ID))
            .where(T_TASK_TEMPLATE_STEP_SCRIPT.SCRIPT_ID.eq(scriptId))
            .and(T_TASK_TEMPLATE_STEP_SCRIPT.SCRIPT_VERSION_ID.eq(JooqDataTypeUtil.buildULong(scriptVersionId)))
            .and(T_TASK_TEMPLATE.IS_DELETED.eq(UByte.valueOf(0)));
        try {
            Result result = query.fetch();
            List<ScriptCitedTaskTemplateDTO> relatedTemplates = new ArrayList<>();
            if (result.size() > 0) {
                result.into(record -> relatedTemplates.add(extract(record)));
            }
            return getDistinctRelatedTemplates(relatedTemplates);
        } catch (Exception e) {
            log.error(String.format("error SQL=%s", query.getSQL(ParamType.INLINED)), e);
        }
        return null;
    }
}
