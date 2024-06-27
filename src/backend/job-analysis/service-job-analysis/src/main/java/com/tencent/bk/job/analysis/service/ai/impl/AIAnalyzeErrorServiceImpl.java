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
import com.tencent.bk.job.analysis.service.ai.AIAnalyzeErrorService;
import com.tencent.bk.job.analysis.service.ai.AIChatHistoryService;
import com.tencent.bk.job.analysis.service.ai.AIPromptService;
import com.tencent.bk.job.analysis.service.ai.AIService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AIAnalyzeErrorServiceImpl implements AIAnalyzeErrorService {

    private final AIPromptService aiPromptService;
    private final AIService aiService;
    private final AIChatHistoryService aiChatHistoryService;

    @Autowired
    public AIAnalyzeErrorServiceImpl(AIPromptService aiPromptService,
                                     AIService aiService,
                                     AIChatHistoryService aiChatHistoryService) {
        this.aiPromptService = aiPromptService;
        this.aiService = aiService;
        this.aiChatHistoryService = aiChatHistoryService;
    }

    @Override
    public AIAnswer analyze(String username, String errorContent) {
        long startTime = System.currentTimeMillis();
        Pair<String, String> pair = aiPromptService.getAnalyzeErrorPrompt(errorContent);
        String rawPrompt = pair.getLeft();
        String renderedPrompt = pair.getRight();
        AIAnswer aiAnswer = aiService.getAIAnswer(renderedPrompt);
        AIChatHistoryDTO aiChatHistoryDTO = aiChatHistoryService.buildAIChatHistoryDTO(
            username,
            startTime,
            rawPrompt,
            renderedPrompt,
            aiAnswer
        );
        aiChatHistoryService.insertChatHistory(aiChatHistoryDTO);
        return aiAnswer;
    }
}
