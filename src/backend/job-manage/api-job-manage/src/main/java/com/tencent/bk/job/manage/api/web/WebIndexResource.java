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

package com.tencent.bk.job.manage.api.web;

import com.tencent.bk.job.common.annotation.WebAPI;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.ServiceResponse;
import com.tencent.bk.job.common.model.vo.HostInfoVO;
import com.tencent.bk.job.manage.model.web.vo.index.AgentStatistics;
import com.tencent.bk.job.manage.model.web.vo.index.GreetingVO;
import com.tencent.bk.job.manage.model.web.vo.index.JobAndScriptStatistics;
import com.tencent.bk.job.manage.model.web.vo.task.TaskTemplateVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Api(tags = {"job-manage:web:Index"})
@RequestMapping("/web/index/app/{appId}")
@RestController
@WebAPI
public interface WebIndexResource {

    @Deprecated
    @ApiOperation(value = "获取问候语列表", produces = "application/json")
    @GetMapping("/analysis/greeting/list")
    ServiceResponse<List<GreetingVO>> listGreeting(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiParam("业务Id")
        @PathVariable("appId")
            Long appId
    );

    @ApiOperation(value = "获取Agent统计数据", produces = "application/json")
    @GetMapping("/statistics/agent")
    ServiceResponse<AgentStatistics> getAgentStatistics(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiParam("业务Id")
        @PathVariable("appId")
            Long appId
    );

    @ApiOperation(value = "根据Agent状态获取主机分页列表，agentStatus：0为异常，1为正常", produces = "application/json")
    @GetMapping("/hosts/agentStatus/{agentStatus}")
    ServiceResponse<PageData<HostInfoVO>> listHostsByAgentStatus(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiParam("业务Id")
        @PathVariable("appId")
            Long appId,
        @ApiParam("业务Id")
        @PathVariable("agentStatus")
            Integer agentStatus,
        @ApiParam("起始位置")
        @RequestParam(value = "start", required = false)
            Long start,
        @ApiParam("分页大小")
        @RequestParam(value = "pageSize", required = false)
            Long pageSize
    );

    @ApiOperation(value = "根据Agent状态获取IP分页列表，agentStatus：0为异常，1为正常", produces = "application/json")
    @GetMapping("/IPs/agentStatus/{agentStatus}")
    ServiceResponse<PageData<String>> listIPsByAgentStatus(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiParam("业务Id")
        @PathVariable("appId")
            Long appId,
        @ApiParam("业务Id")
        @PathVariable("agentStatus")
            Integer agentStatus,
        @ApiParam("起始位置")
        @RequestParam(value = "start", required = false)
            Long start,
        @ApiParam("分页大小")
        @RequestParam(value = "pageSize", required = false)
            Long pageSize
    );

    @ApiOperation(value = "获取作业与脚本统计数据", produces = "application/json")
    @GetMapping("/statistics/jobAndScript")
    ServiceResponse<JobAndScriptStatistics> getJobAndScriptStatistics(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiParam("业务Id")
        @PathVariable("appId")
            Long appId
    );

    @ApiOperation(value = "获取我收藏的作业", produces = "application/json")
    @GetMapping("/jobs/myFavorList")
    ServiceResponse<List<TaskTemplateVO>> listMyFavorTasks(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiParam("业务Id")
        @PathVariable("appId")
            Long appId,
        @ApiParam("拉取数量，不传为拉全量")
        @RequestParam(value = "limit", required = false)
            Long limit
    );
}
