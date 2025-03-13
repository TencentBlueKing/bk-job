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

import java.io.OutputStream;
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
    private final LinkedBlockingQueue<MessagePartEvent> messageEventQueue;

    /**
     * 构造函数
     *
     * @param capacity 可缓存于内存的消息事件量
     */
    public AIAnswerStreamSynchronizer(int capacity) {
        messageEventQueue = new LinkedBlockingQueue<>(capacity);
    }

    /**
     * 构建异步消费者与流式响应体组合
     *
     * @return 异步消费者与流式响应体组合
     */
    public AsyncConsumerAndStreamingResponseBodyPair buildAsyncConsumerAndStreamingResponseBodyPair() {
        StreamingResponseBody streamingResponseBody = buildStreamingResponseBody();
        Consumer<String> partialRespConsumer = new AIMessagePartConsumer(messageEventQueue);
        return new AsyncConsumerAndStreamingResponseBodyPair(partialRespConsumer, streamingResponseBody);
    }

    /**
     * 构建当前同步器绑定的流式响应体
     *
     * @return 流式响应体
     */
    private StreamingResponseBody buildStreamingResponseBody() {
        return outputStream -> {
            while (!isFinished.get()) {
                // 从当前同步器的队列中获取一个消息事件并处理
                boolean needContinue = pollEventAndHandle(outputStream);
                if (!needContinue) {
                    break;
                }
            }
            outputStream.close();
        };
    }

    /**
     * 从队列中获取一个消息事件并处理
     *
     * @param outputStream 输出流
     * @return 是否需要继续处理下一个消息事件
     */
    private boolean pollEventAndHandle(OutputStream outputStream) {
        try {
            final int maxWaitSeconds = 90;
            MessagePartEvent event = messageEventQueue.poll(maxWaitSeconds, TimeUnit.SECONDS);
            // 远端流式数据超时，构建对应的错误信息输出至本地输出流
            if (event == null) {
                Response<AIAnswer> respBody =
                    Response.buildCommonFailResp(ErrorCode.BK_OPEN_AI_API_DATA_TIMEOUT);
                respBody.setData(AIAnswer.failAnswer(respBody.getErrorMsg(), respBody.getErrorMsg()));
                AIAnswerUtil.setRequestIdAndWriteResp(outputStream, respBody);
                return false;
            }
            // 收到数据结束事件，结束处理
            if (event.isEnd()) {
                Throwable throwable = event.getThrowable();
                if (throwable != null) {
                    // 用户主动取消导致的异常，直接结束处理即可
                    if (throwable instanceof CancellationException) {
                        return false;
                    }
                    // 其他异常，表明远端流式接口调用失败，构建对应的错误信息输出
                    log.warn("Receive end event with throwable", throwable);
                    Response<AIAnswer> respBody = Response.buildCommonFailResp(ErrorCode.BK_OPEN_AI_API_DATA_ERROR);
                    respBody.setData(AIAnswer.failAnswer(respBody.getErrorMsg(), throwable.getMessage()));
                    AIAnswerUtil.setRequestIdAndWriteResp(outputStream, respBody);
                }
                return false;
            }
            // 收到普通块数据，直接输出至本地输出流
            String partMessage = event.getMessagePart();
            Response<AIAnswer> respBody = Response.buildSuccessResp(AIAnswer.successAnswer(partMessage));
            AIAnswerUtil.setRequestIdAndWriteResp(outputStream, respBody);
            if (log.isDebugEnabled()) {
                // 记录延迟数据至调试日志，便于排查可能出现的性能相关问题
                log.debug(
                    "partMessage={}, time={}, delay={}ms",
                    partMessage,
                    TimeUtil.formatTime(event.getTimeMills(), "yyyy-MM-dd HH:mm:ss.SSS"),
                    System.currentTimeMillis() - event.getTimeMills()
                );
            }
        } catch (InterruptedException e) {
            // 线程被中断，通常在发布变更进程关闭时产生，忽略并记录日志即可
            log.debug("Interrupted when take message from queue", e);
        } catch (Exception e) {
            // 写入本地输出流失败，可能前端主动关闭了连接，忽略并记录日志即可
            log.warn("Write resp to output stream failed", e);
            return false;
        }
        return true;
    }

    /**
     * 触发结束事件，消费者读取完数据后，通知流式响应体做出相应的停止输出动作并清理
     *
     * @param throwable 异常
     */
    public void triggerEndEvent(Throwable throwable) {
        isFinished.set(true);
        messageEventQueue.offer(MessagePartEvent.endEvent(throwable));
    }
}
