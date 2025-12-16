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

import com.tencent.bk.job.common.cc.model.result.HostEventDetail;
import com.tencent.bk.job.common.cc.model.result.ResourceEvent;
import com.tencent.bk.job.manage.background.event.cmdb.handler.factory.EventHandlerFactory;
import com.tencent.bk.job.manage.config.JobManageConfig;
import com.tencent.bk.job.manage.metrics.CmdbEventSampler;
import com.tencent.bk.job.manage.metrics.MetricsConstants;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 主机事件并发处理器，内部开启多个异步主机事件处理器（HostEventHandler）并发处理主机事件；
 * 同一主机的多个事件处理时需要保持事件产生时的先后顺序，采用一致性路由算法将同一主机的多个事件路由至同一个处理器。
 */
@Slf4j
public class ConcurrentHostEventHandler implements CmdbEventHandler<HostEventDetail> {

    /**
     * CMDB事件处理器工厂
     */
    private final EventHandlerFactory eventHandlerFactory;
    /**
     * CMDB事件指标数据采样器
     */
    private final CmdbEventSampler cmdbEventSampler;
    /**
     * 租户ID
     */
    private final String tenantId;
    /**
     * 内部主机事件处理器数量
     */
    private final int hostEventsHandlerNum;
    /**
     * 内部主机事件处理器队列大小
     */
    private final int hostEventQueueSize;
    /**
     * 内部主机事件处理器列表
     */
    private final List<AsyncEventHandler<HostEventDetail>> hostEventsHandlers = new ArrayList<>();
    /**
     * 内部主机事件处理器使用的事件队列列表
     */
    private final List<BlockingQueue<ResourceEvent<HostEventDetail>>> hostEventQueues = new ArrayList<>();

    public ConcurrentHostEventHandler(EventHandlerFactory eventHandlerFactory,
                                      CmdbEventSampler cmdbEventSampler,
                                      JobManageConfig jobManageConfig,
                                      String tenantId) {
        this.eventHandlerFactory = eventHandlerFactory;
        this.cmdbEventSampler = cmdbEventSampler;
        this.hostEventsHandlerNum = jobManageConfig.getHostEventHandlerNum();
        this.hostEventQueueSize = jobManageConfig.getHostEventQueueSize();
        this.tenantId = tenantId;
        initHostEventHandlers();
    }

    @Override
    public void handleEvent(ResourceEvent<HostEventDetail> event) {
        dispatchEventToHandler(event);
    }

    @Override
    public int getExtraThreadNum() {
        // 每个主机事件处理器使用一个独立线程
        return hostEventsHandlerNum;
    }

    @Override
    public void close() {
        closeAllHostEventHandlers();
    }

    /**
     * 初始化内部的多个主机事件处理器
     */
    private void initHostEventHandlers() {
        initHostEventQueues();
        for (int i = 0; i < hostEventsHandlerNum; i++) {
            BlockingQueue<ResourceEvent<HostEventDetail>> eventQueue = hostEventQueues.get(i);
            String handlerName = "HostEventHandler-" + i;
            cmdbEventSampler.registerEventQueueToGauge(eventQueue, buildHostEventHandlerTags(handlerName));
            AsyncEventHandler<HostEventDetail> eventsHandler = eventHandlerFactory
                .createHostEventHandler(tenantId, eventQueue);
            String threadName = "[" + eventsHandler.getId() + "]-" + handlerName;
            eventsHandler.setName(threadName);
            hostEventsHandlers.add(eventsHandler);
            eventsHandler.start();
        }
    }

    /**
     * 初始化主机事件处理器需要使用的事件队列
     */
    private void initHostEventQueues() {
        for (int i = 0; i < hostEventsHandlerNum; i++) {
            BlockingQueue<ResourceEvent<HostEventDetail>> queue = new LinkedBlockingQueue<>(hostEventQueueSize);
            hostEventQueues.add(queue);
        }
    }

    /**
     * 构建主机事件处理器的指标数据维度标签
     *
     * @param handlerName 处理器名称
     * @return 维度标签
     */
    private Iterable<Tag> buildHostEventHandlerTags(String handlerName) {
        return Tags.of(
            MetricsConstants.TAG_KEY_CMDB_EVENT_TYPE, MetricsConstants.TAG_VALUE_CMDB_EVENT_TYPE_HOST,
            MetricsConstants.TAG_KEY_CMDB_HOST_EVENT_HANDLER_NAME, handlerName
        );
    }

    /**
     * 将主机事件分发给一个处理器进行处理
     *
     * @param event 主机事件
     */
    private void dispatchEventToHandler(ResourceEvent<HostEventDetail> event) {
        AsyncEventHandler<HostEventDetail> eventsHandler = chooseHandler(event.getDetail().getHostId());
        eventsHandler.handleEvent(event);
    }

    /**
     * 根据主机ID选择一个异步主机事件处理器
     *
     * @param hostId 主机ID
     * @return 异步主机事件处理器
     */
    private AsyncEventHandler<HostEventDetail> chooseHandler(Long hostId) {
        // 保证同一主机的多个事件被分配到同一个Handler
        int index = (int) (hostId % hostEventsHandlerNum);
        return hostEventsHandlers.get(index);
    }

    /**
     * 关闭内部所有主机事件处理器
     */
    private void closeAllHostEventHandlers() {
        for (AsyncEventHandler<HostEventDetail> handler : hostEventsHandlers) {
            handler.close();
        }
    }
}
