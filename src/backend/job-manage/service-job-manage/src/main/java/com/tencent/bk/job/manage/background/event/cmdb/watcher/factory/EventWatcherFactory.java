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

import com.tencent.bk.job.common.cc.sdk.IBizCmdbClient;
import com.tencent.bk.job.common.cc.sdk.IBizSetCmdbClient;
import com.tencent.bk.job.common.gse.service.AgentStateClient;
import com.tencent.bk.job.common.tenant.TenantService;
import com.tencent.bk.job.manage.background.event.cmdb.CmdbEventCursorManager;
import com.tencent.bk.job.manage.background.event.cmdb.watcher.BizEventWatcher;
import com.tencent.bk.job.manage.background.event.cmdb.watcher.BizSetEventWatcher;
import com.tencent.bk.job.manage.background.event.cmdb.watcher.BizSetRelationEventWatcher;
import com.tencent.bk.job.manage.background.event.cmdb.watcher.HostEventWatcher;
import com.tencent.bk.job.manage.background.event.cmdb.watcher.HostRelationEventWatcher;
import com.tencent.bk.job.manage.config.GseConfig;
import com.tencent.bk.job.manage.config.JobManageConfig;
import com.tencent.bk.job.manage.dao.HostTopoDAO;
import com.tencent.bk.job.manage.dao.NoTenantHostDAO;
import com.tencent.bk.job.manage.manager.host.HostCache;
import com.tencent.bk.job.manage.metrics.CmdbEventSampler;
import com.tencent.bk.job.manage.service.ApplicationService;
import com.tencent.bk.job.manage.service.host.NoTenantHostService;
import com.tencent.bk.job.manage.service.impl.BizSetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
    private final CmdbEventSampler cmdbEventSampler;

    private final IBizCmdbClient bizCmdbClient;
    private final IBizSetCmdbClient bizSetCmdbClient;
    private final ApplicationService applicationService;
    private final BizSetService bizSetService;
    private final TenantService tenantService;
    private final NoTenantHostService noTenantHostService;
    private final NoTenantHostDAO noTenantHostDAO;
    private final HostTopoDAO hostTopoDAO;
    private final HostCache hostCache;
    private final AgentStateClient agentStateClient;
    private final JobManageConfig jobManageConfig;
    private final CmdbEventCursorManager cmdbEventCursorManager;

    private final Map<String, BizEventWatcher> bizEventWatcherMap = new ConcurrentHashMap<>();
    private final Map<String, BizSetEventWatcher> bizSetEventWatcherMap = new ConcurrentHashMap<>();
    private final Map<String, BizSetRelationEventWatcher> bizSetRelationEventWatcherMap = new ConcurrentHashMap<>();
    private final Map<String, HostEventWatcher> hostEventWatcherMap = new ConcurrentHashMap<>();
    private final Map<String, HostRelationEventWatcher> hostRelationEventWatcherMap = new ConcurrentHashMap<>();

    @Autowired
    public EventWatcherFactory(RedisTemplate<String, String> redisTemplate,
                               Tracer tracer,
                               CmdbEventSampler cmdbEventSampler,
                               IBizCmdbClient bizCmdbClient,
                               IBizSetCmdbClient bizSetCmdbClient,
                               ApplicationService applicationService,
                               BizSetService bizSetService,
                               TenantService tenantService,
                               NoTenantHostService noTenantHostService,
                               NoTenantHostDAO noTenantHostDAO,
                               HostTopoDAO hostTopoDAO,
                               HostCache hostCache,
                               @Qualifier(GseConfig.MANAGE_BEAN_AGENT_STATE_CLIENT)
                               AgentStateClient agentStateClient,
                               JobManageConfig jobManageConfig,
                               CmdbEventCursorManager cmdbEventCursorManager) {
        this.redisTemplate = redisTemplate;
        this.tracer = tracer;
        this.cmdbEventSampler = cmdbEventSampler;
        this.bizCmdbClient = bizCmdbClient;
        this.bizSetCmdbClient = bizSetCmdbClient;
        this.applicationService = applicationService;
        this.bizSetService = bizSetService;
        this.tenantService = tenantService;
        this.noTenantHostService = noTenantHostService;
        this.noTenantHostDAO = noTenantHostDAO;
        this.hostTopoDAO = hostTopoDAO;
        this.hostCache = hostCache;
        this.agentStateClient = agentStateClient;
        this.jobManageConfig = jobManageConfig;
        this.cmdbEventCursorManager = cmdbEventCursorManager;
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
            (inputTenantId) -> new BizEventWatcher(
                redisTemplate,
                tracer,
                cmdbEventSampler,
                bizCmdbClient,
                applicationService,
                tenantService,
                cmdbEventCursorManager,
                inputTenantId
            )
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
            (inputTenantId) -> new BizSetEventWatcher(
                redisTemplate,
                tracer,
                cmdbEventSampler,
                applicationService,
                bizSetService,
                bizSetCmdbClient,
                tenantService,
                cmdbEventCursorManager,
                inputTenantId
            )
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
            (inputTenantId) -> new BizSetRelationEventWatcher(
                redisTemplate,
                tracer,
                cmdbEventSampler,
                applicationService,
                bizSetService,
                bizSetCmdbClient,
                tenantService,
                cmdbEventCursorManager,
                inputTenantId
            )
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
            (inputTenantId) -> new HostEventWatcher(
                redisTemplate,
                tracer,
                cmdbEventSampler,
                bizCmdbClient,
                noTenantHostService,
                agentStateClient,
                jobManageConfig,
                tenantService,
                cmdbEventCursorManager,
                inputTenantId
            )
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
            (inputTenantId) -> new HostRelationEventWatcher(
                redisTemplate,
                tracer,
                cmdbEventSampler,
                bizCmdbClient,
                applicationService,
                noTenantHostDAO,
                hostTopoDAO,
                hostCache,
                tenantService,
                cmdbEventCursorManager,
                inputTenantId
            )
        );
    }
}
