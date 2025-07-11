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

package com.tencent.bk.job.analysis.api.web.impl;

import com.tencent.bk.job.analysis.api.web.WebAIResource;
import com.tencent.bk.job.analysis.config.AIProperties;
import com.tencent.bk.job.analysis.consts.AIChatStatusEnum;
import com.tencent.bk.job.analysis.consts.AIConsts;
import com.tencent.bk.job.analysis.model.dto.AIChatHistoryDTO;
import com.tencent.bk.job.analysis.model.web.req.AIAnalyzeErrorReq;
import com.tencent.bk.job.analysis.model.web.req.AICheckScriptReq;
import com.tencent.bk.job.analysis.model.web.req.AIGeneralChatReq;
import com.tencent.bk.job.analysis.model.web.req.GenerateChatStreamReq;
import com.tencent.bk.job.analysis.model.web.req.TerminateChatReq;
import com.tencent.bk.job.analysis.model.web.resp.AIChatRecord;
import com.tencent.bk.job.analysis.model.web.resp.ClearChatHistoryResp;
import com.tencent.bk.job.analysis.service.ai.AIAnalyzeErrorService;
import com.tencent.bk.job.analysis.service.ai.AIChatHistoryService;
import com.tencent.bk.job.analysis.service.ai.AICheckScriptService;
import com.tencent.bk.job.analysis.service.ai.ChatService;
import com.tencent.bk.job.analysis.service.ai.impl.AIConfigService;
import com.tencent.bk.job.analysis.service.ai.impl.AIMessageI18nService;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.error.ErrorType;
import com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController("jobAnalysisWebAIResource")
@Slf4j
public class WebAIResourceImpl implements WebAIResource {

    private final AIConfigService aiConfigService;
    private final ChatService chatService;
    private final AICheckScriptService aiCheckScriptService;
    private final AIAnalyzeErrorService aiAnalyzeErrorService;
    private final AIChatHistoryService aiChatHistoryService;
    private final AIMessageI18nService aiMessageI18nService;
    private final AIProperties aiProperties;

    @Autowired
    public WebAIResourceImpl(AIConfigService aiConfigService,
                             ChatService chatService,
                             AICheckScriptService aiCheckScriptService,
                             AIAnalyzeErrorService aiAnalyzeErrorService,
                             AIChatHistoryService aiChatHistoryService,
                             AIMessageI18nService aiMessageI18nService,
                             AIProperties aiProperties) {
        this.aiConfigService = aiConfigService;
        this.chatService = chatService;
        this.aiCheckScriptService = aiCheckScriptService;
        this.aiAnalyzeErrorService = aiAnalyzeErrorService;
        this.aiChatHistoryService = aiChatHistoryService;
        this.aiMessageI18nService = aiMessageI18nService;
        this.aiProperties = aiProperties;
    }

    @Override
    public Response<Map<String, Object>> getAIConfig(String username,
                                                     AppResourceScope appResourceScope,
                                                     String scopeType,
                                                     String scopeId) {
        return Response.buildSuccessResp(aiConfigService.getAIConfig());
    }

    @Override
    public Response<List<AIChatRecord>> getLatestChatHistoryList(String username,
                                                                 AppResourceScope appResourceScope,
                                                                 String scopeType,
                                                                 String scopeId,
                                                                 Integer start,
                                                                 Integer length) {
        if (!aiChatHistoryService.existsChatHistory(username)) {
            AIChatHistoryDTO greetingChatHistory = getGreetingChatHistory(username, appResourceScope.getAppId());
            Long id = aiChatHistoryService.insertChatHistory(greetingChatHistory);
            log.debug("Greeting chat history created, username={}, id={}", username, id);
        }
        List<AIChatHistoryDTO> chatRecordList = chatService.getLatestChatHistoryList(username, start, length);
        List<AIChatRecord> aiChatRecordList = chatRecordList.stream()
            .map(AIChatHistoryDTO::toAIChatRecord)
            .collect(Collectors.toList());
        return Response.buildSuccessResp(aiChatRecordList);
    }

