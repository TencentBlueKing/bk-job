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

package com.tencent.bk.job.analysis.listener.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tencent.bk.job.analysis.consts.AIChatOperationEnum;
import com.tencent.bk.job.common.event.Event;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.StringJoiner;

/**
 * AI对话操作事件
 */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AIChatOperationEvent extends Event {
    /**
     * AI对话相关操作
     *
     * @see AIChatOperationEnum
     */
    private int action;
    /**
     * 操作用户
     */
    private String username;
    /**
     * AI对话记录ID
     */
    private Long recordId;

    /**
     * 构造终止AI对话事件
     *
     * @param recordId 对话记录ID
     * @return 终止AI对话事件
     */
    public static AIChatOperationEvent terminateChat(String username, long recordId) {
        AIChatOperationEvent event = new AIChatOperationEvent();
        event.setAction(AIChatOperationEnum.TERMINATE_CHAT.getValue());
        event.setUsername(username);
        event.setRecordId(recordId);
        event.setTime(LocalDateTime.now());
        return event;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", AIChatOperationEvent.class.getSimpleName() + "[", "]")
            .add("action=" + action)
            .add("username=" + username)
            .add("recordId=" + recordId)
            .add("time=" + time)
            .toString();
    }
}
