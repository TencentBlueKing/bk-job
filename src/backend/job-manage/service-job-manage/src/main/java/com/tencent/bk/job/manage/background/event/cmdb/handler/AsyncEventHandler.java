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

package com.tencent.bk.job.manage.background.event.cmdb.handler;

import com.tencent.bk.job.common.cc.model.result.ResourceEvent;
import com.tencent.bk.job.common.tracing.util.SpanUtil;
import com.tencent.bk.job.manage.metrics.CmdbEventSampler;
import com.tencent.bk.job.manage.metrics.MetricsConstants;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;

import java.util.concurrent.BlockingQueue;

/**
 * 在异步线程中处理事件的处理器
 *
 * @param <T> 事件类型
 */
@Slf4j
public abstract class AsyncEventHandler<T> extends Thread implements CmdbEventHandler<T> {

    /**
     * 事件队列
     */
    protected final BlockingQueue<ResourceEvent<T>> queue;
    /**
     * 日志调用链tracer
     */
    private final Tracer tracer;
    /**
     * CMDB事件指标数据采样器
     */
    private final CmdbEventSampler cmdbEventSampler;
    /**
     * 租户ID
     */
    protected final String tenantId;
    /**
     * 当前事件处理是否活跃
     */
    protected boolean active = true;

    public AsyncEventHandler(BlockingQueue<ResourceEvent<T>> queue,
                             Tracer tracer,
                             CmdbEventSampler cmdbEventSampler,
                             String tenantId) {
        this.queue = queue;
        this.tracer = tracer;
        this.cmdbEventSampler = cmdbEventSampler;
        this.tenantId = tenantId;
    }

    /**
     * 实际处理事件
     *
     * @param event 事件
     */
    abstract void handleEventInternal(ResourceEvent<T> event);

    /**
     * 获取当前事件处理器在事件处理指标维度上的额外标签
     *
     * @return 维度标签
     */
    abstract Iterable<Tag> getEventHandleExtraTags();

    @Override
    public void handleEvent(ResourceEvent<T> event) {
        addEventToQueue(event);
    }

    /**
     * 将事件添加至队列等候处理
     *
     * @param event 事件
     */
    private void addEventToQueue(ResourceEvent<T> event) {
        try {
            this.queue.add(event);
        } catch (Exception e) {
            // 如果队列已满或其他异常产生，则丢弃当前事件，避免大量事件持续阻塞
            log.error("Fail to commitEvent:" + event, e);
        }
    }

    @Override
    public void run() {
        while (active) {
            ResourceEvent<T> event;
            try {
                event = queue.take();
                handleEventWithTrace(event);
            } catch (InterruptedException e) {
                log.warn("queue.take interrupted", e);
            } catch (Throwable t) {
                log.error("Fail to handleEventWithTrace", t);
            }
        }
    }

    /**
     * 处理事件并记录耗时Trace数据
     *
     * @param event 事件
     */
    private void handleEventWithTrace(ResourceEvent<T> event) {
        String eventHandleResult = MetricsConstants.TAG_VALUE_CMDB_EVENT_HANDLE_RESULT_SUCCESS;
        Span span = SpanUtil.buildNewSpan(tracer, "handleEvent");
        try (Tracer.SpanInScope ignored = tracer.withSpan(span.start())) {
            handleEventInternal(event);
        } catch (Throwable t) {
            span.error(t);
            eventHandleResult = MetricsConstants.TAG_VALUE_CMDB_EVENT_HANDLE_RESULT_FAILED;
            log.warn("Fail to handleEvent:" + event, t);
        } finally {
            span.end();
            long timeConsuming = System.currentTimeMillis() - event.getCreateTime();
            cmdbEventSampler.recordEventHandleTimeConsuming(timeConsuming, buildEventHandleTimeTags(eventHandleResult));
        }
    }

    /**
     * 构建事件处理耗时指标数据维度标签
     *
     * @param eventHandleResult 事件处理结果
     * @return 维度标签
     */
    private Tags buildEventHandleTimeTags(String eventHandleResult) {
        Tags tags = Tags.of(MetricsConstants.TAG_KEY_CMDB_EVENT_HANDLE_RESULT, eventHandleResult);
        Iterable<Tag> extraTags = getEventHandleExtraTags();
        if (extraTags != null) {
            tags = Tags.concat(tags, extraTags);
        }
        return tags;
    }

    /**
     * 关闭事件处理器
     */
    @Override
    public void close() {
        active = false;
        this.interrupt();
    }
}
