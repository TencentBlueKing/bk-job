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
import com.tencent.bk.job.analysis.model.web.resp.AIAnswer;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.tracing.util.TraceUtil;
import com.tencent.bk.job.common.util.StringUtil;
import com.tencent.bk.job.common.util.json.JsonUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * 处理AI回答内容、报错信息的工具类
 */
@Slf4j
public class AIAnswerUtil {

    /**
     * 获取限长的AI回答报错信息
     *
     * @param errorMessage AI回答报错信息
     * @return 处理后的AI回答报错信息
     */
    public static String getLimitedErrorMessage(String errorMessage) {
        return getLimitedString(errorMessage, AIConsts.MAX_LENGTH_AI_ANSWER_ERROR_MESSAGE, "errorMessage");
    }

    /**
     * 获取限长的AI回答
     *
     * @param aiAnswer AI回答
     * @return 处理后的AI回答
     */
    public static String getLimitedAIAnswer(String aiAnswer) {
        return getLimitedString(aiAnswer, AIConsts.MAX_LENGTH_AI_ANSWER, "aiAnswer");
    }

    /**
     * 获取限长的字符串
     *
     * @param rawString  原始字符串
     * @param maxLength  最大长度
     * @param stringDesc 原始字符串描述，用于日志打印
     * @return 处理后的AI回答
     */
    private static String getLimitedString(String rawString, int maxLength, String stringDesc) {
        if (rawString == null) {
            return null;
        }
        if (rawString.length() > maxLength) {
            log.info(
                "{} is too long({}), truncated to {}",
                stringDesc,
                rawString.length(),
                maxLength
            );
            return StringUtil.substring(rawString, maxLength);
        }
        return rawString;
    }

    /**
     * 设置RequestId、序列化AIAnswer内容并写入到输出流中
     *
     * @param outputStream 输出流
     * @param respBody     响应数据
     * @throws IOException IO异常
     */
    public static void setRequestIdAndWriteResp(OutputStream outputStream,
                                                Response<AIAnswer> respBody) throws IOException {
        String traceId = TraceUtil.getTraceIdFromCurrentSpan();
        respBody.setRequestId(traceId);
        String message = JsonUtils.toJson(respBody) + "\n";
        outputStream.write(message.getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }
}
