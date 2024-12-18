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

package com.tencent.bk.job.analysis.util.ai;

import com.tencent.bk.job.analysis.consts.AIConsts;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 单元测试 - 处理AI回答内容、报错信息的工具类
 */
public class AIAnswerUtilTest {

    @Test
    public void testGetLimitedErrorMessage() {
        assertThat(AIAnswerUtil.getLimitedErrorMessage(null)).isNull();
        String rawMessage = "";
        String limitedErrorMessage = AIAnswerUtil.getLimitedErrorMessage(rawMessage);
        assertThat(limitedErrorMessage).isNotNull();
        assertThat(limitedErrorMessage).isEqualTo(rawMessage);
        rawMessage = "123456";
        limitedErrorMessage = AIAnswerUtil.getLimitedErrorMessage(rawMessage);
        assertThat(limitedErrorMessage).isEqualTo(rawMessage);

        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < 52; i++) {
            stringBuilder.append("1234567890");
        }
        rawMessage = stringBuilder.toString();
        limitedErrorMessage = AIAnswerUtil.getLimitedErrorMessage(rawMessage);
        assertThat(limitedErrorMessage.length()).isEqualTo(AIConsts.MAX_LENGTH_AI_ANSWER_ERROR_MESSAGE);
        assertThat(limitedErrorMessage).endsWith("12");
    }

    @Test
    public void testGetLimitedAIAnswer() {
        assertThat(AIAnswerUtil.getLimitedAIAnswer(null)).isNull();
        String rawAIAnswer = "";
        String limitedAIAnswer = AIAnswerUtil.getLimitedAIAnswer(rawAIAnswer);
        assertThat(limitedAIAnswer).isNotNull();
        assertThat(limitedAIAnswer).isEqualTo(rawAIAnswer);
        rawAIAnswer = "123456";
        limitedAIAnswer = AIAnswerUtil.getLimitedAIAnswer(rawAIAnswer);
        assertThat(limitedAIAnswer).isEqualTo(rawAIAnswer);

        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < 1500; i++) {
            stringBuilder.append("1234567890");
        }
        stringBuilder.append("ABC");
        rawAIAnswer = stringBuilder.toString();
        limitedAIAnswer = AIAnswerUtil.getLimitedAIAnswer(rawAIAnswer);
        assertThat(limitedAIAnswer.length()).isEqualTo(AIConsts.MAX_LENGTH_AI_ANSWER);
        assertThat(limitedAIAnswer).endsWith("1234567890");
    }

}
