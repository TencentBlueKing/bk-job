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

package com.tencent.bk.job.common.paas.exception;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InvalidParamException;
import lombok.Getter;

import java.util.List;

/**
 * 通知渠道侧判定消息接收人无效（如已离职）导致发送失败。
 * 属于接收人参数问题而非服务故障，因此不归入 PaasException，
 * 否则同步调用链上会被全局异常处理器按兜底的 ServiceException 打出 ERROR 日志。
 * 调用方无需重试。
 */
@Getter
public class CmsiInvalidReceiverException extends InvalidParamException {

    /**
     * 通知渠道类型
     */
    private final String msgType;

    /**
     * 渠道侧返回的无效接收人，渠道未返回明细时为空
     */
    private final List<String> invalidReceivers;

    public CmsiInvalidReceiverException(String msgType, List<String> invalidReceivers) {
        super(
            ErrorCode.CMSI_NO_VALID_RECEIVER,
            new Object[]{String.join(",", invalidReceivers)}
        );
        this.msgType = msgType;
        this.invalidReceivers = invalidReceivers;
    }
}
