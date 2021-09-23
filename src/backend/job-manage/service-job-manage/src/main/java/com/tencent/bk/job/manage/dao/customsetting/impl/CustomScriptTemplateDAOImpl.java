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

package com.tencent.bk.job.manage.dao.customsetting.impl;

import com.tencent.bk.job.manage.common.util.JooqDataTypeUtil;
import com.tencent.bk.job.manage.dao.customsetting.CustomScriptTemplateDAO;
import com.tencent.bk.job.manage.model.dto.customsetting.ScriptTemplateDTO;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.generated.tables.UserCustomScriptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository
public class CustomScriptTemplateDAOImpl implements CustomScriptTemplateDAO {

    private final UserCustomScriptTemplate TB = UserCustomScriptTemplate.USER_CUSTOM_SCRIPT_TEMPLATE;
    private DSLContext ctx;

    @Autowired
    public CustomScriptTemplateDAOImpl(@Qualifier("job-manage-dsl-context") DSLContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public List<ScriptTemplateDTO> listCustomScriptTemplate(String username) {
        Result result = ctx.select(TB.USERNAME, TB.SCRIPT_LANGUAGE, TB.SCRIPT_CONTENT).from(TB)
            .where(TB.USERNAME.eq(username))
            .fetch();
        List<ScriptTemplateDTO> scriptTemplates = new ArrayList<>();
        if (result.size() != 0) {
            result.map(record -> {
                scriptTemplates.add(extractScriptTemplate(record));
                return null;
            });
        }
        return scriptTemplates;
    }

    private ScriptTemplateDTO extractScriptTemplate(Record record) {
        ScriptTemplateDTO scriptTemplate = new ScriptTemplateDTO();
        scriptTemplate.setScriptLanguage(record.get(TB.SCRIPT_LANGUAGE).intValue());
        scriptTemplate.setScriptContent(record.get(TB.SCRIPT_CONTENT, String.class));
        return scriptTemplate;
    }

    @Override
    public void saveScriptTemplate(String username, ScriptTemplateDTO scriptTemplate) {
        ctx.insertInto(TB, TB.USERNAME, TB.SCRIPT_LANGUAGE, TB.SCRIPT_CONTENT)
            .values(username, JooqDataTypeUtil.getByteFromInteger(scriptTemplate.getScriptLanguage()),
                scriptTemplate.getScriptContent())
            .onDuplicateKeyUpdate()
            .set(TB.SCRIPT_CONTENT, scriptTemplate.getScriptContent())
            .execute();
    }
}
