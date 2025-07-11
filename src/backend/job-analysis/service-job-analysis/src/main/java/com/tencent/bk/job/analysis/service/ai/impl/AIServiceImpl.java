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

import com.tencent.bk.job.analysis.model.dto.AIChatHistoryDTO;
import com.tencent.bk.job.analysis.model.web.resp.AIAnswer;
import com.tencent.bk.job.analysis.service.ai.AIService;
import com.tencent.bk.job.analysis.service.login.LoginTokenService;
import com.tencent.bk.job.common.aidev.IBkAIDevClient;
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

@Slf4j
@Service
public class AIServiceImpl implements AIService {

    private final LoginTokenService loginTokenService;
    private final IBkAIDevClient bkAIDevClient;
    private final IBkOpenAIClient bkOpenAIClient;
    private final AIMessageI18nService aiMessageI18nService;

    @Autowired
    public AIServiceImpl(LoginTokenService loginTokenService,
                         IBkAIDevClient bkAIDevClient,
                         IBkOpenAIClient bkOpenAIClient,
                         AIMessageI18nService aiMessageI18nService) {
        this.loginTokenService = loginTokenService;
        this.bkAIDevClient = bkAIDevClient;
        this.bkOpenAIClient = bkOpenAIClient;
        this.aiMessageI18nService = aiMessageI18nService;
    }

    @Override
    public CompletableFuture<String> getAIAnswerStream(List<AIChatHistoryDTO> chatHistoryDTOList,
                                                       String userInput,
                                                       Consumer<String> partialRespConsumer) {
        String token = loginTokenService.getToken();
        return bkOpenAIClient.getHunYuanAnswerStream(
            token,
            buildMessageHistoryList(chatHistoryDTOList),
            userInput,
            partialRespConsumer
        );
    }

    @Override
    public AIAnswer getAIAnswer(List<AIChatHistoryDTO> chatHistoryDTOList, String userInput) {
        String token = loginTokenService.getToken();
        return AIAnswer.successAnswer(
            bkAIDevClient.getHunYuanAnswer(
                token,
                buildMessageHistoryList(chatHistoryDTOList),
                userInput
            )
        );
    }

    @Override
    public AIAnswer getAIAnswer(String userInput) {
        return getAIAnswer(null, userInput);
    }

    private List<AIDevMessage> buildMessageHistoryList(List<AIChatHistoryDTO> chatHistoryDTOList) {
        if (CollectionUtils.isEmpty(chatHistoryDTOList)) {
            return getSystemMessageList();
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
        messageHistoryList.addAll(getSystemMessageList());
        return messageHistoryList;
    }

    private List<AIDevMessage> getSystemMessageList() {
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
