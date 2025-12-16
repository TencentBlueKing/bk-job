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

package com.tencent.bk.job.manage.background.event.cmdb.watcher.factory;

import com.tencent.bk.job.common.cc.model.result.BizEventDetail;
import com.tencent.bk.job.common.cc.model.result.BizSetEventDetail;
import com.tencent.bk.job.common.cc.model.result.BizSetRelationEventDetail;
import com.tencent.bk.job.common.cc.model.result.HostEventDetail;
import com.tencent.bk.job.common.cc.model.result.HostRelationEventDetail;
import com.tencent.bk.job.common.cc.sdk.IBizCmdbClient;
import com.tencent.bk.job.common.cc.sdk.IBizSetCmdbClient;
import com.tencent.bk.job.common.tenant.TenantService;
import com.tencent.bk.job.manage.background.event.cmdb.CmdbEventCursorManager;
import com.tencent.bk.job.manage.background.event.cmdb.handler.CmdbEventHandler;
import com.tencent.bk.job.manage.background.event.cmdb.handler.factory.EventHandlerFactory;
import com.tencent.bk.job.manage.background.event.cmdb.watcher.BizEventWatcher;
import com.tencent.bk.job.manage.background.event.cmdb.watcher.BizSetEventWatcher;
import com.tencent.bk.job.manage.background.event.cmdb.watcher.BizSetRelationEventWatcher;
import com.tencent.bk.job.manage.background.event.cmdb.watcher.HostEventWatcher;
import com.tencent.bk.job.manage.background.event.cmdb.watcher.HostRelationEventWatcher;
import com.tencent.bk.job.manage.metrics.CmdbEventSampler;
import com.tencent.bk.job.manage.service.impl.BizSetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 事件监听器工厂，用于创建各种类型的事件监听器
 */
@Service
public class EventWatcherFactory {

    private final RedisTemplate<String, String> redisTemplate;
    /**
     * 日志调用链tracer
     */
    private final Tracer tracer;
    /**
     * CMDB事件指标数据采样器
     */
    private final CmdbEventSampler cmdbEventSampler;
    /**
     * 业务相关的CMDB接口访问客户端
     */
    private final IBizCmdbClient bizCmdbClient;
    /**
     * 业务集相关的CMDB接口访问客户端
     */
    private final IBizSetCmdbClient bizSetCmdbClient;
    /**
     * 业务集服务
     */
    private final BizSetService bizSetService;
    /**
     * 租户服务
     */
    private final TenantService tenantService;
    /**
     * CMDB事件游标管理器
     */
    private final CmdbEventCursorManager cmdbEventCursorManager;
    /**
     * 事件处理器工厂
     */
    private final EventHandlerFactory eventHandlerFactory;

    /**
     * 缓存Map<租户ID，业务事件监听器>
     */
    private final Map<String, BizEventWatcher> bizEventWatcherMap = new ConcurrentHashMap<>();
    /**
     * 缓存Map<租户ID，业务集事件监听器>
     */
    private final Map<String, BizSetEventWatcher> bizSetEventWatcherMap = new ConcurrentHashMap<>();
    /**
     * 缓存Map<租户ID，业务集关系事件监听器>
     */
    private final Map<String, BizSetRelationEventWatcher> bizSetRelationEventWatcherMap = new ConcurrentHashMap<>();
    /**
     * 缓存Map<租户ID，主机事件监听器>
     */
    private final Map<String, HostEventWatcher> hostEventWatcherMap = new ConcurrentHashMap<>();
    /**
     * 缓存Map<租户ID，主机关系事件监听器>
     */
    private final Map<String, HostRelationEventWatcher> hostRelationEventWatcherMap = new ConcurrentHashMap<>();

    @Autowired
    public EventWatcherFactory(RedisTemplate<String, String> redisTemplate,
                               Tracer tracer,
                               CmdbEventSampler cmdbEventSampler,
                               IBizCmdbClient bizCmdbClient,
                               IBizSetCmdbClient bizSetCmdbClient,
                               BizSetService bizSetService,
                               TenantService tenantService,
                               CmdbEventCursorManager cmdbEventCursorManager,
                               EventHandlerFactory eventHandlerFactory) {
        this.redisTemplate = redisTemplate;
        this.tracer = tracer;
        this.cmdbEventSampler = cmdbEventSampler;
        this.bizCmdbClient = bizCmdbClient;
        this.bizSetCmdbClient = bizSetCmdbClient;
        this.bizSetService = bizSetService;
        this.tenantService = tenantService;
        this.cmdbEventCursorManager = cmdbEventCursorManager;
        this.eventHandlerFactory = eventHandlerFactory;
    }

