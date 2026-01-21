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

package com.tencent.bk.job.analysis.listener;

import com.tencent.bk.job.analysis.consts.AIChatOperationEnum;
import com.tencent.bk.job.analysis.listener.event.AIChatOperationEvent;
import com.tencent.bk.job.analysis.service.ai.ChatService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * AI对话事件处理
 */
@Component("aiChatOperationEvent")
@Slf4j
public class AIChatOperationEventListener {

    private final ChatService chatService;

    @Autowired
    public AIChatOperationEventListener(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 处理AI对话操作事件
     *
     * @param event AI对话操作事件
     */
    public void handleEvent(AIChatOperationEvent event) {
        log.info("Handle aiChatOperation event, event: {}, duration: {}ms", event, event.duration());
        String username = event.getUsername();
        long recordId = event.getRecordId();
        AIChatOperationEnum action = AIChatOperationEnum.valueOf(event.getAction());
        try {
            if (action == AIChatOperationEnum.TERMINATE_CHAT) {
                chatService.terminateChat(username, recordId);
            } else {
                log.error("Invalid event action: {}", action);
            }
        } catch (Throwable e) {
            String errorMsg = MessageFormatter.format(
                "Handle aiChatOperation event error, username={}, recordId={}",
                username,
                recordId
            ).getMessage();
            log.error(errorMsg, e);
        }
    }

}
