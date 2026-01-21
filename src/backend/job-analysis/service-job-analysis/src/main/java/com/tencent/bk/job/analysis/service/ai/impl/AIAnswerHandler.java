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

import com.tencent.bk.job.analysis.model.web.resp.AIAnswer;
import com.tencent.bk.job.analysis.service.ai.AIChatHistoryService;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.util.I18nUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.CancellationException;

/**
 * AI回答处理器
 */
@Slf4j
@Service
public class AIAnswerHandler {

    private final AIChatHistoryService aiChatHistoryService;

    @Autowired
    public AIAnswerHandler(AIChatHistoryService aiChatHistoryService) {
        this.aiChatHistoryService = aiChatHistoryService;
    }

    /**
     * 处理完整的AI回答
     *
     * @param recordId  对话记录ID
     * @param content   AI回答内容
     * @param throwable AI回答过程产生的异常
     */
    public void handleAIAnswer(Long recordId, String content, Throwable throwable) {
        try {
            doHandleAIAnswer(recordId, content, throwable);
        } catch (Throwable t) {
            log.error("Fail to handleAIAnswer", t);
        }
    }

    public void doHandleAIAnswer(Long recordId, String content, Throwable throwable) {
        AIAnswer aiAnswer;
        if (throwable == null) {
            // 1.对话正常完成
            aiAnswer = AIAnswer.successAnswer(content);
            int affectedRow = aiChatHistoryService.finishAIAnswer(recordId, aiAnswer);
            log.info(
                "AIAnswer finished, recordId={}, length={}, affectedRow={}",
                recordId,
                content.length(),
                affectedRow
            );
        } else if (throwable instanceof CancellationException) {
            // 2.对话被主动终止
            int affectedRow = aiChatHistoryService.terminateAIAnswer(recordId);
            log.info(
                "AIAnswer terminated, recordId={}, affectedRow={}",
                recordId,
                affectedRow
            );
        } else {
            // 3.对话异常
            String errorContent = I18nUtil.getI18nMessage(String.valueOf(ErrorCode.BK_OPEN_AI_API_DATA_ERROR));
            aiAnswer = AIAnswer.failAnswer(errorContent, throwable.getMessage());
            int affectedRow = aiChatHistoryService.finishAIAnswer(recordId, aiAnswer);
            String message = MessageFormatter.arrayFormat(
                "AIAnswer finished(fail), recordId={}, affectedRow={}",
                new Object[]{
                    recordId,
                    affectedRow
                }
            ).getMessage();
            log.warn(message, throwable);
        }
    }
}
