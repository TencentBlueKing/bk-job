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

package com.tencent.bk.job.manage.api.web;

import com.tencent.bk.job.common.annotation.WebAPI;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.vo.HostInfoVO;
import com.tencent.bk.job.manage.model.web.vo.common.AgentStatistics;
import com.tencent.bk.job.manage.model.web.vo.index.GreetingVO;
import com.tencent.bk.job.manage.model.web.vo.index.JobAndScriptStatistics;
import com.tencent.bk.job.manage.model.web.vo.task.TaskTemplateVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Hidden;

import java.util.List;

@Tag(name = "job-manage:web:Index")
@RequestMapping("/web/index/scope/{scopeType}/{scopeId}")
@RestController
@WebAPI
public interface WebIndexResource {

    @Deprecated
    @Operation(summary = "获取问候语列表")
    @GetMapping("/analysis/greeting/list")
    Response<List<GreetingVO>> listGreeting(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @Hidden
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @Parameter(description = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @Parameter(description = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId
    );

    @Operation(summary = "获取Agent统计数据")
    @GetMapping("/statistics/agent")
    Response<AgentStatistics> getAgentStatistics(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @Hidden
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @Parameter(description = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @Parameter(description = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId
    );

    @Operation(summary = "根据Agent状态获取主机分页列表，agentStatus：0为异常，1为正常")
    @GetMapping("/hosts/agentStatus/{agentStatus}")
    Response<PageData<HostInfoVO>> listHostsByAgentStatus(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @Hidden
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @Parameter(description = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @Parameter(description = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @Parameter(description = "Agent状态")
        @PathVariable("agentStatus")
            Integer agentStatus,
        @Parameter(description = "起始位置")
        @RequestParam(value = "start", required = false)
            Long start,
        @Parameter(description = "分页大小")
        @RequestParam(value = "pageSize", required = false)
            Long pageSize
    );

    @Operation(summary = "根据Agent状态获取IP分页列表，agentStatus：0为异常，1为正常")
    @GetMapping("/IPs/agentStatus/{agentStatus}")
    Response<PageData<String>> listIPsByAgentStatus(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @Hidden
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @Parameter(description = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @Parameter(description = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @Parameter(description = "Agent状态")
        @PathVariable("agentStatus")
            Integer agentStatus,
        @Parameter(description = "起始位置")
        @RequestParam(value = "start", required = false)
            Long start,
        @Parameter(description = "分页大小")
        @RequestParam(value = "pageSize", required = false)
            Long pageSize
    );

    @Operation(summary = "获取作业与脚本统计数据")
    @GetMapping("/statistics/jobAndScript")
    Response<JobAndScriptStatistics> getJobAndScriptStatistics(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @Hidden
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @Parameter(description = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @Parameter(description = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId
    );

    @Operation(summary = "获取我收藏的作业")
    @GetMapping("/jobs/myFavorList")
    Response<List<TaskTemplateVO>> listMyFavorTasks(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @Hidden
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @Parameter(description = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @Parameter(description = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @Parameter(description = "拉取数量，不传为拉全量")
        @RequestParam(value = "limit", required = false)
            Long limit
    );
}
