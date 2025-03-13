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

package com.tencent.bk.job.analysis.service.ai.impl;

import com.tencent.bk.job.analysis.consts.PromptTemplateCodeEnum;
import com.tencent.bk.job.analysis.dao.AIPromptTemplateDAO;
import com.tencent.bk.job.analysis.model.dto.AIPromptDTO;
import com.tencent.bk.job.analysis.model.dto.AIPromptTemplateDTO;
import com.tencent.bk.job.analysis.service.ai.CheckScriptAIPromptService;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.manage.api.common.constants.script.ScriptTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 检查脚本的AI提示符服务实现类
 */
@Slf4j
@Service
public class CheckScriptAIPromptServiceImpl implements CheckScriptAIPromptService {

    private final ScriptTemplateService scriptTemplateService;
    private final AIPromptTemplateDAO aiPromptTemplateDAO;
    private final AITemplateVarService aiTemplateVarService;

    @Autowired
    public CheckScriptAIPromptServiceImpl(ScriptTemplateService scriptTemplateService,
                                          AIPromptTemplateDAO aiPromptTemplateDAO,
                                          AITemplateVarService aiTemplateVarService) {
        this.scriptTemplateService = scriptTemplateService;
        this.aiPromptTemplateDAO = aiPromptTemplateDAO;
        this.aiTemplateVarService = aiTemplateVarService;
    }

    /**
     * 根据脚本类型与内容获取检查脚本的AI提示符
     *
     * @param type          脚本类型
     * @param scriptContent 脚本内容
     * @return AI提示符
     */
    @Override
    public AIPromptDTO getPrompt(Integer type, String scriptContent) {
        String userLang = JobContextUtil.getUserLang();
        AIPromptTemplateDTO promptTemplate = aiPromptTemplateDAO.getAIPromptTemplate(
            PromptTemplateCodeEnum.CHECK_SCRIPT.name(),
            userLang
        );
        if (promptTemplate == null) {
            String message = "Cannot find prompt template for (code=CHECK_SCRIPT, userLang=" + userLang + "), " +
                "please check template config in DB";
            throw new InternalException(message, ErrorCode.INTERNAL_ERROR);
        }
        log.info(
            "Use prompt template [{}(id={})], userLang={}",
            promptTemplate.getName(),
            promptTemplate.getId(),
            userLang
        );
        String scriptTemplate = scriptTemplateService.getScriptTemplate(type);
        String renderedPrompt = renderCheckScriptPrompt(
            promptTemplate.getTemplate(),
            type,
            scriptTemplate,
            scriptContent
        );
        return new AIPromptDTO(promptTemplate.getId(), promptTemplate.getRawPrompt(), renderedPrompt);
    }

    /**
     * 渲染检查脚本的AI提示符
     *
     * @param promptTemplateContent 提示符模板内容
     * @param type                  脚本类型
     * @param scriptTemplate        脚本模板
     * @param scriptContent         脚本内容
     * @return 渲染后的提示符
     */
    private String renderCheckScriptPrompt(String promptTemplateContent,
                                           Integer type,
                                           String scriptTemplate,
                                           String scriptContent) {
        String scriptTypeName = ScriptTypeEnum.getName(type);
        promptTemplateContent = promptTemplateContent.replace(
            aiTemplateVarService.getScriptTypePlaceHolder(), scriptTypeName
        );
        if (scriptTemplate != null) {
            promptTemplateContent = promptTemplateContent.replace(
                aiTemplateVarService.getScriptTemplatePlaceHolder(), scriptTemplate
            );
        }
        promptTemplateContent = promptTemplateContent.replace(
            aiTemplateVarService.getScriptContentPlaceHolder(), scriptContent
        );
        return promptTemplateContent;
    }
}
