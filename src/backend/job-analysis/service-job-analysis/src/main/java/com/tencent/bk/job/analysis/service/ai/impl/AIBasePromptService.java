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

package com.tencent.bk.job.analysis.service.ai.impl;

import com.tencent.bk.job.analysis.dao.AIPromptTemplateDAO;
import com.tencent.bk.job.analysis.model.dto.AIPromptTemplateDTO;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.util.JobContextUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AIBasePromptService {

    private final AIPromptTemplateDAO aiPromptTemplateDAO;

    public AIBasePromptService(AIPromptTemplateDAO aiPromptTemplateDAO) {
        this.aiPromptTemplateDAO = aiPromptTemplateDAO;
    }

    protected AIPromptTemplateDTO getPromptTemplate(String templateCode) {
        String userLang = JobContextUtil.getUserLang();
        AIPromptTemplateDTO promptTemplate = aiPromptTemplateDAO.getAIPromptTemplate(
            templateCode,
            userLang
        );
        if (promptTemplate == null) {
            String message = "Cannot find prompt template for (" +
                "code=" + templateCode +
                ", userLang=" + userLang +
                "), please check template config in DB";
            throw new InternalException(message, ErrorCode.INTERNAL_ERROR);
        }
        log.info(
            "Use prompt template [{}(id={})], userLang={}",
            promptTemplate.getName(),
            promptTemplate.getId(),
            userLang
        );
        return promptTemplate;
    }

}
