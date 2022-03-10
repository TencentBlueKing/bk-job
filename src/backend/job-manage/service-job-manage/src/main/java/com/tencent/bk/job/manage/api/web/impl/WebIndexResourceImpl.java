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

package com.tencent.bk.job.manage.api.web.impl;

import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.vo.HostInfoVO;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.manage.api.web.WebIndexResource;
import com.tencent.bk.job.manage.model.web.vo.index.AgentStatistics;
import com.tencent.bk.job.manage.model.web.vo.index.GreetingVO;
import com.tencent.bk.job.manage.model.web.vo.index.JobAndScriptStatistics;
import com.tencent.bk.job.manage.model.web.vo.task.TaskTemplateVO;
import com.tencent.bk.job.manage.service.IndexService;
import com.tencent.bk.job.manage.service.auth.TaskTemplateAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Slf4j
public class WebIndexResourceImpl implements WebIndexResource {

    private final IndexService indexService;
    private final TaskTemplateAuthService taskTemplateAuthService;
    private final AppScopeMappingService appScopeMappingService;

    @Autowired
    public WebIndexResourceImpl(IndexService indexService,
                                TaskTemplateAuthService taskTemplateAuthService,
                                AppScopeMappingService appScopeMappingService) {
        this.indexService = indexService;
        this.taskTemplateAuthService = taskTemplateAuthService;
        this.appScopeMappingService = appScopeMappingService;
    }

    @Override
    public Response<List<GreetingVO>> listGreeting(String username, String scopeType, String scopeId) {
        return Response.buildSuccessResp(indexService.listGreeting(username));
    }

    @Override
    public Response<AgentStatistics> getAgentStatistics(String username, String scopeType, String scopeId) {
        Long appId = appScopeMappingService.getAppIdByScope(scopeType, scopeId);
        return Response.buildSuccessResp(indexService.getAgentStatistics(username, appId));
    }

    @Override
    public Response<PageData<HostInfoVO>> listHostsByAgentStatus(String username, String scopeType, String scopeId,
                                                                 Integer agentStatus, Long start,
                                                                 Long pageSize) {
        Long appId = appScopeMappingService.getAppIdByScope(scopeType, scopeId);
        return Response.buildSuccessResp(indexService.listHostsByAgentStatus(username, appId, agentStatus,
            start, pageSize));
    }

    @Override
    public Response<PageData<String>> listIPsByAgentStatus(String username, String scopeType, String scopeId,
                                                           Integer agentStatus,
                                                           Long start, Long pageSize) {
        Long appId = appScopeMappingService.getAppIdByScope(scopeType, scopeId);
        return Response.buildSuccessResp(indexService.listIPsByAgentStatus(username, appId, agentStatus, start,
            pageSize));
    }

    @Override
    public Response<JobAndScriptStatistics> getJobAndScriptStatistics(String username, String scopeType,
                                                                      String scopeId) {
        Long appId = appScopeMappingService.getAppIdByScope(scopeType, scopeId);
        return Response.buildSuccessResp(indexService.getJobAndScriptStatistics(username, appId));
    }

    @Override
    public Response<List<TaskTemplateVO>> listMyFavorTasks(String username, String scopeType, String scopeId,
                                                           Long limit) {
        AppResourceScope appResourceScope = appScopeMappingService.getAppResourceScope(null, scopeType, scopeId);
        List<TaskTemplateVO> resultList = indexService.listMyFavorTasks(username, appResourceScope.getAppId(), limit);
        taskTemplateAuthService.processTemplatePermission(username, appResourceScope, resultList);
        return Response.buildSuccessResp(resultList);
    }
}
