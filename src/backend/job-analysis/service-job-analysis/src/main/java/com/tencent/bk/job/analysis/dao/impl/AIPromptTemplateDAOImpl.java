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

import com.tencent.bk.job.analysis.dao.AIPromptTemplateDAO;
import com.tencent.bk.job.analysis.model.dto.AIPromptTemplateDTO;
import com.tencent.bk.job.analysis.model.tables.AiPromptTemplate;
import com.tencent.bk.job.common.mysql.dao.BaseDAOImpl;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;

@Slf4j
@Repository
public class AIPromptTemplateDAOImpl extends BaseDAOImpl implements AIPromptTemplateDAO {
    private static final AiPromptTemplate defaultTable = AiPromptTemplate.AI_PROMPT_TEMPLATE;

    private final DSLContext dslContext;

    public AIPromptTemplateDAOImpl(@Qualifier("job-analysis-dsl-context") DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    @Override
    public AIPromptTemplateDTO getAIPromptTemplate(String code, String locale) {
        Collection<Condition> conditions = new ArrayList<>();
        conditions.add(defaultTable.CODE.eq(code));
        conditions.add(defaultTable.LOCALE.eq(locale));
        val query = dslContext.select(
                defaultTable.ID,
                defaultTable.CODE,
                defaultTable.LOCALE,
                defaultTable.NAME,
                defaultTable.RAW_PROMPT,
                defaultTable.TEMPLATE
            )
            .from(defaultTable)
            .where(conditions);
        return fetchOne(query, this::convertRecordToDto);
    }

    private AIPromptTemplateDTO convertRecordToDto(Record record) {
        AIPromptTemplateDTO aiPromptTemplateDTO = new AIPromptTemplateDTO();
        aiPromptTemplateDTO.setId(record.get(defaultTable.ID));
        aiPromptTemplateDTO.setCode(record.get(defaultTable.CODE));
        aiPromptTemplateDTO.setLocale(record.get(defaultTable.LOCALE));
        aiPromptTemplateDTO.setName(record.get(defaultTable.NAME));
        aiPromptTemplateDTO.setRawPrompt(record.get(defaultTable.RAW_PROMPT));
        aiPromptTemplateDTO.setTemplate(record.get(defaultTable.TEMPLATE));
        return aiPromptTemplateDTO;
    }
}
