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

package com.tencent.bk.job.analysis.service.ai.impl;

import com.tencent.bk.job.analysis.service.ai.context.model.MessagePartEvent;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

/**
 * AI大模型回复的流式消息块消费者
 */
public class AIMessagePartConsumer implements Consumer<String> {
    private final LinkedBlockingQueue<MessagePartEvent> messageQueue;

    public AIMessagePartConsumer(LinkedBlockingQueue<MessagePartEvent> messageQueue) {
        this.messageQueue = messageQueue;
    }

    /**
     * 消费一块流式消息，将其放入阻塞队列中
     *
     * @param s 一块流式消息
     */
    @Override
    public void accept(String s) {
        messageQueue.offer(MessagePartEvent.normalEvent(s));
    }

    /**
     * 将当前消费者和另一个消费者串行执行
     *
     * @param after 下一个消费者
     * @return 串行执行的两个消费者组合
     */
    @Override
    public Consumer<String> andThen(Consumer<? super String> after) {
        return Consumer.super.andThen(after);
    }
}
