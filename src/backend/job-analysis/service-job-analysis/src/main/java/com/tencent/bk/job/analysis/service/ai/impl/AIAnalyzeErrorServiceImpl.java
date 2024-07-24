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

import com.tencent.bk.job.analysis.model.dto.AIPromptDTO;
import com.tencent.bk.job.analysis.model.web.req.AIAnalyzeErrorReq;
import com.tencent.bk.job.analysis.model.web.resp.AIAnswer;
import com.tencent.bk.job.analysis.service.ai.AIAnalyzeErrorService;
import com.tencent.bk.job.analysis.service.ai.AIChatHistoryService;
import com.tencent.bk.job.analysis.service.ai.AIService;
import com.tencent.bk.job.analysis.service.ai.FileTransferTaskErrorAIPromptService;
import com.tencent.bk.job.analysis.service.ai.ScriptExecuteTaskErrorAIPromptService;
import com.tencent.bk.job.analysis.service.ai.context.model.TaskContext;
import com.tencent.bk.job.analysis.service.ai.context.TaskContextService;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InvalidParamException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AIAnalyzeErrorServiceImpl extends AIBaseService implements AIAnalyzeErrorService {

    private final TaskContextService taskContextService;
    private final ScriptExecuteTaskErrorAIPromptService scriptExecuteTaskErrorAIPromptService;
    private final FileTransferTaskErrorAIPromptService fileTransferTaskErrorAIPromptService;

    @Autowired
    public AIAnalyzeErrorServiceImpl(TaskContextService taskContextService,
                                     ScriptExecuteTaskErrorAIPromptService scriptExecuteTaskErrorAIPromptService,
                                     FileTransferTaskErrorAIPromptService fileTransferTaskErrorAIPromptService,
                                     AIService aiService,
                                     AIChatHistoryService aiChatHistoryService) {
        super(aiService, aiChatHistoryService);
        this.taskContextService = taskContextService;
        this.scriptExecuteTaskErrorAIPromptService = scriptExecuteTaskErrorAIPromptService;
        this.fileTransferTaskErrorAIPromptService = fileTransferTaskErrorAIPromptService;
    }

    @Override
    public AIAnswer analyze(String username, Long appId, AIAnalyzeErrorReq req) {
        TaskContext taskContext = taskContextService.getTaskContext(
            username,
            appId,
            req.getStepInstanceId()
        );
        String errorContent = req.getContent();
        AIPromptDTO aiPromptDTO;
        if (taskContext.isScriptTask()) {
            aiPromptDTO = scriptExecuteTaskErrorAIPromptService.getPrompt(
                taskContext.getScriptTaskContext(),
                errorContent
            );
        } else if (taskContext.isFileTask()) {
            aiPromptDTO = fileTransferTaskErrorAIPromptService.getPrompt(
                taskContext.getFileTaskContext(),
                errorContent
            );
        } else {
            throw new InvalidParamException(ErrorCode.AI_ANALYZE_ERROR_ONLY_SUPPORT_SCRIPT_OR_FILE_STEP);
        }
        return getAIAnswer(username, aiPromptDTO);
    }
}
