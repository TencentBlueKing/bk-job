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

package com.tencent.bk.job.manage.background.event.cmdb;

import com.tencent.bk.job.common.cc.sdk.IBizCmdbClient;
import com.tencent.bk.job.common.cc.sdk.IBizSetCmdbClient;
import com.tencent.bk.job.common.gse.service.AgentStateClient;
import com.tencent.bk.job.common.paas.model.OpenApiTenant;
import com.tencent.bk.job.common.paas.user.IUserApiClient;
import com.tencent.bk.job.manage.config.GseConfig;
import com.tencent.bk.job.manage.config.JobManageConfig;
import com.tencent.bk.job.manage.dao.HostTopoDAO;
import com.tencent.bk.job.manage.dao.NoTenantHostDAO;
import com.tencent.bk.job.manage.manager.host.HostCache;
import com.tencent.bk.job.manage.metrics.CmdbEventSampler;
import com.tencent.bk.job.manage.service.ApplicationService;
import com.tencent.bk.job.manage.service.host.NoTenantHostService;
import com.tencent.bk.job.manage.service.impl.BizSetService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CMDB事件管理器，负责处理CMDB事件监听相关逻辑
 */
@SuppressWarnings("FieldCanBeLocal")
@Slf4j
@Service
public class CmdbEventManagerImpl implements CmdbEventManager {

    private final IBizCmdbClient bizCmdbClient;
    private final IBizSetCmdbClient bizSetCmdbClient;
    private final IUserApiClient userApiClient;
    private final ApplicationService applicationService;
    private final BizSetService bizSetService;
    private final NoTenantHostService noTenantHostService;
    private final NoTenantHostDAO noTenantHostDAO;
    private final HostTopoDAO hostTopoDAO;
    private final HostCache hostCache;
    private final JobManageConfig jobManageConfig;
    private final RedisTemplate<String, String> redisTemplate;
    private final Map<String, TenantBizEventWatcher> tenantBizEventWatcherMap = new HashMap<>();
    private final Map<String, TenantHostEventWatcher> tenantHostEventWatcherMap = new HashMap<>();
    private final Map<String, TenantHostRelationEventWatcher> tenantHostRelationEventWatcherMap = new HashMap<>();
    private final Map<String, TenantBizSetEventWatcher> bizSetEventWatcherMap = new HashMap<>();
    private final Map<String, TenantBizSetRelationEventWatcher> bizSetRelationEventWatcherMap = new HashMap<>();
    private final AgentStateClient agentStateClient;

    /**
     * 日志调用链tracer
     */
    private final Tracer tracer;
    private final CmdbEventSampler cmdbEventSampler;

    @Autowired
    public CmdbEventManagerImpl(IBizCmdbClient bizCmdbClient,
                                IBizSetCmdbClient bizSetCmdbClient,
                                IUserApiClient userApiClient,
                                ApplicationService applicationService,
                                BizSetService bizSetService,
                                NoTenantHostService noTenantHostService,
                                NoTenantHostDAO noTenantHostDAO,
                                HostTopoDAO hostTopoDAO,
                                HostCache hostCache,
                                JobManageConfig jobManageConfig,
                                RedisTemplate<String, String> redisTemplate,
                                @Qualifier(GseConfig.MANAGE_BEAN_AGENT_STATE_CLIENT)
                                AgentStateClient agentStateClient,
                                Tracer tracer,
                                CmdbEventSampler cmdbEventSampler) {
        this.bizCmdbClient = bizCmdbClient;
        this.bizSetCmdbClient = bizSetCmdbClient;
        this.userApiClient = userApiClient;
        this.applicationService = applicationService;
        this.bizSetService = bizSetService;
        this.noTenantHostService = noTenantHostService;
        this.noTenantHostDAO = noTenantHostDAO;
        this.hostTopoDAO = hostTopoDAO;
        this.hostCache = hostCache;
        this.jobManageConfig = jobManageConfig;
        this.redisTemplate = redisTemplate;
        this.agentStateClient = agentStateClient;
        this.tracer = tracer;
        this.cmdbEventSampler = cmdbEventSampler;
    }

    @Override
    public void init() {
        // TODO：增加调度机制保证各实例之间负载均衡
        List<OpenApiTenant> tenantList = userApiClient.listAllTenant();
        // 遍历所有租户监听事件
        for (OpenApiTenant openApiTenant : tenantList) {
            initTenant(openApiTenant.getId());
        }
    }

    public void initTenant(String tenantId) {
        if (jobManageConfig.isEnableResourceWatch()) {
            watchBizEvent(tenantId);
            watchBizSetEvent(tenantId);
            watchBizSetRelationEvent(tenantId);
            watchHostEvent(tenantId);
            watchHostRelationEvent(tenantId);
        } else {
            log.info("resourceWatch not enabled, you can enable it in config file");
        }
    }

