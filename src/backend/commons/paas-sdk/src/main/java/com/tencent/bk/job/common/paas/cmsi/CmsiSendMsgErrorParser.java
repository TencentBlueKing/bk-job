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

package com.tencent.bk.job.common.paas.cmsi;

import com.tencent.bk.job.common.esb.model.OpenApiError;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 解析 CMSI 发送消息接口返回的错误信息
 */
class CmsiSendMsgErrorParser {

    /**
     * CMSI 返回的发送失败错误码
     */
    private static final String ERROR_CODE_SEND_FAILED = "SEND_FAILED";
    private static final String DETAIL_KEY_MESSAGE = "message";
    private static final String DETAIL_KEY_FAILED = "failed";
    /**
     * 渠道适配器返回的「接收人无效」错误文案关键字，需为小写
     */
    private static final List<String> INVALID_RECEIVER_KEYWORDS = Arrays.asList(
        "no valid receiver",
        "无有效的接收者"
    );

    private CmsiSendMsgErrorParser() {
    }

    /**
     * 判定发送失败是否由接收人无效引起。
     * 以语义化的 error.code 作为主判据，具体失败原因只能从渠道适配器返回的错误文案中识别，
     * 文案不匹配时保守地判定为 false，即按普通发送失败处理。
     */
    static boolean isInvalidReceiverError(OpenApiError error) {
        if (error == null || !ERROR_CODE_SEND_FAILED.equals(error.getCode())) {
            return false;
        }
        if (CollectionUtils.isEmpty(error.getDetails())) {
            return false;
        }
        for (Map<String, Object> detail : error.getDetails()) {
            Object message = detail.get(DETAIL_KEY_MESSAGE);
            if (message == null) {
                continue;
            }
            String lowerCaseMessage = message.toString().toLowerCase();
            for (String keyword : INVALID_RECEIVER_KEYWORDS) {
                if (lowerCaseMessage.contains(keyword)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 提取渠道侧返回的发送失败接收人，渠道未返回明细时为空列表
     */
    static List<String> extractFailedReceivers(OpenApiError error) {
        List<String> failedReceivers = new ArrayList<>();
        if (error == null || CollectionUtils.isEmpty(error.getDetails())) {
            return failedReceivers;
        }
        for (Map<String, Object> detail : error.getDetails()) {
            Object failed = detail.get(DETAIL_KEY_FAILED);
            if (failed instanceof Collection) {
                ((Collection<?>) failed).forEach(receiver -> failedReceivers.add(String.valueOf(receiver)));
            }
        }
        return failedReceivers;
    }
}
