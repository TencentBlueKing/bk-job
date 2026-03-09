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

package com.tencent.bk.job.manage.background.event.cmdb.handler.factory;

import com.tencent.bk.job.common.cc.model.result.BizEventDetail;
import com.tencent.bk.job.common.cc.model.result.BizSetEventDetail;
import com.tencent.bk.job.common.cc.model.result.BizSetRelationEventDetail;
import com.tencent.bk.job.common.cc.model.result.HostEventDetail;
import com.tencent.bk.job.common.cc.model.result.HostRelationEventDetail;
import com.tencent.bk.job.common.cc.sdk.IBizCmdbClient;
import com.tencent.bk.job.common.gse.service.AgentStateClient;
import com.tencent.bk.job.manage.background.event.cmdb.handler.AsyncEventHandler;
import com.tencent.bk.job.manage.background.event.cmdb.handler.BizEventHandler;
import com.tencent.bk.job.manage.background.event.cmdb.handler.BizSetEventHandler;
import com.tencent.bk.job.manage.background.event.cmdb.handler.BizSetRelationEventHandler;
import com.tencent.bk.job.manage.background.event.cmdb.handler.CmdbEventHandler;
import com.tencent.bk.job.manage.background.event.cmdb.handler.ConcurrentHostEventHandler;
import com.tencent.bk.job.manage.background.event.cmdb.handler.HostEventHandler;
import com.tencent.bk.job.manage.background.event.cmdb.handler.HostRelationEventHandler;
import com.tencent.bk.job.manage.config.GseConfig;
import com.tencent.bk.job.manage.config.JobManageConfig;
import com.tencent.bk.job.manage.dao.HostTopoDAO;
import com.tencent.bk.job.manage.manager.host.HostCache;
import com.tencent.bk.job.manage.metrics.CmdbEventSampler;
import com.tencent.bk.job.manage.service.ApplicationService;
import com.tencent.bk.job.manage.service.host.NoTenantHostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 事件处理器工厂，用于创建各种类型的事件处理器
 */
@Service
public class EventHandlerFactory {

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
     * 业务服务
     */
    private final ApplicationService applicationService;
    /**
     * 租户无关的主机服务
     */
    private final NoTenantHostService noTenantHostService;
    /**
     * 主机拓扑数据访问对象
     */
    private final HostTopoDAO hostTopoDAO;
    /**
     * 主机缓存
     */
    private final HostCache hostCache;
    /**
     * 主机Agent状态查询客户端
     */
    private final AgentStateClient agentStateClient;
    /**
     * Job-Manage服务相关配置
     */
    private final JobManageConfig jobManageConfig;

    /**
     * 缓存Map<租户ID，业务事件处理器>
     */
    private final Map<String, CmdbEventHandler<BizEventDetail>> bizEventHandlerMap = new ConcurrentHashMap<>();

    /**
     * 缓存Map<租户ID，业务集事件处理器>
     */
    private final Map<String, CmdbEventHandler<BizSetEventDetail>> bizSetEventHandlerMap = new ConcurrentHashMap<>();

    /**
     * 缓存Map<租户ID，业务集关系事件处理器>
     */
    private final Map<String, CmdbEventHandler<BizSetRelationEventDetail>> bizSetRelationEventHandlerMap =
        new ConcurrentHashMap<>();

    /**
     * 缓存Map<租户ID，主机事件处理器>
     */
    private final Map<String, CmdbEventHandler<HostEventDetail>> hostEventHandlerMap = new ConcurrentHashMap<>();

    /**
     * 缓存Map<租户ID，主机关系事件处理器>
     */
    private final Map<String, CmdbEventHandler<HostRelationEventDetail>> hostRelationEventHandlerMap =
        new ConcurrentHashMap<>();

