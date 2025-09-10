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

import com.tencent.bk.job.common.cc.model.req.ResourceWatchReq;
import com.tencent.bk.job.common.cc.model.result.BizEventDetail;
import com.tencent.bk.job.common.cc.model.result.ResourceEvent;
import com.tencent.bk.job.common.cc.model.result.ResourceWatchResult;
import com.tencent.bk.job.common.cc.sdk.IBizCmdbClient;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.tenant.TenantService;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.background.ha.BackGroundTaskCode;
import com.tencent.bk.job.manage.background.ha.TaskEntity;
import com.tencent.bk.job.manage.metrics.CmdbEventSampler;
import com.tencent.bk.job.manage.metrics.MetricsConstants;
import com.tencent.bk.job.manage.service.ApplicationService;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class TenantBizEventWatcher extends AbstractCmdbResourceEventWatcher<BizEventDetail> {

    private final IBizCmdbClient bizCmdbClient;
    private final ApplicationService applicationService;

    private final AtomicBoolean bizWatchFlag = new AtomicBoolean(true);

    public TenantBizEventWatcher(RedisTemplate<String, String> redisTemplate,
                                 Tracer tracer,
                                 CmdbEventSampler cmdbEventSampler,
                                 IBizCmdbClient bizCmdbClient,
                                 ApplicationService applicationService,
                                 TenantService tenantService,
                                 String tenantId) {
        super(tenantId, "biz", redisTemplate, tenantService, tracer, cmdbEventSampler);
        this.bizCmdbClient = bizCmdbClient;
        this.applicationService = applicationService;
    }

    public void setWatchFlag(boolean value) {
        bizWatchFlag.set(value);
    }

    /**
     * 事件监听开关
     */
    protected boolean isWatchingEnabled() {
        return bizWatchFlag.get();
    }

    @Override
    protected ResourceWatchResult<BizEventDetail> fetchEventsByCursor(String startCursor) {
        return bizCmdbClient.getAppEvents(tenantId, null, startCursor);
    }

    @Override
    protected ResourceWatchResult<BizEventDetail> fetchEventsByStartTime(Long startTime) {
        return bizCmdbClient.getAppEvents(tenantId, startTime, null);
    }

    @Override
    public void handleEvent(ResourceEvent<BizEventDetail> event) {
        ApplicationDTO newestApp = event.getDetail().toAppInfoDTO(tenantId);
        ApplicationDTO cachedApp = null;
        try {
            cachedApp = applicationService.getAppByScope(newestApp.getScope());
        } catch (NotFoundException e) {
            log.debug("cannot find app by scope:{}, need to create", newestApp.getScope());
        }
        String eventType = event.getEventType();
        switch (eventType) {
            case ResourceWatchReq.EVENT_TYPE_CREATE:
            case ResourceWatchReq.EVENT_TYPE_UPDATE:
                handleCreateOrUpdateEvent(event, cachedApp, newestApp);
                break;
            case ResourceWatchReq.EVENT_TYPE_DELETE:
                if (cachedApp != null) {
                    applicationService.deleteApp(cachedApp.getId());
                } else {
                    log.info("ignore delete event of app not exist:{}", event);
                }
                break;
            default:
                break;
        }
    }

    private void handleCreateOrUpdateEvent(ResourceEvent<BizEventDetail> event,
                                           ApplicationDTO cachedApp,
                                           ApplicationDTO newestApp) {
        try {
            if (cachedApp != null) {
                updateBizProps(cachedApp, newestApp);
                applicationService.updateApp(cachedApp);
            } else {
                if (ResourceWatchReq.EVENT_TYPE_CREATE.equals(event.getEventType())) {
                    tryToCreateApp(newestApp);
                } else {
                    // 不存在的业务（已归档）的Update事件，忽略
                    if (log.isDebugEnabled()) {
                        log.debug("ignore update event of invalid app:{}", JsonUtils.toJson(event));
                    }
                }
            }
        } catch (Throwable t) {
            log.error("handle app event fail", t);
        }
    }

    @Override
    protected Tags getEventMetricTags() {
        return Tags.of(MetricsConstants.TAG_KEY_CMDB_EVENT_TYPE,
            MetricsConstants.TAG_VALUE_CMDB_EVENT_TYPE_BIZ);
    }

    private void tryToCreateApp(ApplicationDTO app) {
        try {
            applicationService.createApp(app);
        } catch (Exception e) {
            log.error("create app fail:appInfo=" + app, e);
        }
    }

    private void updateBizProps(ApplicationDTO originApp, ApplicationDTO updateApp) {
        originApp.setName(updateApp.getName());
        originApp.setBkSupplierAccount(updateApp.getBkSupplierAccount());
        originApp.setLanguage(updateApp.getLanguage());
        originApp.setTimeZone(updateApp.getTimeZone());
    }

    @Override
    public String getUniqueCode() {
        return getTaskEntity().getUniqueCode();
    }

    @Override
    public TaskEntity getTaskEntity() {
        return new TaskEntity(BackGroundTaskCode.WATCH_BIZ, getTenantId());
    }

    @Override
    public String getTenantId() {
        return tenantId;
    }

    /**
     * 计算资源总消耗，监听线程自己处理事件，资源消耗就是Watcher自身的资源消耗值
     *
     * @return 资源消耗值
     */
    @Override
    public int getResourceCost() {
        return resourceCostForWatcher();
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
    }
}
