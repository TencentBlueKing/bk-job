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

package com.tencent.bk.job.manage.background.ha;

import com.tencent.bk.job.common.cc.sdk.IBizCmdbClient;
import com.tencent.bk.job.common.cc.sdk.IBizSetCmdbClient;
import com.tencent.bk.job.common.gse.service.AgentStateClient;
import com.tencent.bk.job.manage.background.event.cmdb.TenantBizEventWatcher;
import com.tencent.bk.job.manage.background.event.cmdb.TenantBizSetEventWatcher;
import com.tencent.bk.job.manage.background.event.cmdb.TenantBizSetRelationEventWatcher;
import com.tencent.bk.job.manage.background.event.cmdb.TenantHostEventWatcher;
import com.tencent.bk.job.manage.background.event.cmdb.TenantHostRelationEventWatcher;
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

/**
 * 后台任务解析器，根据任务实体内容解析出真实的任务
 */
@Slf4j
@Service
public class BackGroundTaskParser {

    private final RedisTemplate<String, String> redisTemplate;
    private final Tracer tracer;
    private final CmdbEventSampler cmdbEventSampler;
    private final IBizCmdbClient bizCmdbClient;
    private final BizSetService bizSetService;
    private final IBizSetCmdbClient bizSetCmdbClient;
    private final ApplicationService applicationService;
    private final NoTenantHostService noTenantHostService;
    private final NoTenantHostDAO noTenantHostDAO;
    private final AgentStateClient agentStateClient;
    private final JobManageConfig jobManageConfig;
    private final HostTopoDAO hostTopoDAO;
    private final HostCache hostCache;

    @Autowired
    public BackGroundTaskParser(RedisTemplate<String, String> redisTemplate,
                                Tracer tracer,
                                CmdbEventSampler cmdbEventSampler,
                                IBizCmdbClient bizCmdbClient,
                                BizSetService bizSetService,
                                IBizSetCmdbClient bizSetCmdbClient,
                                ApplicationService applicationService,
                                NoTenantHostService noTenantHostService,
                                NoTenantHostDAO noTenantHostDAO,
                                @Qualifier(GseConfig.MANAGE_BEAN_AGENT_STATE_CLIENT)
                                AgentStateClient agentStateClient,
                                JobManageConfig jobManageConfig,
                                HostTopoDAO hostTopoDAO,
                                HostCache hostCache) {
        this.redisTemplate = redisTemplate;
        this.tracer = tracer;
        this.cmdbEventSampler = cmdbEventSampler;
        this.bizCmdbClient = bizCmdbClient;
        this.bizSetService = bizSetService;
        this.bizSetCmdbClient = bizSetCmdbClient;
        this.applicationService = applicationService;
        this.noTenantHostService = noTenantHostService;
        this.noTenantHostDAO = noTenantHostDAO;
        this.agentStateClient = agentStateClient;
        this.jobManageConfig = jobManageConfig;
        this.hostTopoDAO = hostTopoDAO;
        this.hostCache = hostCache;
    }

    public IBackGroundTask parse(TaskEntity taskEntity) {
        switch (taskEntity.getTaskCode()) {
            case BackGroundTaskCode.WATCH_BIZ:
                return new TenantBizEventWatcher(
                    redisTemplate,
                    tracer,
                    cmdbEventSampler,
                    bizCmdbClient,
                    applicationService,
                    taskEntity.getTenantId()
                );
            case BackGroundTaskCode.WATCH_BIZ_SET:
                return new TenantBizSetEventWatcher(
                    redisTemplate,
                    tracer,
                    cmdbEventSampler,
                    applicationService,
                    bizSetService,
                    bizSetCmdbClient,
                    taskEntity.getTenantId()
                );
            case BackGroundTaskCode.WATCH_BIZ_SET_RELATION:
                return new TenantBizSetRelationEventWatcher(
                    redisTemplate,
                    tracer,
                    cmdbEventSampler,
                    applicationService,
                    bizSetService,
                    bizSetCmdbClient,
                    taskEntity.getTenantId()
                );
            case BackGroundTaskCode.WATCH_HOST:
                return new TenantHostEventWatcher(
                    redisTemplate,
                    tracer,
                    cmdbEventSampler,
                    bizCmdbClient,
                    noTenantHostService,
                    agentStateClient,
                    jobManageConfig,
                    taskEntity.getTenantId()
                );
            case BackGroundTaskCode.WATCH_HOST_RELATION:
                return new TenantHostRelationEventWatcher(
                    redisTemplate,
                    tracer,
                    cmdbEventSampler,
                    bizCmdbClient,
                    applicationService,
                    noTenantHostDAO,
                    hostTopoDAO,
                    hostCache,
                    taskEntity.getTenantId()
                );
        }
        log.warn("task not supported: {}", taskEntity);
        return null;
    }
}
