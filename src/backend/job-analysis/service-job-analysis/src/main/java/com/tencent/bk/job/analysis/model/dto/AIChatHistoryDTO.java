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

package com.tencent.bk.job.analysis.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tencent.bk.job.analysis.consts.AIChatStatusEnum;
import com.tencent.bk.job.analysis.model.web.resp.AIAnswer;
import com.tencent.bk.job.analysis.model.web.resp.AIChatRecord;
import com.tencent.bk.job.analysis.model.web.resp.UserInput;
import com.tencent.bk.job.analysis.util.ai.AIAnswerUtil;
import com.tencent.bk.job.common.util.TimeUtil;
import com.tencent.bk.job.common.util.json.LongTimestampSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AI对话历史记录
 */
@Slf4j
@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class AIChatHistoryDTO {
    /**
     * id
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * Job业务ID
     */
    private Long appId;

    /**
     * 用户输入内容
     */
    private String userInput;

    /**
     * 使用的提示符模板ID
     */
    private Integer promptTemplateId;

    /**
     * 提交给AI的输入内容
     */
    private String aiInput;

    /**
     * AI对话状态，取值源于{@link AIChatStatusEnum}.
     */
    private Integer status;

    /**
     * AI回答的内容
     */
    private String aiAnswer;

    /**
     * AI回答失败时的错误码
     */
    private String errorCode;

    /**
     * AI回答失败时的错误信息
     */
    private String errorMessage;

    /**
     * 开始时间
     */
    @JsonSerialize(using = LongTimestampSerializer.class)
    private Long startTime;

    /**
     * AI回答完成时间
     */
    @JsonSerialize(using = LongTimestampSerializer.class)
    private Long answerTime;

    /**
     * 总耗时
     */
    @JsonSerialize(using = LongTimestampSerializer.class)
    private Long totalTime;

    /**
     * 是否已删除：0表示未删除，1表示已删除
     */
    private Boolean isDeleted;

    /**
     * 报错分析场景的上下文信息
     */
    private AIAnalyzeErrorContextDTO aiAnalyzeErrorContext;

    public void updateTotalTime() {
        if (startTime != null && answerTime != null) {
            totalTime = answerTime - startTime;
        }
    }

    public AIChatRecord toAIChatRecord() {
        AIChatRecord aiChatRecord = new AIChatRecord();
        aiChatRecord.setId(id);
        UserInput userInput = new UserInput();
        userInput.setContent(this.userInput);
        userInput.setTime(startTime);
        aiChatRecord.setUserInput(userInput);
        AIAnswer aiAnswer = new AIAnswer();
        aiAnswer.setContent(this.aiAnswer);
        aiAnswer.setTime(answerTime);
        aiAnswer.setErrorCode(errorCode);
        aiAnswer.setErrorMessage(errorMessage);
        aiChatRecord.setAiAnswer(aiAnswer);
        aiChatRecord.setStatus(status);
        return aiChatRecord;
    }

    @JsonIgnore
    public boolean isInitOrReplying() {
        return status == AIChatStatusEnum.INIT.getStatus() || status == AIChatStatusEnum.REPLYING.getStatus();
    }

    @JsonIgnore
    public boolean isFinished() {
        return status == AIChatStatusEnum.FINISHED.getStatus();
    }

    @JsonIgnore
    public String getLimitedAIAnswer() {
        return AIAnswerUtil.getLimitedAIAnswer(aiAnswer);
    }

    public String getLimitedErrorMessage() {
        return AIAnswerUtil.getLimitedErrorMessage(errorMessage);
    }

    @JsonIgnore
    public String getAnswerTimeStr() {
        if (answerTime == null) {
            return null;
        }
        return TimeUtil.formatTime(answerTime, "yyyy-MM-dd HH:mm:ss.SSS");
    }
}
