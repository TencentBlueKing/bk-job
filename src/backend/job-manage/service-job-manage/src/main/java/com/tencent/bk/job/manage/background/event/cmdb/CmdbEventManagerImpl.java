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
import com.tencent.bk.job.manage.background.ha.BackGroundTaskRegistry;
import com.tencent.bk.job.manage.background.ha.IBackGroundTask;
import com.tencent.bk.job.manage.background.ha.TaskEntity;
import com.tencent.bk.job.manage.background.ha.mq.BackGroundTaskDispatcher;
import com.tencent.bk.job.manage.background.ha.mq.BackGroundTaskListenerController;
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
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * CMDB事件管理器，负责处理CMDB事件监听相关逻辑
 */
@SuppressWarnings("FieldCanBeLocal")
@Slf4j
@Service
public class CmdbEventManagerImpl implements CmdbEventManager, DisposableBean {

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
    private final AgentStateClient agentStateClient;

    /**
     * 日志调用链tracer
     */
    private final Tracer tracer;
    private final CmdbEventSampler cmdbEventSampler;
    private final ThreadPoolExecutor shutdownEventWatchExecutor;
    private final BackGroundTaskDispatcher backGroundTaskDispatcher;
    private final BackGroundTaskListenerController backGroundTaskListenerController;
    private final BackGroundTaskRegistry backGroundTaskRegistry;

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
                                CmdbEventSampler cmdbEventSampler,
                                ThreadPoolExecutor shutdownEventWatchExecutor,
                                BackGroundTaskDispatcher backGroundTaskDispatcher,
                                BackGroundTaskListenerController backGroundTaskListenerController,
                                BackGroundTaskRegistry backGroundTaskRegistry) {
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
        this.shutdownEventWatchExecutor = shutdownEventWatchExecutor;
        this.backGroundTaskDispatcher = backGroundTaskDispatcher;
        this.backGroundTaskListenerController = backGroundTaskListenerController;
        this.backGroundTaskRegistry = backGroundTaskRegistry;
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
    @Override
    public void watchBizEvent(String tenantId) {
        TenantBizEventWatcher tenantBizEventWatcher = new TenantBizEventWatcher(
            redisTemplate,
            tracer,
            cmdbEventSampler,
            bizCmdbClient,
            applicationService,
            tenantId
        );
        String uniqueCode = tenantBizEventWatcher.getUniqueCode();
        if (backGroundTaskRegistry.existsTask(uniqueCode)) {
            log.warn("task {} already exists in registry, ignore", uniqueCode);
            return;
        }
        // 注册后台任务用于动态调度
        boolean registerSuccess = backGroundTaskRegistry.registerTask(uniqueCode, tenantBizEventWatcher);
        if (registerSuccess) {
            // 开一个常驻线程监听业务资源变动事件
            tenantBizEventWatcher.start();
        }
    }

    /**
     * 监听业务集相关的事件
     */
    @Override
    public void watchBizSetEvent(String tenantId) {
        TenantBizSetEventWatcher bizSetEventWatcher = new TenantBizSetEventWatcher(
            redisTemplate,
            tracer,
            cmdbEventSampler,
            applicationService,
            bizSetService,
            bizSetCmdbClient,
            tenantId
        );
        String uniqueCode = bizSetEventWatcher.getUniqueCode();
        if (backGroundTaskRegistry.existsTask(uniqueCode)) {
            log.warn("task {} already exists in registry, ignore", uniqueCode);
            return;
        }
        // 注册后台任务用于动态调度
        boolean registerSuccess = backGroundTaskRegistry.registerTask(
            uniqueCode,
            bizSetEventWatcher
        );
        if (registerSuccess) {
            // 开一个常驻线程监听业务集资源变动事件
            bizSetEventWatcher.start();
        }
    }

    /**
     * 监听业务集相关的事件
     */
    @Override
    public void watchBizSetRelationEvent(String tenantId) {
        TenantBizSetRelationEventWatcher bizSetRelationEventWatcher = new TenantBizSetRelationEventWatcher(
            redisTemplate,
            tracer,
            cmdbEventSampler,
            applicationService,
            bizSetService,
            bizSetCmdbClient,
            tenantId
        );
        String uniqueCode = bizSetRelationEventWatcher.getUniqueCode();
        if (backGroundTaskRegistry.existsTask(uniqueCode)) {
            log.warn("task {} already exists in registry, ignore", uniqueCode);
            return;
        }
        // 注册后台任务用于动态调度
        boolean registerSuccess = backGroundTaskRegistry.registerTask(uniqueCode, bizSetRelationEventWatcher);
        if (registerSuccess) {
            // 开一个常驻线程监听业务集关系变动事件
            bizSetRelationEventWatcher.start();
        }
    }

    /**
     * 监听主机相关的事件
     */
    @Override
    public void watchHostEvent(String tenantId) {
        TenantHostEventWatcher tenantHostEventWatcher = new TenantHostEventWatcher(
            redisTemplate,
            tracer,
            cmdbEventSampler,
            bizCmdbClient,
            noTenantHostService,
            agentStateClient,
            jobManageConfig,
            tenantId
        );
        String uniqueCode = tenantHostEventWatcher.getUniqueCode();
        if (backGroundTaskRegistry.existsTask(uniqueCode)) {
            log.warn("task {} already exists in registry, ignore", uniqueCode);
            return;
        }
        // 注册后台任务用于动态调度
        boolean registerSuccess = backGroundTaskRegistry.registerTask(uniqueCode, tenantHostEventWatcher);
        if (registerSuccess) {
            // 开一个常驻线程监听主机资源变动事件
            tenantHostEventWatcher.start();
        }
    }

    /**
     * 监听主机关系相关的事件
     */
    @Override
    public void watchHostRelationEvent(String tenantId) {
        TenantHostRelationEventWatcher hostRelationEventWatcher = new TenantHostRelationEventWatcher(
            redisTemplate,
            tracer,
            cmdbEventSampler,
            bizCmdbClient,
            applicationService,
            noTenantHostDAO,
            hostTopoDAO,
            hostCache,
            tenantId
        );
        String uniqueCode = hostRelationEventWatcher.getUniqueCode();
        if (backGroundTaskRegistry.existsTask(uniqueCode)) {
            log.warn("task {} already exists in registry, ignore", uniqueCode);
            return;
        }
        // 注册后台任务用于动态调度
        boolean registerSuccess = backGroundTaskRegistry.registerTask(uniqueCode, hostRelationEventWatcher);
        if (registerSuccess) {
            // 开一个常驻线程监听主机关系资源变动事件
            hostRelationEventWatcher.start();
        }
    }

    @Override
    public TenantHostEventWatcher getTenantHostEventWatcher(String tenantId) {
        for (IBackGroundTask task : backGroundTaskRegistry.getTaskMap().values()) {
            if (task instanceof TenantHostEventWatcher && task.getTenantId().equals(tenantId)) {
                return (TenantHostEventWatcher) task;
            }
        }
        return null;
    }

    @Override
    public Boolean enableBizWatch() {
        log.info("appWatch enabled by op");
        for (IBackGroundTask task : backGroundTaskRegistry.getTaskMap().values()) {
            if (task instanceof TenantBizEventWatcher) {
                TenantBizEventWatcher tenantBizEventWatcher = (TenantBizEventWatcher) task;
                tenantBizEventWatcher.setWatchFlag(true);
            }
        }
        return true;
    }

    @Override
    public Boolean disableBizWatch() {
        log.info("appWatch disabled by op");
        for (IBackGroundTask task : backGroundTaskRegistry.getTaskMap().values()) {
            if (task instanceof TenantBizEventWatcher) {
                TenantBizEventWatcher tenantBizEventWatcher = (TenantBizEventWatcher) task;
                tenantBizEventWatcher.setWatchFlag(false);
            }
        }
        return true;
    }

    @Override
    public Boolean enableHostWatch() {
        log.info("hostWatch enabled by op");
        for (IBackGroundTask task : backGroundTaskRegistry.getTaskMap().values()) {
            if (task instanceof TenantHostEventWatcher) {
                TenantHostEventWatcher tenantHostEventWatcher = (TenantHostEventWatcher) task;
                tenantHostEventWatcher.setWatchFlag(true);
            }
        }
        for (IBackGroundTask task : backGroundTaskRegistry.getTaskMap().values()) {
            if (task instanceof TenantHostRelationEventWatcher) {
                TenantHostRelationEventWatcher tenantHostRelationEventWatcher = (TenantHostRelationEventWatcher) task;
                tenantHostRelationEventWatcher.setWatchFlag(true);
            }
        }
        return true;
    }

    @Override
    public Boolean disableHostWatch() {
        log.info("hostWatch disabled by op");
        for (IBackGroundTask task : backGroundTaskRegistry.getTaskMap().values()) {
            if (task instanceof TenantHostEventWatcher) {
                TenantHostEventWatcher tenantHostEventWatcher = (TenantHostEventWatcher) task;
                tenantHostEventWatcher.setWatchFlag(false);
            }
        }
        for (IBackGroundTask task : backGroundTaskRegistry.getTaskMap().values()) {
            if (task instanceof TenantHostRelationEventWatcher) {
                TenantHostRelationEventWatcher tenantHostRelationEventWatcher = (TenantHostRelationEventWatcher) task;
                tenantHostRelationEventWatcher.setWatchFlag(false);
            }
        }
        return true;
    }

    @Override
    public void destroy() {
        log.info("On destroy, shutdown all tasks and re-schedule them");
        // 1.关闭任务接收通道
        backGroundTaskListenerController.stop();
        // 2.停止所有任务，并将其重新调度至其他实例
        for (IBackGroundTask task : backGroundTaskRegistry.getTaskMap().values()) {
            shutdownEventWatchExecutor.submit(() -> {
                TaskEntity taskEntity = task.getTaskEntity();
                task.shutdownGracefully();
                backGroundTaskDispatcher.dispatch(taskEntity);
                log.info("task {} rescheduled", taskEntity.getUniqueCode());
            });
        }
    }
}
