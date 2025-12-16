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

package com.tencent.bk.job.manage.background.event.cmdb.watcher;

import com.tencent.bk.job.common.cc.model.result.HostEventDetail;
import com.tencent.bk.job.common.cc.model.result.ResourceWatchResult;
import com.tencent.bk.job.common.cc.sdk.IBizCmdbClient;
import com.tencent.bk.job.common.tenant.TenantService;
import com.tencent.bk.job.manage.api.common.constants.EventWatchTaskTypeEnum;
import com.tencent.bk.job.manage.background.event.cmdb.CmdbEventCursorManager;
import com.tencent.bk.job.manage.background.event.cmdb.handler.CmdbEventHandler;
import com.tencent.bk.job.manage.background.ha.TaskEntity;
import com.tencent.bk.job.manage.config.JobManageConfig;
import com.tencent.bk.job.manage.metrics.CmdbEventSampler;
import com.tencent.bk.job.manage.metrics.MetricsConstants;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 主机事件监听器
 */
@Slf4j
public class HostEventWatcher extends AbstractCmdbResourceEventWatcher<HostEventDetail> {

    private final IBizCmdbClient bizCmdbClient;
    private final CmdbEventHandler<HostEventDetail> hostEventHandler;

    public HostEventWatcher(RedisTemplate<String, String> redisTemplate,
                            Tracer tracer,
                            CmdbEventSampler cmdbEventSampler,
                            IBizCmdbClient bizCmdbClient,
                            TenantService tenantService,
                            CmdbEventCursorManager cmdbEventCursorManager,
                            CmdbEventHandler<HostEventDetail> hostEventHandler,
                            String tenantId) {
        super(tenantId, "host", redisTemplate,
            tenantService, tracer, cmdbEventSampler, cmdbEventCursorManager, hostEventHandler);
        this.bizCmdbClient = bizCmdbClient;
        this.hostEventHandler = hostEventHandler;
    }

    @Override
    protected ResourceWatchResult<HostEventDetail> fetchEventsByCursor(String startCursor) {
        return bizCmdbClient.getHostEvents(tenantId, null, startCursor);
    }

    @Override
    protected ResourceWatchResult<HostEventDetail> fetchEventsByStartTime(Long startTime) {
        return bizCmdbClient.getHostEvents(tenantId, startTime, null);
    }

    @Override
    protected Tags getEventMetricTags() {
        return Tags.of(MetricsConstants.TAG_KEY_CMDB_EVENT_TYPE, MetricsConstants.TAG_VALUE_CMDB_EVENT_TYPE_HOST);
    }

    @Override
    public String getUniqueCode() {
        return getTaskEntity().getUniqueCode();
    }

    @Override
    public TaskEntity getTaskEntity() {
        return new TaskEntity(EventWatchTaskTypeEnum.WATCH_HOST, getTenantId());
    }

    @Override
    public String getTenantId() {
        return tenantId;
    }

    /**
     * 计算当前Watcher与对应Handler的资源总消耗
     *
     * @return 资源消耗值
     */
    @Override
    public int getResourceCost() {
        return resourceCostForWatcher() + hostEventHandler.getExtraThreadNum();
    }

    /**
     * 计算Watcher自身的资源消耗
     *
     * @return 资源消耗值
     */
    public static int resourceCostForWatcher() {
        return SINGLE_WATCHER_THREAD_NUM;
    }

    /**
     * 从监听器类型层面计算一个Watcher与对应Handler的资源总消耗
     *
     * @return 资源消耗值
     */
    public static int resourceCostForWatcherAndHandler(JobManageConfig jobManageConfig) {
        return resourceCostForWatcher() + jobManageConfig.getHostEventHandlerNum();
    }

    @Override
    public void shutdownGracefully() {
        super.shutdownGracefully();
    }
}
