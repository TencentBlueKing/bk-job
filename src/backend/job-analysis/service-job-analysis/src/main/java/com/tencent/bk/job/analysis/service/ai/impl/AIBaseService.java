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
import com.tencent.bk.job.analysis.model.dto.AIPromptDTO;
import com.tencent.bk.job.analysis.model.web.resp.AIAnswer;
import com.tencent.bk.job.analysis.model.web.resp.AIChatRecord;
import com.tencent.bk.job.analysis.service.ai.AIChatHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AIBaseService {

    private final AIChatHistoryService aiChatHistoryService;

    @Autowired
    public AIBaseService(AIChatHistoryService aiChatHistoryService) {
        this.aiChatHistoryService = aiChatHistoryService;
    }

    /**
     * 使用AI提示符调用AI接口生成AI回答
     *
     * @param username    用户名
     * @param aiPromptDTO AI提示符
     * @return AI对话记录
     */
    public AIChatRecord getAIChatRecord(String username, AIPromptDTO aiPromptDTO) {
        long startTime = System.currentTimeMillis();
        String rawPrompt = aiPromptDTO.getRawPrompt();
        String renderedPrompt = aiPromptDTO.getRenderedPrompt();
        // 1.插入初始聊天记录
        AIChatHistoryDTO aiChatHistoryDTO = aiChatHistoryService.buildAIChatHistoryDTO(
            username,
            startTime,
            rawPrompt,
            renderedPrompt,
            AIChatStatusEnum.INIT.getStatus(),
            null
        );
        Long historyId = aiChatHistoryService.insertChatHistory(aiChatHistoryDTO);
        aiChatHistoryDTO.setId(historyId);
        return aiChatHistoryDTO.toAIChatRecord();
    }

    /**
     * 使用指定内容直接生成AI回答
     *
     * @param username    用户名
     * @param aiPromptDTO AI提示符
     * @param content     指定内容
     * @return AI对话记录
     */
    public AIChatRecord getDirectlyAIChatRecord(String username, AIPromptDTO aiPromptDTO, String content) {
        long startTime = System.currentTimeMillis();
        String rawPrompt = aiPromptDTO.getRawPrompt();
        AIAnswer aiAnswer = new AIAnswer("0", "", content, System.currentTimeMillis());
        AIChatHistoryDTO aiChatHistoryDTO = aiChatHistoryService.buildAIChatHistoryDTO(
            username,
            startTime,
            rawPrompt,
            buildAIDirectlyAnswerInput(content),
            AIChatStatusEnum.FINISHED.getStatus(),
            aiAnswer
        );
        Long historyId = aiChatHistoryService.insertChatHistory(aiChatHistoryDTO);
        aiChatHistoryDTO.setId(historyId);
        return aiChatHistoryDTO.toAIChatRecord();
    }

    /**
     * 构建直接回答的AI输入（无需调用AI生成回答）
     *
     * @param content 回答内容
     * @return AI输入
     */
    private String buildAIDirectlyAnswerInput(String content) {
        return "Answer This Directly:" + content;
    }
}
