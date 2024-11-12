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

package com.tencent.bk.job.file.worker.state.event;

import com.tencent.bk.job.common.tracing.util.SpanUtil;
import com.tencent.bk.job.file.worker.state.event.handler.DefaultEventHandler;
import com.tencent.bk.job.file.worker.state.event.handler.EventHandler;
import com.tencent.bk.job.file.worker.state.event.handler.HeartBeatEventHandler;
import com.tencent.bk.job.file.worker.state.event.handler.OffLineEventHandler;
import com.tencent.bk.job.file.worker.state.event.handler.WaitAccessEventHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

@Slf4j
@Component
public class WorkerEventDispatcher extends Thread {

    @SuppressWarnings("FieldCanBeLocal")
    private boolean enabled = true;
    /**
     * 日志调用链tracer
     */
    private final Tracer tracer;
    private BlockingQueue<WorkerEvent> eventQueue;
    private static final Map<WorkerActionEnum, EventHandler> handlerMap = new HashMap<>();
    private static final EventHandler defaultHandler = new DefaultEventHandler();

    @Autowired
    public WorkerEventDispatcher(Tracer tracer,
                                 WaitAccessEventHandler waitAccessEventHandler,
                                 HeartBeatEventHandler heartBeatEventHandler,
                                 OffLineEventHandler offLineEventHandler) {
        this.tracer = tracer;
        handlerMap.put(WorkerActionEnum.WAIT_ACCESS_READY, waitAccessEventHandler);
        handlerMap.put(WorkerActionEnum.HEART_BEAT, heartBeatEventHandler);
        handlerMap.put(WorkerActionEnum.OFF_LINE, offLineEventHandler);
    }

    public void initQueue(BlockingQueue<WorkerEvent> eventQueue) {
        this.eventQueue = eventQueue;
    }

    @Override
    public void run() {
        while (enabled) {
            WorkerEvent event;
            try {
                event = eventQueue.take();
                dispatchEventWithTrace(event);
            } catch (InterruptedException e) {
                log.warn("queue.take interrupted", e);
            } catch (Throwable t) {
                log.error("Fail to handleEventWithTrace", t);
            }
        }
    }

    private void dispatchEventWithTrace(WorkerEvent event) {
        Span span = buildSpan(event);
        try (Tracer.SpanInScope ignored = this.tracer.withSpan(span.start())) {
            dispatchEvent(event);
        } catch (Throwable t) {
            span.error(t);
            log.warn("Fail to handlePropChangeEvent:" + event, t);
        } finally {
            span.end();
        }
    }

    private void dispatchEvent(WorkerEvent event) {
        EventHandler handler = handlerMap.getOrDefault(event.getAction(), defaultHandler);
        handler.handleEvent(event);
    }

    private Span buildSpan(WorkerEvent event) {
        return SpanUtil.buildNewSpan(this.tracer, event.getAction().name());
    }
}
