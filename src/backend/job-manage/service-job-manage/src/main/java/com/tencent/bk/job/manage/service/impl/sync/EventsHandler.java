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

package com.tencent.bk.job.manage.service.impl.sync;

import com.tencent.bk.job.common.cc.model.result.ResourceEvent;
import com.tencent.bk.job.common.tracing.util.SpanUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;

import java.util.concurrent.BlockingQueue;

@Slf4j
public abstract class EventsHandler<T> extends Thread {

    /**
     * 日志调用链tracer
     */
    private final Tracer tracer;
    protected boolean enabled = true;
    BlockingQueue<ResourceEvent<T>> queue;
    Long bizId = null;

    public EventsHandler(BlockingQueue<ResourceEvent<T>> queue, Tracer tracer) {
        this.queue = queue;
        this.tracer = tracer;
    }

    public Long getBizId() {
        return bizId;
    }

    public void commitEvent(Long bizId, ResourceEvent<T> event) {
        try {
            boolean result = this.queue.add(event);
            if (!result) {
                log.warn("Fail to commitEvent:{}", event);
            } else {
                this.bizId = bizId;
            }
        } catch (Exception e) {
            log.warn("Fail to commitEvent:" + event, e);
        }
    }

    abstract void handleEvent(ResourceEvent<T> event);

    abstract String getSpanName();

    void handleEventWithTrace(ResourceEvent<T> event) {
        Span span = buildSpan();
        try (Tracer.SpanInScope ignored = this.tracer.withSpan(span.start())) {
            handleEvent(event);
        } catch (Exception e) {
            span.error(e);
            throw e;
        } finally {
            span.end();
        }
    }

    private Span buildSpan() {
        return SpanUtil.buildNewSpan(this.tracer, getSpanName());
    }

    @Override
    public void run() {
        while (enabled) {
            ResourceEvent<T> event = null;
            try {
                event = queue.take();
                handleEventWithTrace(event);
            } catch (InterruptedException e) {
                log.warn("queue.take interrupted", e);
            } catch (Throwable t) {
                log.warn("Fail to handleOneEvent:" + event, t);
            } finally {
                if (queue.size() == 0) {
                    this.bizId = null;
                }
            }
        }
    }
}
