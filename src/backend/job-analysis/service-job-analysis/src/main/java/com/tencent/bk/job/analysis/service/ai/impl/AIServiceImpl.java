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

import com.tencent.bk.job.analysis.model.dto.AIChatHistoryDTO;
import com.tencent.bk.job.analysis.service.ai.AIService;
import com.tencent.bk.job.analysis.service.login.LoginTokenService;
import com.tencent.bk.job.common.aidev.IBkOpenAIClient;
import com.tencent.bk.job.common.aidev.model.common.AIDevMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * AI服务实现类
 */
@Slf4j
@Service
public class AIServiceImpl implements AIService {

    private final LoginTokenService loginTokenService;
    private final IBkOpenAIClient bkOpenAIClient;
    private final AIMessageI18nService aiMessageI18nService;

    @Autowired
    public AIServiceImpl(LoginTokenService loginTokenService,
                         IBkOpenAIClient bkOpenAIClient,
                         AIMessageI18nService aiMessageI18nService) {
        this.loginTokenService = loginTokenService;
        this.bkOpenAIClient = bkOpenAIClient;
        this.aiMessageI18nService = aiMessageI18nService;
    }


    /**
     * 通过蓝鲸OpenAI接口获取AI回答流
     *
     * @param chatHistoryDTOList  历史对话记录
     * @param userInput           用户输入
     * @param partialRespConsumer AI回答流回调
     * @return AI回答结果Future
     */
    @Override
    public CompletableFuture<String> getAIAnswerStream(List<AIChatHistoryDTO> chatHistoryDTOList,
                                                       String userInput,
                                                       Consumer<String> partialRespConsumer) {
        String token = loginTokenService.getToken();
        return bkOpenAIClient.getAIAnswerStream(
            token,
            buildMessageHistoryList(chatHistoryDTOList),
            userInput,
            partialRespConsumer
        );
    }

    /**
     * 构建传递给蓝鲸OpenAI接口的历史消息记录列表，用作问答上下文
     *
     * @param chatHistoryDTOList 历史对话记录
     * @return 历史消息列表
     */
    private List<AIDevMessage> buildMessageHistoryList(List<AIChatHistoryDTO> chatHistoryDTOList) {
        if (CollectionUtils.isEmpty(chatHistoryDTOList)) {
            return buildSystemMessageList();
        }
        List<AIDevMessage> messageHistoryList = new ArrayList<>();
        for (AIChatHistoryDTO chatHistoryDTO : chatHistoryDTOList) {
            AIDevMessage aiDevMessage = new AIDevMessage();
            aiDevMessage.setRole(AIDevMessage.ROLE_USER);
            aiDevMessage.setContent(chatHistoryDTO.getAiInput());
            messageHistoryList.add(aiDevMessage);
            aiDevMessage = new AIDevMessage();
            aiDevMessage.setRole(AIDevMessage.ROLE_ASSISTANT);
            aiDevMessage.setContent(chatHistoryDTO.getAiAnswer());
            messageHistoryList.add(aiDevMessage);
        }
        messageHistoryList.addAll(buildSystemMessageList());
        return messageHistoryList;
    }

    /**
     * 构建系统消息列表，用于指定环境语言等基础信息
     *
     * @return 系统消息列表
     */
    private List<AIDevMessage> buildSystemMessageList() {
        List<AIDevMessage> systemMessageList = new ArrayList<>();
        AIDevMessage languageSpecifyMessage = new AIDevMessage();
        // ROLE_SYSTEM系统角色不生效，使用ROLE_USER消息代替
        languageSpecifyMessage.setRole(AIDevMessage.ROLE_USER);
        languageSpecifyMessage.setContent(aiMessageI18nService.getLanguageSpecifySystemMessage());
        systemMessageList.add(languageSpecifyMessage);
        AIDevMessage aiReplyMessage = new AIDevMessage();
        aiReplyMessage.setRole(AIDevMessage.ROLE_ASSISTANT);
        aiReplyMessage.setContent(aiMessageI18nService.getLanguageSpecifyAIReplyMessage());
        systemMessageList.add(aiReplyMessage);
        return systemMessageList;
    }
}
