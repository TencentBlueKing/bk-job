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

package com.tencent.bk.job.manage.background.event.cmdb;

import com.tencent.bk.job.common.cc.model.result.HostRelationEventDetail;
import com.tencent.bk.job.common.cc.model.result.ResourceEvent;
import com.tencent.bk.job.common.cc.model.result.ResourceWatchResult;
import com.tencent.bk.job.common.cc.sdk.IBizCmdbClient;
import com.tencent.bk.job.common.tenant.TenantService;
import com.tencent.bk.job.manage.background.ha.BackGroundTaskCode;
import com.tencent.bk.job.manage.background.ha.TaskEntity;
import com.tencent.bk.job.manage.dao.HostTopoDAO;
import com.tencent.bk.job.manage.dao.NoTenantHostDAO;
import com.tencent.bk.job.manage.manager.host.HostCache;
import com.tencent.bk.job.manage.metrics.CmdbEventSampler;
import com.tencent.bk.job.manage.metrics.MetricsConstants;
import com.tencent.bk.job.manage.service.ApplicationService;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class TenantHostRelationEventWatcher extends AbstractCmdbResourceEventWatcher<HostRelationEventDetail> {

    /**
     * 日志调用链tracer
     */
    private final Tracer tracer;
    private final CmdbEventSampler cmdbEventSampler;
    private final IBizCmdbClient bizCmdbClient;
    private final ApplicationService applicationService;
    private final NoTenantHostDAO noTenantHostDAO;
    private final HostTopoDAO hostTopoDAO;
    private final HostCache hostCache;

    private final HostRelationEventHandler eventsHandler;
    private final BlockingQueue<ResourceEvent<HostRelationEventDetail>> eventQueue =
        new LinkedBlockingQueue<>(10000);

    private final AtomicBoolean hostRelationWatchFlag = new AtomicBoolean(true);

    public TenantHostRelationEventWatcher(RedisTemplate<String, String> redisTemplate,
                                          Tracer tracer,
                                          CmdbEventSampler cmdbEventSampler,
                                          IBizCmdbClient bizCmdbClient,
                                          ApplicationService applicationService,
                                          NoTenantHostDAO noTenantHostDAO,
                                          HostTopoDAO hostTopoDAO,
                                          HostCache hostCache,
                                          TenantService tenantService,
                                          String tenantId) {
        super(tenantId, "hostRelation", redisTemplate, tenantService, tracer, cmdbEventSampler);
        this.tracer = tracer;
        this.cmdbEventSampler = cmdbEventSampler;
        this.bizCmdbClient = bizCmdbClient;
        this.applicationService = applicationService;
        this.noTenantHostDAO = noTenantHostDAO;
        this.hostTopoDAO = hostTopoDAO;
        this.hostCache = hostCache;
        this.eventsHandler = buildHostRelationEventHandler();
        this.eventsHandler.setName(getName() + ":Handler");
    }

    private HostRelationEventHandler buildHostRelationEventHandler() {
        return new HostRelationEventHandler(
            tracer,
            cmdbEventSampler,
            eventQueue,
            applicationService,
            noTenantHostDAO,
            hostTopoDAO,
            hostCache,
            tenantId
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
        return bizCmdbClient.getHostRelationEvents(tenantId, null, startCursor);
    }

    @Override
    protected ResourceWatchResult<HostRelationEventDetail> fetchEventsByStartTime(Long startTime) {
        return bizCmdbClient.getHostRelationEvents(tenantId, startTime, null);
    }

    @Override
    public void handleEvent(ResourceEvent<HostRelationEventDetail> event) {
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
        eventsHandler.commitEvent(event);
    }

    @Override
    public String getUniqueCode() {
        return getTaskEntity().getUniqueCode();
    }

    @Override
    public TaskEntity getTaskEntity() {
        return new TaskEntity(BackGroundTaskCode.WATCH_HOST_RELATION, getTenantId());
    }

    @Override
    public String getTenantId() {
        return tenantId;
    }


    @Override
    public int getResourceCost() {
        return resourceCostForWatcherAndHandler();
    }

    /**
     * 计算Watcher与对应Handler的资源总消耗
     *
     * @return 资源消耗值
     */
    public static int resourceCostForWatcherAndHandler() {
        return resourceCostForWatcher() + HostRelationEventHandler.SINGLE_HANDLER_THREAD_RESOURCE_COST;
    }

    /**
     * 计算Watcher自身的资源消耗
     *
     * @return 资源消耗值
     */
    public static int resourceCostForWatcher() {
        return SINGLE_WATCHER_THREAD_RESOURCE_COST;
    }

    @Override
    public void shutdownGracefully() {
        super.shutdownGracefully();
        eventsHandler.close();
    }
}
