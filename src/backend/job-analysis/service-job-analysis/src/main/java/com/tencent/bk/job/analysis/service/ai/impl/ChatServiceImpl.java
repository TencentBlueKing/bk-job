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
import com.tencent.bk.job.analysis.model.web.resp.AIAnswer;
import com.tencent.bk.job.analysis.service.ai.AIChatHistoryService;
import com.tencent.bk.job.analysis.service.ai.AIService;
import com.tencent.bk.job.analysis.service.ai.ChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    private final AIService aiService;
    private final AIChatHistoryService aiChatHistoryService;

    @Autowired
    public ChatServiceImpl(AIService aiService, AIChatHistoryService aiChatHistoryService) {
        this.aiService = aiService;
        this.aiChatHistoryService = aiChatHistoryService;
    }

    @Override
    public AIAnswer chatWithAI(String username, String userInput) {
        // 1.调用AI服务获取回答
        Long startTime = System.currentTimeMillis();
        AIAnswer aiAnswer = aiService.getAIAnswer(userInput);
        // 2.保存聊天记录
        AIChatHistoryDTO aiChatHistoryDTO = buildAIChatHistoryDTO(username, startTime, userInput, aiAnswer);
        aiChatHistoryService.insertChatHistory(aiChatHistoryDTO);
        return aiAnswer;
    }

    @Override
    public List<AIChatHistoryDTO> getLatestChatHistoryList(String username, Integer start, Integer length) {
        return aiChatHistoryService.getLatestChatHistoryList(username, start, length);
    }

    private AIChatHistoryDTO buildAIChatHistoryDTO(String username,
                                                   Long startTime,
                                                   String userInput,
                                                   AIAnswer aiAnswer) {
        AIChatHistoryDTO aiChatHistoryDTO = new AIChatHistoryDTO();
        aiChatHistoryDTO.setUsername(username);
        aiChatHistoryDTO.setUserInput(userInput);
        aiChatHistoryDTO.setPromptTemplateId(null);
        aiChatHistoryDTO.setAiInput(userInput);
        aiChatHistoryDTO.setAiAnswer(aiAnswer.getContent());
        aiChatHistoryDTO.setErrorCode(String.valueOf(aiAnswer.getErrorCode()));
        aiChatHistoryDTO.setErrorMessage(aiAnswer.getErrorMessage());
        aiChatHistoryDTO.setStartTime(startTime);
        aiChatHistoryDTO.setAnswerTime(System.currentTimeMillis());
        aiChatHistoryDTO.updateTotalTime();
        aiChatHistoryDTO.setIsDeleted(false);
        return aiChatHistoryDTO;
    }
}
