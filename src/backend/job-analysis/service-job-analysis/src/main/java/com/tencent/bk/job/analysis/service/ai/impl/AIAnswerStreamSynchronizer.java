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

import com.tencent.bk.job.analysis.model.web.resp.AIAnswer;
import com.tencent.bk.job.analysis.service.ai.context.model.AsyncConsumerAndProducerPair;
import com.tencent.bk.job.analysis.service.ai.context.model.MessagePartEvent;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.util.json.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * AI回答流式响应同步器
 * 使用阻塞队列构建异步消费者与生产者组合，消费者读取到数据后立即提供给生产者进行输出
 */
@Slf4j
public class AIAnswerStreamSynchronizer {

    private final AtomicBoolean isFinished = new AtomicBoolean(false);
    private final LinkedBlockingQueue<MessagePartEvent> messageQueue;

    /**
     * 构造函数
     *
     * @param capacity 可缓存于内存的消息事件量
     */
    public AIAnswerStreamSynchronizer(int capacity) {
        messageQueue = new LinkedBlockingQueue<>(capacity);
    }

    /**
     * 构建异步消费者与生产者组合
     *
     * @return 异步消费者与生产者组合
     */
    public AsyncConsumerAndProducerPair buildAsyncConsumerAndProducerPair() {
        StreamingResponseBody streamingResponseBody = outputStream -> {
            while (!isFinished.get()) {
                try {
                    MessagePartEvent event = messageQueue.take();
                    if (event.isEnd()) {
                        break;
                    }
                    String partMessage = event.getMessagePart();
                    Response<AIAnswer> respBody = Response.buildSuccessResp(AIAnswer.successAnswer(partMessage));
                    String message = JsonUtils.toJson(respBody) + "\n";
                    outputStream.write(message.getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();
                } catch (InterruptedException e) {
                    log.debug("Interrupted when take message from queue", e);
                } catch (IOException e) {
                    log.error("Write resp to output stream failed", e);
                }
            }
            outputStream.close();
        };
        Consumer<String> partialRespConsumer =
            messagePart -> messageQueue.offer(MessagePartEvent.normalEvent(messagePart));
        return new AsyncConsumerAndProducerPair(partialRespConsumer, streamingResponseBody);
    }

    /**
     * 触发结束事件，消费者读取完数据后，触发生产者停止生产并清理
     */
    public void triggerEndEvent() {
        isFinished.set(true);
        messageQueue.offer(MessagePartEvent.endEvent());
    }
}
