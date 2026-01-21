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

package com.tencent.bk.job.analysis.service.ai.context.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 含有部分消息的事件
 */
@AllArgsConstructor
@Data
public class MessagePartEvent {
    /**
     * 是否为消息结束事件
     */
    private boolean end;
    /**
     * 消息内容
     */
    private String messagePart;
    /**
     * 事件产生时间
     */
    private Long timeMills;
    /**
     * 异常
     */
    private Throwable throwable;

    public static MessagePartEvent endEvent(Throwable throwable) {
        return new MessagePartEvent(true, null, System.currentTimeMillis(), throwable);
    }

    public static MessagePartEvent normalEvent(String messagePart) {
        return new MessagePartEvent(false, messagePart, System.currentTimeMillis(), null);
    }
}
