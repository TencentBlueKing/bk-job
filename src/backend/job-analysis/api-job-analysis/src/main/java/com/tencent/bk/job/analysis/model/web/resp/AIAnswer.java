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

package com.tencent.bk.job.analysis.model.web.resp;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tencent.bk.job.analysis.consts.AIConsts;
import com.tencent.bk.job.common.util.json.LongTimestampSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@ApiModel("AI回答内容")
@Data
public class AIAnswer {

    /**
     * 错误码
     */
    @ApiModelProperty(value = "错误码")
    private String errorCode;

    /**
     * 错误信息
     */
    @ApiModelProperty(value = "错误信息")
    private String errorMessage;

    /**
     * 内容
     */
    @ApiModelProperty(value = "内容")
    private String content;

    /**
     * 回答时间
     */
    @ApiModelProperty("回答时间")
    @JsonSerialize(using = LongTimestampSerializer.class)
    private Long time;

    public static AIAnswer successAnswer(String content) {
        AIAnswer aiAnswer = new AIAnswer();
        aiAnswer.setErrorCode(AIConsts.AI_ANSWER_ERROR_CODE_OK);
        aiAnswer.setTime(System.currentTimeMillis());
        aiAnswer.setContent(content);
        return aiAnswer;
    }

    public static AIAnswer failAnswer(String content, String errorMessage) {
        AIAnswer aiAnswer = new AIAnswer();
        aiAnswer.setErrorCode(AIConsts.AI_ANSWER_ERROR_CODE_FAILED);
        aiAnswer.setErrorMessage(errorMessage);
        aiAnswer.setTime(System.currentTimeMillis());
        aiAnswer.setContent(content);
        return aiAnswer;
    }
}
