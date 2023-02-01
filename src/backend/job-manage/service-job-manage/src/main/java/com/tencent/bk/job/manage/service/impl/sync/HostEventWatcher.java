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

import com.tencent.bk.job.common.cc.model.result.HostEventDetail;
import com.tencent.bk.job.common.cc.model.result.ResourceEvent;
import com.tencent.bk.job.common.cc.model.result.ResourceWatchResult;
import com.tencent.bk.job.common.cc.sdk.BizCmdbClient;
import com.tencent.bk.job.common.gse.service.QueryAgentStatusClient;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.manage.config.JobManageConfig;
import com.tencent.bk.job.manage.dao.ApplicationHostDAO;
import com.tencent.bk.job.manage.manager.host.HostCache;
import com.tencent.bk.job.manage.metrics.CmdbEventSampler;
import com.tencent.bk.job.manage.metrics.MetricsConstants;
import com.tencent.bk.job.manage.service.ApplicationService;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class HostEventWatcher extends AbstractCmdbResourceEventWatcher<HostEventDetail> {

    /**
     * 日志调用链tracer
     */
    private final Tracer tracer;
    private final CmdbEventSampler cmdbEventSampler;
    private final BizCmdbClient bizCmdbClient;
    private final ApplicationService applicationService;
    private final ApplicationHostDAO applicationHostDAO;
    private final QueryAgentStatusClient queryAgentStatusClient;
    private final HostCache hostCache;

    private final AtomicBoolean hostWatchFlag = new AtomicBoolean(true);
    private final int eventsHandlerNum;
    private final List<HostEventHandler> eventsHandlers = new ArrayList<>();
    private final List<BlockingQueue<ResourceEvent<HostEventDetail>>> hostEventQueues = new ArrayList<>();

    @Autowired
    public HostEventWatcher(RedisTemplate<String, String> redisTemplate,
                            Tracer tracer,
                            CmdbEventSampler cmdbEventSampler,
                            BizCmdbClient bizCmdbClient,
                            ApplicationService applicationService,
                            ApplicationHostDAO applicationHostDAO,
                            QueryAgentStatusClient queryAgentStatusClient,
                            HostCache hostCache,
                            JobManageConfig jobManageConfig) {
        super("host", redisTemplate, tracer, cmdbEventSampler);
        this.tracer = tracer;
        this.cmdbEventSampler = cmdbEventSampler;
        this.bizCmdbClient = bizCmdbClient;
        this.applicationService = applicationService;
        this.applicationHostDAO = applicationHostDAO;
        this.queryAgentStatusClient = queryAgentStatusClient;
        this.hostCache = hostCache;
        this.eventsHandlerNum = jobManageConfig.getHostEventHandlerNum();
    }

    @Override
    protected void initBeforeWatch() {
        initHostEventQueues();
        initHostEventHandlers();
        for (HostEventHandler eventsHandler : eventsHandlers) {
            eventsHandler.start();
        }
    }

    /**
     * 事件监听开关
     */
    protected boolean isWatchingEnabled() {
        return hostWatchFlag.get();
    }

    @Override
    protected ResourceWatchResult<HostEventDetail> fetchEventsByCursor(String startCursor) {
        return bizCmdbClient.getHostEvents(null, startCursor);
    }

    @Override
    protected ResourceWatchResult<HostEventDetail> fetchEventsByStartTime(Long startTime) {
        return bizCmdbClient.getHostEvents(startTime, null);
    }

    @Override
    protected void handleEvent(ResourceEvent<HostEventDetail> event) {
        dispatchEventToHandler(event);
    }

    @Override
    protected Tags getEventMetricTags() {
        return Tags.of(MetricsConstants.TAG_KEY_CMDB_EVENT_TYPE, MetricsConstants.TAG_VALUE_CMDB_EVENT_TYPE_HOST);
    }

    public void setWatchFlag(boolean value) {
        hostWatchFlag.set(value);
    }

    private void initHostEventQueues() {
        int hostEventQueueCapacity = 10000;
        for (int i = 0; i < eventsHandlerNum; i++) {
            BlockingQueue<ResourceEvent<HostEventDetail>> queue = new LinkedBlockingQueue<>(hostEventQueueCapacity);
            hostEventQueues.add(queue);
        }
    }

    private void initHostEventHandlers() {
        for (int i = 0; i < eventsHandlerNum; i++) {
            BlockingQueue<ResourceEvent<HostEventDetail>> eventQueue = hostEventQueues.get(i);
            String handlerName = "HostEventHandler-" + i;
            cmdbEventSampler.registerEventQueueToGauge(eventQueue, buildHostEventHandlerTags(handlerName));
            HostEventHandler eventsHandler = buildHostEventHandler(eventQueue);
            String threadName = "[" + eventsHandler.getId() + "]-" + handlerName;
            eventsHandler.setName(threadName);
            eventsHandlers.add(eventsHandler);
        }
    }

    private Iterable<Tag> buildHostEventHandlerTags(String handlerName) {
        return Tags.of(
            MetricsConstants.TAG_KEY_CMDB_EVENT_TYPE, MetricsConstants.TAG_VALUE_CMDB_EVENT_TYPE_HOST,
            MetricsConstants.TAG_KEY_CMDB_HOST_EVENT_HANDLER_NAME, handlerName
        );
    }

    private HostEventHandler buildHostEventHandler(BlockingQueue<ResourceEvent<HostEventDetail>> hostEventQueue) {
        return new HostEventHandler(
            tracer,
            cmdbEventSampler,
            hostEventQueue,
            applicationService,
            applicationHostDAO,
            queryAgentStatusClient,
            hostCache
        );
    }

    private HostEventHandler chooseHandler(Long hostId) {
        // 保证同一主机的多个事件被分配到同一个Handler
        int index = (int) (hostId % eventsHandlerNum);
        return eventsHandlers.get(index);
    }

    private void dispatchEventToHandler(ResourceEvent<HostEventDetail> event) {
        ApplicationHostDTO hostInfoDTO = HostEventDetail.toHostInfoDTO(event.getDetail());
        Long hostId = hostInfoDTO.getHostId();
        ApplicationHostDTO oldHostInfoDTO = applicationHostDAO.getHostById(hostId);
        HostEventHandler eventsHandler = chooseHandler(hostId);
        eventsHandler.commitEvent(oldHostInfoDTO == null ? null : oldHostInfoDTO.getBizId(), event);
    }
}
