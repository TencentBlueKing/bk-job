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

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.common.esb.model.OpenApiError;
import com.tencent.bk.job.common.util.json.JsonUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CmsiSendMsgErrorParserTest {

    private OpenApiError parseError(String errorStr) {
        return JsonUtils.fromJson(errorStr, new TypeReference<OpenApiError>() {
        });
    }

    @Test
    void isInvalidReceiverErrorWhenChannelReportNoValidReceiver() {
        String errorStr = "{\"code\":\"SEND_FAILED\",\"message\":\"部分消息发送失败\","
            + "\"details\":[{\"adapter_id\":\"tof\",\"code\":\"REQUEST_ERROR\","
            + "\"message\":\"[code:10002] 无有效的接收者 no valid receiver。更多信息请查看："
            + "https://xx.com/tools/query-site?errcode=10002\",\"failed\":[\"zhangsan\"]}],"
            + "\"data\":{\"channel_type\":\"weixin\",\"failed\":[\"zhangsan\"]}}";
        OpenApiError error = parseError(errorStr);

        assertThat(CmsiSendMsgErrorParser.isInvalidReceiverError(error)).isTrue();
        assertThat(CmsiSendMsgErrorParser.extractFailedReceivers(error)).containsExactly("zhangsan");
    }

    @Test
    void isInvalidReceiverErrorWhenSendFailedByOtherReason() {
        String errorStr = "{\"code\":\"SEND_FAILED\",\"message\":\"部分消息发送失败\","
            + "\"details\":[{\"adapter_id\":\"tof\",\"code\":\"REQUEST_ERROR\","
            + "\"message\":\"channel unavailable\",\"failed\":[]}]}";
        OpenApiError error = parseError(errorStr);

        assertThat(CmsiSendMsgErrorParser.isInvalidReceiverError(error)).isFalse();
    }

    @Test
    void isInvalidReceiverErrorWhenErrorCodeNotSendFailed() {
        String errorStr = "{\"code\":\"INTERNAL\",\"message\":\"internal error\","
            + "\"details\":[{\"message\":\"no valid receiver\"}]}";
        OpenApiError error = parseError(errorStr);

        assertThat(CmsiSendMsgErrorParser.isInvalidReceiverError(error)).isFalse();
    }

    @Test
    void isInvalidReceiverErrorWhenDetailsAbsent() {
        OpenApiError error = parseError("{\"code\":\"SEND_FAILED\",\"message\":\"部分消息发送失败\"}");

        assertThat(CmsiSendMsgErrorParser.isInvalidReceiverError(error)).isFalse();
        assertThat(CmsiSendMsgErrorParser.extractFailedReceivers(error)).isEmpty();
    }

    @Test
    void isInvalidReceiverErrorWhenErrorIsNull() {
        assertThat(CmsiSendMsgErrorParser.isInvalidReceiverError(null)).isFalse();
        assertThat(CmsiSendMsgErrorParser.extractFailedReceivers(null)).isEmpty();
    }

    @Test
    void extractFailedReceiversWhenChannelReportNoFailedDetail() {
        String errorStr = "{\"code\":\"SEND_FAILED\",\"message\":\"消息发送失败\","
            + "\"details\":[{\"message\":\"no valid receiver\"}]}";
        OpenApiError error = parseError(errorStr);

        assertThat(CmsiSendMsgErrorParser.isInvalidReceiverError(error)).isTrue();
        assertThat(CmsiSendMsgErrorParser.extractFailedReceivers(error)).isEmpty();
    }
}
