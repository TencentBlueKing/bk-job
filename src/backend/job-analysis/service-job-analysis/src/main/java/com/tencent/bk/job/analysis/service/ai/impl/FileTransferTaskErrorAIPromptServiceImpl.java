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
import com.tencent.bk.job.analysis.service.ai.FileTransferTaskErrorAIPromptService;
import com.tencent.bk.job.analysis.service.ai.context.model.FileTaskContext;
import com.tencent.bk.job.common.config.BkConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 文件分发任务报错分析AI提示符服务
 */
@Slf4j
@Service
public class FileTransferTaskErrorAIPromptServiceImpl extends AIBasePromptService
    implements FileTransferTaskErrorAIPromptService {

    private final AITemplateVarService aiTemplateVarService;
    private final BkConfig bkConfig;
    private final AIMessageI18nService aiMessageI18nService;

    @Autowired
    public FileTransferTaskErrorAIPromptServiceImpl(AIPromptTemplateDAO aiPromptTemplateDAO,
                                                    AITemplateVarService aiTemplateVarService,
                                                    BkConfig bkConfig,
                                                    AIMessageI18nService aiMessageI18nService) {
        super(aiPromptTemplateDAO);
        this.aiTemplateVarService = aiTemplateVarService;
        this.bkConfig = bkConfig;
        this.aiMessageI18nService = aiMessageI18nService;
    }

    /**
     * 根据文件任务上下文获取文件分发任务报错分析AI提示符
     *
     * @param context 文件分发任务上下文
     * @return AI提示符
     */
    @Override
    public AIPromptDTO getPrompt(FileTaskContext context) {
        String templateCode = PromptTemplateCodeEnum.ANALYZE_FILE_TRANSFER_TASK_ERROR.name();
        AIPromptTemplateDTO promptTemplate = getPromptTemplate(templateCode);
        String renderedRawPrompt = renderPrompt(promptTemplate.getRawPrompt(), context);
        String renderedPrompt = renderPrompt(promptTemplate.getTemplate(), context);
        return new AIPromptDTO(promptTemplate.getId(), renderedRawPrompt, renderedPrompt);
    }

    /**
     * 渲染AI提示符
     *
     * @param promptTemplateContent AI提示符模板内容
     * @param context               文件任务上下文
     * @return 渲染后的AI提示符
     */
    private String renderPrompt(String promptTemplateContent, FileTaskContext context) {
        return promptTemplateContent
            .replace(aiTemplateVarService.getStepInstanceNamePlaceHolder(), context.getName())
            .replace(aiTemplateVarService.getBkHelperLinkPlaceHolder(), bkConfig.getBkHelperLink())
            .replace(
                aiTemplateVarService.getFileTaskErrorSourcePlaceHolder(),
                aiMessageI18nService.getI18nMessage(context.getFileTaskErrorSourceI18nKey())
            )
            .replace(aiTemplateVarService.getUploadFileErrorDataPlaceHolder(), context.getUploadFileErrorData())
            .replace(aiTemplateVarService.getDownloadFileErrorDataPlaceHolder(), context.getDownloadFileErrorData());
    }
}
