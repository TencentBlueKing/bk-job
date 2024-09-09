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

package com.tencent.bk.job.analysis.consts;

import lombok.Getter;

/**
 * AI对话状态
 */
public enum AIChatStatusEnum {
    /**
     * 初始状态
     */
    INIT(0, "Chat initialized."),
    /**
     * 正在回答
     */
    REPLYING(1, "Chat is replying."),
    /**
     * 已完成
     */
    FINISHED(2, "Chat finished."),
    /**
     * 已终止
     */
    TERMINATED(3, "Chat terminated.");

    @Getter
    private final int status;

    @Getter
    private final String description;

    AIChatStatusEnum(int status, String description) {
        this.status = status;
        this.description = description;
    }

    public static AIChatStatusEnum getAIChatStatus(int status) {
        for (AIChatStatusEnum aiChatStatusEnum : AIChatStatusEnum.values()) {
            if (aiChatStatusEnum.getStatus() == status) {
                return aiChatStatusEnum;
            }
        }
        throw new RuntimeException("Unknown AIChat status " + status);
    }

}
