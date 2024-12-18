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
import com.tencent.bk.job.analysis.service.ai.context.model.AsyncConsumerAndStreamingResponseBodyPair;
import com.tencent.bk.job.analysis.service.ai.context.model.MessagePartEvent;
import com.tencent.bk.job.analysis.util.ai.AIAnswerUtil;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.util.TimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * AI回答流式响应同步器
 * 使用阻塞队列构建互相绑定的异步消费者与流式响应体组合，消费者从流式数据源读取到数据后将其写入到阻塞队列中并提供给流式响应体进行读取输出
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
     * 构建异步消费者与流式响应体组合
     *
     * @return 异步消费者与流式响应体组合
     */
    public AsyncConsumerAndStreamingResponseBodyPair buildAsyncConsumerAndStreamingResponseBodyPair() {
        StreamingResponseBody streamingResponseBody = outputStream -> {
            while (!isFinished.get()) {
                try {
                    MessagePartEvent event = messageQueue.poll(90, TimeUnit.SECONDS);
                    if (event == null) {
                        Response<AIAnswer> respBody =
                            Response.buildCommonFailResp(ErrorCode.BK_OPEN_AI_API_DATA_TIMEOUT);
                        respBody.setData(AIAnswer.failAnswer(respBody.getErrorMsg(), respBody.getErrorMsg()));
                        AIAnswerUtil.setRequestIdAndWriteResp(outputStream, respBody);
                        break;
                    }
                    if (event.isEnd()) {
                        Throwable throwable = event.getThrowable();
                        if (throwable != null) {
                            if (throwable instanceof CancellationException) {
                                break;
                            }
                            log.warn("Receive end event with throwable", throwable);
                            Response<AIAnswer> respBody =
                                Response.buildCommonFailResp(ErrorCode.BK_OPEN_AI_API_DATA_ERROR);
                            respBody.setData(AIAnswer.failAnswer(respBody.getErrorMsg(), throwable.getMessage()));
                            AIAnswerUtil.setRequestIdAndWriteResp(outputStream, respBody);
                        }
                        break;
                    }
                    String partMessage = event.getMessagePart();
                    Response<AIAnswer> respBody = Response.buildSuccessResp(AIAnswer.successAnswer(partMessage));
                    AIAnswerUtil.setRequestIdAndWriteResp(outputStream, respBody);
                    if (log.isDebugEnabled()) {
                        log.debug(
                            "partMessage={}, time={}, delay={}ms",
                            partMessage,
                            TimeUtil.formatTime(event.getTimeMills(), "yyyy-MM-dd HH:mm:ss.SSS"),
                            System.currentTimeMillis() - event.getTimeMills()
                        );
                    }
                } catch (InterruptedException e) {
                    log.debug("Interrupted when take message from queue", e);
                } catch (IOException e) {
                    log.error("Write resp to output stream failed", e);
                }
            }
            outputStream.close();
        };
        Consumer<String> partialRespConsumer = new AIMessagePartConsumer(messageQueue);
        return new AsyncConsumerAndStreamingResponseBodyPair(partialRespConsumer, streamingResponseBody);
    }

    /**
     * 触发结束事件，消费者读取完数据后，通知流式响应体做出相应的停止输出动作并清理
     *
     * @param throwable 异常
     */
    public void triggerEndEvent(Throwable throwable) {
        isFinished.set(true);
        messageQueue.offer(MessagePartEvent.endEvent(throwable));
    }
}