    /**
     * 监听业务相关的事件
     */
    private void watchBizEvent(String tenantId) {
        TenantBizEventWatcher tenantBizEventWatcher = tenantBizEventWatcherMap.computeIfAbsent(
            tenantId, key ->
                new TenantBizEventWatcher(
                    redisTemplate,
                    tracer,
                    cmdbEventSampler,
                    bizCmdbClient,
                    applicationService,
                    tenantId
                )
        );
        // 开一个常驻线程监听业务资源变动事件
        tenantBizEventWatcher.start();
    }

    /**
     * 监听业务集相关的事件
     */
    private void watchBizSetEvent(String tenantId) {
        TenantBizSetEventWatcher bizSetEventWatcher = bizSetEventWatcherMap.computeIfAbsent(
            tenantId, key ->
                new TenantBizSetEventWatcher(
                    redisTemplate,
                    tracer,
                    cmdbEventSampler,
                    applicationService,
                    bizSetService,
                    bizSetCmdbClient,
                    tenantId
                )
        );
        // 开一个常驻线程监听业务集变动事件
        bizSetEventWatcher.start();
    }

    /**
     * 监听业务集相关的事件
     */
    private void watchBizSetRelationEvent(String tenantId) {
        TenantBizSetRelationEventWatcher bizSetRelationEventWatcher = bizSetRelationEventWatcherMap.computeIfAbsent(
            tenantId, key ->
                new TenantBizSetRelationEventWatcher(
                    redisTemplate,
                    tracer,
                    cmdbEventSampler,
                    applicationService,
                    bizSetService,
                    bizSetCmdbClient,
                    tenantId
                )
        );
        // 开一个常驻线程监听业务集关系变动事件
        bizSetRelationEventWatcher.start();
    }

    /**
     * 监听主机相关的事件
     */
    private void watchHostEvent(String tenantId) {
        TenantHostEventWatcher tenantHostEventWatcher = tenantHostEventWatcherMap.computeIfAbsent(
            tenantId, key ->
                new TenantHostEventWatcher(
                    redisTemplate,
                    tracer,
                    cmdbEventSampler,
                    bizCmdbClient,
                    noTenantHostService,
                    agentStateClient,
                    jobManageConfig,
                    tenantId
                )
        );
        // 开一个常驻线程监听主机资源变动事件
        tenantHostEventWatcher.start();
    }

    /**
     * 监听主机关系相关的事件
     */
    private void watchHostRelationEvent(String tenantId) {
        TenantHostRelationEventWatcher hostRelationEventWatcher = tenantHostRelationEventWatcherMap.computeIfAbsent(
            tenantId, key ->
                new TenantHostRelationEventWatcher(
                    redisTemplate,
                    tracer,
                    cmdbEventSampler,
                    bizCmdbClient,
                    applicationService,
                    noTenantHostDAO,
                    hostTopoDAO,
                    hostCache,
                    tenantId
                )
        );
        // 开一个常驻线程监听主机关系资源变动事件
        hostRelationEventWatcher.start();
    }

    @Override
    public TenantHostEventWatcher getTenantHostEventWatcher(String tenantId) {
        return tenantHostEventWatcherMap.get(tenantId);
    }

    @Override
    public Boolean enableBizWatch() {
        log.info("appWatch enabled by op");
        for (TenantBizEventWatcher tenantBizEventWatcher : tenantBizEventWatcherMap.values()) {
            tenantBizEventWatcher.setWatchFlag(true);
        }
        return true;
    }

    @Override
    public Boolean disableBizWatch() {
        log.info("appWatch disabled by op");
        for (TenantBizEventWatcher tenantBizEventWatcher : tenantBizEventWatcherMap.values()) {
            tenantBizEventWatcher.setWatchFlag(false);
        }
        return true;
    }

    @Override
    public Boolean enableHostWatch() {
        log.info("hostWatch enabled by op");
        for (TenantHostEventWatcher tenantHostEventWatcher : tenantHostEventWatcherMap.values()) {
            tenantHostEventWatcher.setWatchFlag(true);
        }
        for (TenantHostRelationEventWatcher watcher : tenantHostRelationEventWatcherMap.values()) {
            watcher.setWatchFlag(true);
        }
        return true;
    }

    @Override
    public Boolean disableHostWatch() {
        log.info("hostWatch disabled by op");
        for (TenantHostEventWatcher watcher : tenantHostEventWatcherMap.values()) {
            watcher.setWatchFlag(false);
        }
        for (TenantHostRelationEventWatcher watcher : tenantHostRelationEventWatcherMap.values()) {
            watcher.setWatchFlag(false);
        }
        return true;
    }

}
