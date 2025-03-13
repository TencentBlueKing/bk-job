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

import com.tencent.bk.job.analysis.model.dto.AIAnalyzeErrorContextDTO;
import com.tencent.bk.job.analysis.model.dto.AIPromptDTO;
import com.tencent.bk.job.analysis.model.web.req.AIAnalyzeErrorReq;
import com.tencent.bk.job.analysis.model.web.resp.AIChatRecord;
import com.tencent.bk.job.analysis.service.ai.AIAnalyzeErrorService;
import com.tencent.bk.job.analysis.service.ai.AIChatHistoryService;
import com.tencent.bk.job.analysis.service.ai.FileTransferTaskErrorAIPromptService;
import com.tencent.bk.job.analysis.service.ai.ScriptExecuteTaskErrorAIPromptService;
import com.tencent.bk.job.analysis.service.ai.context.TaskContextService;
import com.tencent.bk.job.analysis.service.ai.context.model.TaskContext;
import com.tencent.bk.job.analysis.service.ai.context.model.TaskContextQuery;
import com.tencent.bk.job.common.config.BkConfig;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InvalidParamException;
import lombok.extern.slf4j.Slf4j;
import org.jooq.tools.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 通过AI分析任务报错信息的服务实现类
 */
@Slf4j
@Service
public class AIAnalyzeErrorServiceImpl extends AIBaseService implements AIAnalyzeErrorService {

    private final TaskContextService taskContextService;
    private final ScriptExecuteTaskErrorAIPromptService scriptExecuteTaskErrorAIPromptService;
    private final FileTransferTaskErrorAIPromptService fileTransferTaskErrorAIPromptService;
    private final AIMessageI18nService aiMessageI18nService;
    private final AITemplateVarService aiTemplateVarService;
    private final BkConfig bkConfig;

    @Autowired
    public AIAnalyzeErrorServiceImpl(TaskContextService taskContextService,
                                     ScriptExecuteTaskErrorAIPromptService scriptExecuteTaskErrorAIPromptService,
                                     FileTransferTaskErrorAIPromptService fileTransferTaskErrorAIPromptService,
                                     AIChatHistoryService aiChatHistoryService,
                                     AIMessageI18nService aiMessageI18nService,
                                     AITemplateVarService aiTemplateVarService,
                                     BkConfig bkConfig) {
        super(aiChatHistoryService);
        this.taskContextService = taskContextService;
        this.scriptExecuteTaskErrorAIPromptService = scriptExecuteTaskErrorAIPromptService;
        this.fileTransferTaskErrorAIPromptService = fileTransferTaskErrorAIPromptService;
        this.aiMessageI18nService = aiMessageI18nService;
        this.aiTemplateVarService = aiTemplateVarService;
        this.bkConfig = bkConfig;
    }

    /**
     * 通过AI分析任务报错信息，并记录对话历史
     *
     * @param username 用户名
     * @param appId    Job业务ID
     * @param req      请求内容
     * @return AI对话记录
     */
    @Override
    public AIChatRecord analyze(String username, Long appId, AIAnalyzeErrorReq req) {
        TaskContextQuery contextQuery = TaskContextQuery.fromAIAnalyzeErrorReq(appId, req);
        TaskContext taskContext = taskContextService.getTaskContext(username, contextQuery);
        if (taskContext.isScriptTask()) {
            return analyzeScriptTask(username, appId, taskContext, req);
        } else if (taskContext.isFileTask()) {
            return analyzeFileTask(username, appId, taskContext, req);
        } else {
            throw new InvalidParamException(ErrorCode.AI_ANALYZE_ERROR_ONLY_SUPPORT_SCRIPT_OR_FILE_STEP);
        }
    }

    /**
     * 分析脚本执行任务报错信息
     *
     * @param username    用户名
     * @param appId       Job业务ID
     * @param taskContext 任务上下文
     * @param req         请求体
     * @return AI对话记录
     */
    private AIChatRecord analyzeScriptTask(String username,
                                           Long appId,
                                           TaskContext taskContext,
                                           AIAnalyzeErrorReq req) {
        String errorContent = req.getContent();
        AIPromptDTO aiPromptDTO = scriptExecuteTaskErrorAIPromptService.getPrompt(
            taskContext.getScriptTaskContext(),
            errorContent
        );
        if (!taskContext.isTaskFail()) {
            return getDirectlyAIChatRecord(
                username,
                appId,
                aiPromptDTO,
                aiMessageI18nService.getNotFailTaskAIAnswerMessage()
            );
        }
        if (StringUtils.isEmpty(errorContent)) {
            return getDirectlyAIChatRecord(
                username,
                appId,
                aiPromptDTO,
                getRenderedEmptyLogTaskAIAnswerMessage()
            );
        }
        AIAnalyzeErrorContextDTO analyzeErrorContext = AIAnalyzeErrorContextDTO.fromAIAnalyzeErrorReq(req);
        return getAIChatRecord(username, appId, aiPromptDTO, analyzeErrorContext);
    }

    /**
     * 分析文件分发任务报错信息
     *
     * @param username    用户名
     * @param appId       Job业务ID
     * @param taskContext 任务上下文
     * @param req         请求体
     * @return AI对话记录
     */
    private AIChatRecord analyzeFileTask(String username,
                                         Long appId,
                                         TaskContext taskContext,
                                         AIAnalyzeErrorReq req) {
        AIPromptDTO aiPromptDTO = fileTransferTaskErrorAIPromptService.getPrompt(taskContext.getFileTaskContext());
        if (!taskContext.isTaskFail()) {
            return getDirectlyAIChatRecord(
                username,
                appId,
                aiPromptDTO,
                aiMessageI18nService.getNotFailTaskAIAnswerMessage()
            );
        }
        if (StringUtils.isEmpty(req.getContent())) {
            return getDirectlyAIChatRecord(
                username,
                appId,
                aiPromptDTO,
                getRenderedEmptyLogTaskAIAnswerMessage()
            );
        }
        AIAnalyzeErrorContextDTO analyzeErrorContext = AIAnalyzeErrorContextDTO.fromAIAnalyzeErrorReq(req);
        return getAIChatRecord(username, appId, aiPromptDTO, analyzeErrorContext);
    }

    /**
     * 获取变量渲染后的空日志报错回复消息
     *
     * @return 空日志报错回复消息
     */
    private String getRenderedEmptyLogTaskAIAnswerMessage() {
        String messageTemplate = aiMessageI18nService.getEmptyLogTaskAIAnswerMessage();
        if (StringUtils.isBlank(messageTemplate)) {
            return messageTemplate;
        }
        return messageTemplate.replace(aiTemplateVarService.getBkHelperLinkPlaceHolder(), bkConfig.getBkHelperLink());
    }
}
