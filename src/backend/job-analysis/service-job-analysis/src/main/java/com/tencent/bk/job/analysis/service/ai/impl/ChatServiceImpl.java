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

import com.tencent.bk.job.analysis.consts.AIChatStatusEnum;
import com.tencent.bk.job.analysis.model.dto.AIChatHistoryDTO;
import com.tencent.bk.job.analysis.model.web.resp.AIAnswer;
import com.tencent.bk.job.analysis.service.ai.AIChatHistoryService;
import com.tencent.bk.job.analysis.service.ai.AIService;
import com.tencent.bk.job.analysis.service.ai.ChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
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
        Long startTime = System.currentTimeMillis();
        // 1.获取最近的聊天记录
        List<AIChatHistoryDTO> chatHistoryDTOList = getLatestChatHistoryList(username, 0, 5);
        chatHistoryDTOList.sort(Comparator.comparing(AIChatHistoryDTO::getStartTime));
        // 2.保存初始聊天记录
        AIChatHistoryDTO aiChatHistoryDTO = aiChatHistoryService.buildAIChatHistoryDTO(
            username,
            startTime,
            userInput,
            userInput,
            AIChatStatusEnum.REPLYING.getStatus(),
            null
        );
        Long historyId = aiChatHistoryService.insertChatHistory(aiChatHistoryDTO);
        // 3.调用AI服务获取回答
        AIAnswer aiAnswer = aiService.getAIAnswer(chatHistoryDTOList, userInput);
        // 4.更新聊天记录状态与回答内容
        aiChatHistoryService.finishAIAnswer(historyId, aiAnswer);
        return aiAnswer;
    }

    @Override
    public List<AIChatHistoryDTO> getLatestChatHistoryList(String username, Integer start, Integer length) {
        return aiChatHistoryService.getLatestChatHistoryList(username, start, length);
    }

}