    private AIChatHistoryDTO getGreetingChatHistory(String username, Long appId) {
        AIChatHistoryDTO greetingChatHistory = new AIChatHistoryDTO();
        greetingChatHistory.setUsername(username);
        greetingChatHistory.setAppId(appId);
        greetingChatHistory.setUserInput("");
        greetingChatHistory.setStartTime(System.currentTimeMillis());
        greetingChatHistory.setPromptTemplateId(null);
        greetingChatHistory.setAiInput("");
        greetingChatHistory.setStatus(AIChatStatusEnum.FINISHED.getStatus());
        greetingChatHistory.setAiAnswer(aiMessageI18nService.getAIGreetingMessage());
        greetingChatHistory.setAnswerTime(System.currentTimeMillis());
        greetingChatHistory.updateTotalTime();
        greetingChatHistory.setErrorCode(AIConsts.AI_ANSWER_ERROR_CODE_OK);
        greetingChatHistory.setErrorMessage(null);
        greetingChatHistory.setIsDeleted(false);
        return greetingChatHistory;
    }

    @Override
    public Response<AIChatRecord> generalChat(String username,
                                              AppResourceScope appResourceScope,
                                              String scopeType,
                                              String scopeId,
                                              AIGeneralChatReq req) {
        AIChatRecord aiChatRecord = chatService.chatWithAI(username, appResourceScope.getAppId(), req.getContent());
        return Response.buildSuccessResp(aiChatRecord);
    }

    @Override
    public Response<AIChatRecord> checkScript(String username,
                                              AppResourceScope appResourceScope,
                                              String scopeType,
                                              String scopeId,
                                              AICheckScriptReq req) {
        AIChatRecord aiChatRecord = aiCheckScriptService.check(
            username,
            appResourceScope.getAppId(),
            req.getType(),
            req.getContent()
        );
        return Response.buildSuccessResp(aiChatRecord);
    }

    @Override
    public Response<AIChatRecord> analyzeError(String username,
                                               AppResourceScope appResourceScope,
                                               String scopeType,
                                               String scopeId,
                                               AIAnalyzeErrorReq req) {
        checkScriptLogContentLength(req);
        AIChatRecord aiChatRecord = aiAnalyzeErrorService.analyze(username, appResourceScope.getAppId(), req);
        return Response.buildSuccessResp(aiChatRecord);
    }

    @Override
    public ResponseEntity<StreamingResponseBody> generateChatStream(String username,
                                                                    AppResourceScope appResourceScope,
                                                                    String scopeType,
                                                                    String scopeId,
                                                                    GenerateChatStreamReq req) {
        StreamingResponseBody streamingResponseBody = chatService.generateChatStream(username, req.getRecordId());
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(streamingResponseBody);
    }

    @Override
    public Response<Boolean> terminateChat(String username,
                                           AppResourceScope appResourceScope,
                                           String scopeType,
                                           String scopeId,
                                           TerminateChatReq req) {
        boolean result = chatService.triggerTerminateChat(username, req.getRecordId());
        return Response.buildSuccessResp(result);
    }

    /**
     * 结合动态配置的限制值检查脚本日志内容长度是否超限
     *
     * @param req 请求体
     */
    private void checkScriptLogContentLength(AIAnalyzeErrorReq req) {
        if (StepExecuteTypeEnum.EXECUTE_SCRIPT.getValue().equals(req.getStepExecuteType())) {
            Long logMaxLengthBytes = aiProperties.getAnalyzeErrorLog().getLogMaxLengthBytes();
            if (req.getContent().length() > logMaxLengthBytes) {
                throw new ServiceException(
                    ErrorType.INVALID_PARAM,
                    ErrorCode.AI_ANALYZE_ERROR_CONTENT_EXCEED_MAX_LENGTH,
                    new Object[]{aiProperties.getAnalyzeErrorLog().getLogMaxLength()}
                );
            }
        }
    }

    @Override
    public Response<ClearChatHistoryResp> clearChatHistory(String username,
                                                           AppResourceScope appResourceScope,
                                                           String scopeType,
                                                           String scopeId) {
        int deletedTotalCount = aiChatHistoryService.softDeleteChatHistory(username);
        return Response.buildSuccessResp(new ClearChatHistoryResp(deletedTotalCount));
    }

}