    /**
     * 对指定租户获取现有的或创建一个业务事件监听器
     *
     * @param tenantId 租户ID
     * @return 业务事件监听器
     */
    public BizEventWatcher getOrCreateBizEventWatcher(String tenantId) {
        return bizEventWatcherMap.computeIfAbsent(
            tenantId,
            (inputTenantId) -> {
                CmdbEventHandler<BizEventDetail> bizEventHandler =
                    eventHandlerFactory.getOrCreateBizEventHandler(inputTenantId);
                return new BizEventWatcher(
                    redisTemplate,
                    tracer,
                    cmdbEventSampler,
                    bizCmdbClient,
                    tenantService,
                    cmdbEventCursorManager,
                    bizEventHandler,
                    inputTenantId
                );
            }
        );
    }

    /**
     * 对指定租户获取现有的或创建一个业务集事件监听器
     *
     * @param tenantId 租户ID
     * @return 业务集事件监听器
     */
    public BizSetEventWatcher getOrCreateBizSetEventWatcher(String tenantId) {
        return bizSetEventWatcherMap.computeIfAbsent(
            tenantId,
            (inputTenantId) -> {
                CmdbEventHandler<BizSetEventDetail> bizSetEventHandler =
                    eventHandlerFactory.getOrCreateBizSetEventHandler(inputTenantId);
                return new BizSetEventWatcher(
                    redisTemplate,
                    tracer,
                    cmdbEventSampler,
                    bizSetService,
                    bizSetCmdbClient,
                    tenantService,
                    cmdbEventCursorManager,
                    bizSetEventHandler,
                    inputTenantId
                );
            }
        );
    }

    /**
     * 对指定租户获取现有的或创建一个业务集关系事件监听器
     *
     * @param tenantId 租户ID
     * @return 业务集关系事件监听器
     */
    public BizSetRelationEventWatcher getOrCreateBizSetRelationEventWatcher(String tenantId) {
        return bizSetRelationEventWatcherMap.computeIfAbsent(
            tenantId,
            (inputTenantId) -> {
                CmdbEventHandler<BizSetRelationEventDetail> bizSetRelationEventHandler =
                    eventHandlerFactory.getOrCreateBizSetRelationEventHandler(inputTenantId);
                return new BizSetRelationEventWatcher(
                    redisTemplate,
                    tracer,
                    cmdbEventSampler,
                    bizSetService,
                    bizSetCmdbClient,
                    tenantService,
                    cmdbEventCursorManager,
                    bizSetRelationEventHandler,
                    inputTenantId
                );
            }
        );
    }

    /**
     * 对指定租户获取现有的或创建一个业务集事件监听器
     *
     * @param tenantId 租户ID
     * @return 业务集事件监听器
     */
    public HostEventWatcher getOrCreateHostEventWatcher(String tenantId) {
        return hostEventWatcherMap.computeIfAbsent(
            tenantId,
            (inputTenantId) -> {
                CmdbEventHandler<HostEventDetail> hostEventHandler =
                    eventHandlerFactory.getOrCreateConcurrentHostEventHandler(inputTenantId);
                return new HostEventWatcher(
                    redisTemplate,
                    tracer,
                    cmdbEventSampler,
                    bizCmdbClient,
                    tenantService,
                    cmdbEventCursorManager,
                    hostEventHandler,
                    inputTenantId
                );
            }
        );
    }

    /**
     * 对指定租户获取现有的或创建一个业务集事件监听器
     *
     * @param tenantId 租户ID
     * @return 业务集事件监听器
     */
    public HostRelationEventWatcher getOrCreateHostRelationEventWatcher(String tenantId) {
        return hostRelationEventWatcherMap.computeIfAbsent(
            tenantId,
            (inputTenantId) -> {
                CmdbEventHandler<HostRelationEventDetail> hostRelationEventHandler =
                    eventHandlerFactory.getOrCreateHostRelationEventHandler(inputTenantId);
                return new HostRelationEventWatcher(
                    redisTemplate,
                    tracer,
                    cmdbEventSampler,
                    bizCmdbClient,
                    tenantService,
                    cmdbEventCursorManager,
                    hostRelationEventHandler,
                    inputTenantId
                );
            }
        );
    }
}
