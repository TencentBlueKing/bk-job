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

import com.tencent.bk.job.common.cc.model.result.HostRelationEventDetail;
import com.tencent.bk.job.common.cc.model.result.ResourceEvent;
import com.tencent.bk.job.common.cc.model.result.ResourceWatchResult;
import com.tencent.bk.job.common.cc.sdk.BizCmdbClient;
import com.tencent.bk.job.manage.dao.ApplicationHostDAO;
import com.tencent.bk.job.manage.dao.HostTopoDAO;
import com.tencent.bk.job.manage.manager.host.HostCache;
import com.tencent.bk.job.manage.metrics.CmdbEventSampler;
import com.tencent.bk.job.manage.metrics.MetricsConstants;
import com.tencent.bk.job.manage.model.dto.HostTopoDTO;
import com.tencent.bk.job.manage.service.ApplicationService;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class HostRelationEventWatcher extends AbstractCmdbResourceEventWatcher<HostRelationEventDetail> {

    private static final AtomicInteger instanceNum = new AtomicInteger(1);

    /**
     * 日志调用链tracer
     */
    private final Tracer tracer;
    private final CmdbEventSampler cmdbEventSampler;
    private final BizCmdbClient bizCmdbClient;
    private final ApplicationService applicationService;
    private final ApplicationHostDAO applicationHostDAO;
    private final HostTopoDAO hostTopoDAO;
    private final HostCache hostCache;

    private final HostRelationEventHandler eventsHandler;
    private final BlockingQueue<ResourceEvent<HostRelationEventDetail>> eventQueue =
        new LinkedBlockingQueue<>(10000);

    private final AtomicBoolean hostRelationWatchFlag = new AtomicBoolean(true);

    @Autowired
    public HostRelationEventWatcher(RedisTemplate<String, String> redisTemplate,
                                    Tracer tracer,
                                    CmdbEventSampler cmdbEventSampler,
                                    BizCmdbClient bizCmdbClient,
                                    ApplicationService applicationService,
                                    ApplicationHostDAO applicationHostDAO,
                                    HostTopoDAO hostTopoDAO,
                                    HostCache hostCache) {
        super("hostRelation", redisTemplate, tracer, cmdbEventSampler);
        this.tracer = tracer;
        this.cmdbEventSampler = cmdbEventSampler;
        this.bizCmdbClient = bizCmdbClient;
        this.applicationService = applicationService;
        this.applicationHostDAO = applicationHostDAO;
        this.hostTopoDAO = hostTopoDAO;
        this.hostCache = hostCache;
        this.setName("[" + getId() + "]-HostRelationWatchThread-" + instanceNum.getAndIncrement());
        this.eventsHandler = buildHostRelationEventHandler();
        this.eventsHandler.setName("[" + eventsHandler.getId() + "]-HostRelationEventHandler");
    }

    private HostRelationEventHandler buildHostRelationEventHandler() {
        return new HostRelationEventHandler(
            tracer,
            cmdbEventSampler,
            eventQueue,
            applicationService,
            applicationHostDAO,
            hostTopoDAO,
            hostCache
        );
    }

    /**
     * 事件监听开关
     */
    protected boolean isWatchingEnabled() {
        return hostRelationWatchFlag.get();
    }

    @Override
    protected ResourceWatchResult<HostRelationEventDetail> fetchEventsByCursor(String startCursor) {
        return bizCmdbClient.getHostRelationEvents(null, startCursor);
    }

    @Override
    protected ResourceWatchResult<HostRelationEventDetail> fetchEventsByStartTime(Long startTime) {
        return bizCmdbClient.getHostRelationEvents(startTime, null);
    }

    @Override
    protected void handleEvent(ResourceEvent<HostRelationEventDetail> event) {
        dispatchEventToHandler(event);
    }

    @Override
    protected Tags getEventMetricTags() {
        return Tags.of(MetricsConstants.TAG_KEY_CMDB_EVENT_TYPE,
            MetricsConstants.TAG_VALUE_CMDB_EVENT_TYPE_HOST_RELATION);
    }

    @Override
    protected void initBeforeWatch() {
        String handlerName = "HostRelationEventHandler";
        cmdbEventSampler.registerEventQueueToGauge(
            eventQueue,
            buildHostRelationEventHandlerTags(handlerName)
        );
        eventsHandler.start();
    }

    private Iterable<Tag> buildHostRelationEventHandlerTags(String handlerName) {
        return Tags.of(
            MetricsConstants.TAG_KEY_CMDB_EVENT_TYPE, MetricsConstants.TAG_VALUE_CMDB_EVENT_TYPE_HOST_RELATION,
            MetricsConstants.TAG_KEY_CMDB_HOST_EVENT_HANDLER_NAME, handlerName
        );
    }

    public void setWatchFlag(boolean value) {
        hostRelationWatchFlag.set(value);
    }

    private void dispatchEventToHandler(ResourceEvent<HostRelationEventDetail> event) {
        HostTopoDTO hostTopoDTO = HostTopoDTO.fromHostRelationEvent(event.getDetail());
        Long appId = hostTopoDTO.getBizId();
        eventsHandler.commitEvent(appId, event);
    }
}