    @Autowired
    public EventHandlerFactory(Tracer tracer,
                               CmdbEventSampler cmdbEventSampler,
                               IBizCmdbClient bizCmdbClient,
                               ApplicationService applicationService,
                               NoTenantHostService noTenantHostService,
                               HostTopoDAO hostTopoDAO,
                               HostCache hostCache,
                               @Qualifier(GseConfig.MANAGE_BEAN_AGENT_STATE_CLIENT)
                               AgentStateClient agentStateClient,
                               JobManageConfig jobManageConfig) {
        this.tracer = tracer;
        this.cmdbEventSampler = cmdbEventSampler;
        this.bizCmdbClient = bizCmdbClient;
        this.applicationService = applicationService;
        this.noTenantHostService = noTenantHostService;
        this.hostTopoDAO = hostTopoDAO;
        this.hostCache = hostCache;
        this.agentStateClient = agentStateClient;
        this.jobManageConfig = jobManageConfig;
    }

    /**
     * 对指定租户获取现有的或创建一个业务事件处理器
     *
     * @param tenantId 租户ID
     * @return 业务事件处理器
     */
    public CmdbEventHandler<BizEventDetail> getOrCreateBizEventHandler(String tenantId) {
        return bizEventHandlerMap.computeIfAbsent(
            tenantId,
            (inputTenantId) -> new BizEventHandler(applicationService, inputTenantId)
        );
    }

    /**
     * 对指定租户获取现有的或创建一个业务集事件处理器
     *
     * @param tenantId 租户ID
     * @return 业务集事件处理器
     */
    public CmdbEventHandler<BizSetEventDetail> getOrCreateBizSetEventHandler(String tenantId) {
        return bizSetEventHandlerMap.computeIfAbsent(
            tenantId,
            (inputTenantId) -> new BizSetEventHandler(applicationService, inputTenantId)
        );
    }

    /**
     * 对指定租户获取现有的或创建一个业务集关系事件处理器
     *
     * @param tenantId 租户ID
     * @return 业务集关系事件处理器
     */
    public CmdbEventHandler<BizSetRelationEventDetail> getOrCreateBizSetRelationEventHandler(String tenantId) {
        return bizSetRelationEventHandlerMap.computeIfAbsent(
            tenantId,
            (inputTenantId) -> new BizSetRelationEventHandler(applicationService)
        );
    }

    /**
     * 对指定租户创建一个主机事件处理器
     *
     * @param tenantId 租户ID
     * @return 主机事件处理器
     */
    public AsyncEventHandler<HostEventDetail> createHostEventHandler(
        String tenantId,
        String handlerName,
        int hostEventQueueSize
    ) {
        return new HostEventHandler(
            tracer,
            cmdbEventSampler,
            handlerName,
            hostEventQueueSize,
            noTenantHostService,
            agentStateClient,
            bizCmdbClient,
            tenantId
        );
    }

    /**
     * 对指定租户获取现有的或创建一个主机事件并发处理器
     *
     * @param tenantId 租户ID
     * @return 主机事件并发处理器
     */
    public CmdbEventHandler<HostEventDetail> getOrCreateConcurrentHostEventHandler(String tenantId) {
        return hostEventHandlerMap.computeIfAbsent(
            tenantId,
            (inputTenantId) -> new ConcurrentHostEventHandler(
                this,
                jobManageConfig,
                inputTenantId
            )
        );
    }

    /**
     * 对指定租户获取现有的或创建一个主机关系事件处理器
     *
     * @param tenantId 租户ID
     * @return 主机关系事件处理器
     */
    public CmdbEventHandler<HostRelationEventDetail> getOrCreateHostRelationEventHandler(String tenantId) {
        return hostRelationEventHandlerMap.computeIfAbsent(
            tenantId,
            (inputTenantId) -> new HostRelationEventHandler(
                tracer,
                cmdbEventSampler,
                applicationService,
                noTenantHostService,
                hostTopoDAO,
                hostCache,
                jobManageConfig,
                inputTenantId
            )
        );
    }
}
