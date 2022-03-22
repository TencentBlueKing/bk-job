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

package com.tencent.bk.job.execute.api.web;

import com.tencent.bk.job.common.annotation.WebAPI;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.execute.common.constants.TaskTotalTimeTypeEnum;
import com.tencent.bk.job.execute.model.web.vo.ExecuteVariableVO;
import com.tencent.bk.job.execute.model.web.vo.FileDistributionDetailVO;
import com.tencent.bk.job.execute.model.web.vo.IpFileLogContentVO;
import com.tencent.bk.job.execute.model.web.vo.IpScriptLogContentVO;
import com.tencent.bk.job.execute.model.web.vo.StepExecutionDetailVO;
import com.tencent.bk.job.execute.model.web.vo.StepExecutionRecordVO;
import com.tencent.bk.job.execute.model.web.vo.TaskExecuteResultVO;
import com.tencent.bk.job.execute.model.web.vo.TaskInstanceVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;

/**
 * 作业执行情况API-前端调用
 */
@Api(tags = {"job-execute:web:Task_Execution_Result"})
@RequestMapping("/web/execution")
@RestController
@WebAPI
public interface WebTaskExecutionResultResource {
    @ApiOperation(value = "获取作业执行历史列表", produces = "application/json")
    @GetMapping(value = {"/app/{appId}/task-execution-history/list",
        "/scope/{scopeType}/{scopeId}/task-execution-history/list"})
    Response<PageData<TaskInstanceVO>> getTaskHistoryList(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiIgnore
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @ApiParam(value = "资源范围类型", required = false)
        @PathVariable(value = "scopeType", required = false)
            String scopeType,
        @ApiParam(value = "资源范围ID", required = false)
        @PathVariable(value = "scopeId", required = false)
            String scopeId,
        @ApiParam(value = "任务名称", name = "taskName", required = false)
        @RequestParam(value = "taskName", required = false)
            String taskName,
        @ApiParam(value = "任务ID", name = "taskInstanceId", required = false)
        @RequestParam(value = "taskInstanceId", required = false)
            Long taskInstanceId,
        @ApiParam(value = "任务状态", name = "status", required = false)
        @RequestParam(value = "status", required = false)
            Integer status,
        @ApiParam(value = "执行人", name = "operator", required = false)
        @RequestParam(value = "operator", required = false)
            String operator,
        @ApiParam(value = "任务类型", name = "taskType", required = false)
        @RequestParam(value = "taskType", required = false)
            Integer taskType,
        @ApiParam(value = "开始时间", name = "startTime", required = false)
        @RequestParam(value = "startTime", required = false)
            String startTime,
        @ApiParam(value = "结束时间", name = "endTime", required = false)
        @RequestParam(value = "endTime", required = false)
            String endTime,
        @ApiParam(value = "时间范围，单位天;如果使用timeRange参数,将忽略startTime/endTime。最大值:30", name = "timeRange", required = false)
        @RequestParam(value = "timeRange", required = false)
            Integer timeRange,
        @ApiParam(value = "耗时类型", name = "totalTimeType", required = false)
        @RequestParam(value = "totalTimeType", required = false)
            TaskTotalTimeTypeEnum totalTimeType,
        @ApiParam(value = "分页-开始", required = false)
        @RequestParam(value = "start", required = false)
            Integer start,
        @ApiParam(value = "分页-每页大小", required = false)
        @RequestParam(value = "pageSize", required = false)
            Integer pageSize,
        @ApiParam(value = "定时任务ID", required = false)
        @RequestParam(value = "cronTaskId", required = false)
            Long cronTaskId,
        @ApiParam(value = "执行方式,1-页面执行,2-API调用,3-定时任务。如果传多个用,分隔，比如:1,2", name = "startupModes", required = false)
        @RequestParam(value = "startupModes", required = false)
            String startupModes,
        @ApiParam(value = "ip", name = "ip", required = false)
        @RequestParam(value = "ip", required = false)
            String ip
    );

    @ApiOperation(value = "获取作业执行信息", produces = "application/json")
    @GetMapping(value = {"/app/{appId}/task-execution-result/{taskInstanceId}",
        "/scope/{scopeType}/{scopeId}/task-execution-result/{taskInstanceId}"})
    Response<TaskExecuteResultVO> getTaskExecutionResult(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiIgnore
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @ApiParam(value = "资源范围类型", required = false)
        @PathVariable(value = "scopeType", required = false)
            String scopeType,
        @ApiParam(value = "资源范围ID", required = false)
        @PathVariable(value = "scopeId", required = false)
            String scopeId,
        @ApiParam(value = "作业实例ID", name = "taskInstanceId", required = true)
        @PathVariable("taskInstanceId")
            Long taskInstanceId
    );

    @ApiOperation(value = "获取作业步骤执行信息", produces = "application/json")
    @GetMapping(value = {"/app/{appId}/step-execution-result/{stepInstanceId}/{executeCount}",
        "/app/{appId}/step-execution-result/{stepInstanceId}",
        "/scope/{scopeType}/{scopeId}/step-execution-result/{stepInstanceId}/{executeCount}",
        "/scope/{scopeType}/{scopeId}/step-execution-result/{stepInstanceId}"
    })
    Response<StepExecutionDetailVO> getStepExecutionResult(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiIgnore
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @ApiParam(value = "资源范围类型", required = false)
        @PathVariable(value = "scopeType", required = false)
            String scopeType,
        @ApiParam(value = "资源范围ID", required = false)
        @PathVariable(value = "scopeId", required = false)
            String scopeId,
        @ApiParam(value = "步骤实例ID", name = "stepInstanceId", required = true)
        @PathVariable("stepInstanceId")
            Long stepInstanceId,
        @ApiParam(value = "执行次数，首次传0", name = "executeCount", required = true)
        @PathVariable(value = "executeCount", required = false)
            Integer executeCount,
        @ApiParam(value = "任务执行结果", name = "resultType")
        @RequestParam(value = "resultType", required = false)
            Integer resultType,
        @ApiParam(value = "用户脚本输出的结果分组tag", name = "tag")
        @RequestParam(value = "tag", required = false)
            String tag,
        @ApiParam(value = "结果分组下返回的ip最大数", name = "maxIpsPerResultGroup")
        @RequestParam(value = "maxIpsPerResultGroup", required = false)
            Integer maxIpsPerResultGroup,
        @ApiParam(value = "日志搜索关键词", name = "keyword", required = true)
        @RequestParam(value = "keyword", required = false)
            String keyword,
        @ApiParam(value = "过滤ip,支持模糊匹配", name = "searchIp")
        @RequestParam(value = "searchIp", required = false)
            String searchIp,
        @ApiParam(value = "排序字段，当前支持totalTime|cloudAreaId|exitCode", name = "orderField")
        @RequestParam(value = "orderField", required = false)
            String orderField,
        @ApiParam(value = "排序顺序,0:降序;1:升序", name = "order")
        @RequestParam(value = "order", required = false)
            Integer order
    );

    @ApiOperation(value = "获取快速作业的步骤执行信息", produces = "application/json")
    @GetMapping(value = {"/app/{appId}/step-execution-result/taskInstanceId/{taskInstanceId}",
        "/scope/{scopeType}/{scopeId}/step-execution-result/taskInstanceId/{taskInstanceId}"})
    Response<StepExecutionDetailVO> getFastTaskStepExecutionResult(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiIgnore
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @ApiParam(value = "资源范围类型", required = false)
        @PathVariable(value = "scopeType", required = false)
            String scopeType,
        @ApiParam(value = "资源范围ID", required = false)
        @PathVariable(value = "scopeId", required = false)
            String scopeId,
        @ApiParam(value = "任务实例ID", name = "taskInstanceId", required = true)
        @PathVariable("taskInstanceId")
            Long taskInstanceId,
        @ApiParam(value = "任务执行结果", name = "resultType", required = false)
        @RequestParam(value = "resultType", required = false)
            Integer resultType,
        @ApiParam(value = "用户脚本输出的结果分组tag", name = "tag", required = false)
        @RequestParam(value = "tag", required = false)
            String tag,
        @ApiParam(value = "结果分组下返回的ip最大数", name = "maxIpsPerResultGroup", required = false)
        @RequestParam(value = "maxIpsPerResultGroup", required = false)
            Integer maxIpsPerResultGroup,
        @ApiParam(value = "排序字段，当前支持totalTime|cloudAreaId|exitCode", name = "orderField")
        @RequestParam(value = "orderField", required = false)
            String orderField,
        @ApiParam(value = "排序顺序,0:降序;1:升序", name = "searchIp")
        @RequestParam(value = "order", required = false)
            Integer order
    );

    @ApiOperation(value = "获取IP对应的脚本日志内容", produces = "application/json")
    @GetMapping(value = {"/app/{appId}/step-execution-result/log-content/{stepInstanceId}/{executeCount}/{ip}",
        "/scope/{scopeType}/{scopeId}/step-execution-result/log-content/{stepInstanceId}/{executeCount}/{ip}"})
    Response<IpScriptLogContentVO> getScriptLogContentByIp(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiIgnore
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @ApiParam(value = "资源范围类型", required = false)
        @PathVariable(value = "scopeType", required = false)
            String scopeType,
        @ApiParam(value = "资源范围ID", required = false)
        @PathVariable(value = "scopeId", required = false)
            String scopeId,
        @ApiParam(value = "步骤实例ID", name = "stepInstanceId", required = true)
        @PathVariable("stepInstanceId")
            Long stepInstanceId,
        @ApiParam(value = "执行次数，首次传0", name = "executeCount", required = true)
        @PathVariable("executeCount")
            Integer executeCount,
        @ApiParam(value = "IP，格式为云区域ID:IP,比如1:10.10.10.10", name = "ip", required = true)
        @PathVariable("ip") String ip
    );

    @ApiOperation(value = "获取文件分发步骤IP对应的日志", produces = "application/json")
    @GetMapping(value = {"/app/{appId}/step-execution-result/log-content/file/{stepInstanceId}/{executeCount}/{ip}",
        "/scope/{scopeType}/{scopeId}/step-execution-result/log-content/file/{stepInstanceId}/{executeCount}/{ip}"})
    Response<IpFileLogContentVO> getFileLogContentByIp(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiIgnore
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @ApiParam(value = "资源范围类型", required = false)
        @PathVariable(value = "scopeType", required = false)
            String scopeType,
        @ApiParam(value = "资源范围ID", required = false)
        @PathVariable(value = "scopeId", required = false)
            String scopeId,
        @ApiParam(value = "步骤实例ID", name = "stepInstanceId", required = true)
        @PathVariable("stepInstanceId")
            Long stepInstanceId,
        @ApiParam(value = "执行次数，首次传0", name = "executeCount", required = true)
        @PathVariable("executeCount")
            Integer executeCount,
        @ApiParam(value = "IP，格式为云区域ID:IP,比如1:10.10.10.10", name = "ip", required = true)
        @PathVariable("ip")
            String ip,
        @ApiParam(value = "文件任务上传下载标识,upload-上传,download-下载", name = "mode", required = true)
        @RequestParam(value = "mode")
            String mode
    );

    @ApiOperation(value = "获取文件分发步骤文件任务ID对应的执行日志", produces = "application/json")
    @PostMapping(value = {
        "/app/{appId}/step-execution-result/log-content/file/{stepInstanceId}/{executeCount}/query-by-ids",
        "/scope/{scopeType}/{scopeId}/step-execution-result/log-content/file/{stepInstanceId}/{executeCount}/query-by" +
            "-ids"})
    Response<List<FileDistributionDetailVO>> getFileLogContentByFileTaskIds(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiIgnore
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @ApiParam(value = "资源范围类型", required = false)
        @PathVariable(value = "scopeType", required = false)
            String scopeType,
        @ApiParam(value = "资源范围ID", required = false)
        @PathVariable(value = "scopeId", required = false)
            String scopeId,
        @ApiParam(value = "步骤实例ID", name = "stepInstanceId", required = true)
        @PathVariable("stepInstanceId")
            Long stepInstanceId,
        @ApiParam(value = "执行次数，首次传0", name = "executeCount", required = true)
        @PathVariable("executeCount")
            Integer executeCount,
        @ApiParam(value = "文件任务ID列表", name = "taskIds", required = true)
        @RequestBody
            List<String> taskIds
    );

    @ApiOperation(value = "获取执行步骤-主机对应的变量列表", produces = "application/json")
    @GetMapping(value = {"/app/{appId}/step-execution-result/variable/{stepInstanceId}/{ip}",
        "/scope/{scopeType}/{scopeId}/step-execution-result/variable/{stepInstanceId}/{ip}"})
    Response<List<ExecuteVariableVO>> getStepVariableByIp(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiIgnore
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @ApiParam(value = "资源范围类型", required = false)
        @PathVariable(value = "scopeType", required = false)
            String scopeType,
        @ApiParam(value = "资源范围ID", required = false)
        @PathVariable(value = "scopeId", required = false)
            String scopeId,
        @ApiParam(value = "步骤实例ID", name = "stepInstanceId", required = true)
        @PathVariable("stepInstanceId")
            Long stepInstanceId,
        @ApiParam(value = "ip", name = "ip", required = true)
        @PathVariable("ip")
            String ip
    );

    @ApiOperation(value = "获取执行结果分组下的主机列表", produces = "application/json")
    @GetMapping(value = {"/app/{appId}/step-execution-result/hosts/{stepInstanceId}/{executeCount}",
        "/scope/{scopeType}/{scopeId}/step-execution-result/hosts/{stepInstanceId}/{executeCount}"})
    Response<List<HostDTO>> getHostsByResultType(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiIgnore
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @ApiParam(value = "资源范围类型", required = false)
        @PathVariable(value = "scopeType", required = false)
            String scopeType,
        @ApiParam(value = "资源范围ID", required = false)
        @PathVariable(value = "scopeId", required = false)
            String scopeId,
        @ApiParam(value = "步骤实例ID", name = "stepInstanceId", required = true)
        @PathVariable("stepInstanceId")
            Long stepInstanceId,
        @ApiParam(value = "执行次数，首次传0", name = "executeCount", required = true)
        @PathVariable("executeCount")
            Integer executeCount,
        @ApiParam(value = "任务执行结果", name = "resultType", required = true)
        @RequestParam(value = "resultType")
            Integer resultType,
        @ApiParam(value = "用户脚本输出的结果分组tag", name = "tag", required = false)
        @RequestParam(value = "tag", required = false)
            String tag,
        @ApiParam(value = "关键字", name = "keyword")
        @RequestParam(value = "keyword", required = false)
            String keyword
    );

    @ApiOperation(value = "获取步骤执行历史", produces = "application/json")
    @GetMapping(value = {"/app/{appId}/step-execution-history/{stepInstanceId}",
        "/scope/{scopeType}/{scopeId}/step-execution-history/{stepInstanceId}"})
    Response<List<StepExecutionRecordVO>> listStepExecutionHistory(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiIgnore
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @ApiParam(value = "资源范围类型", required = false)
        @PathVariable(value = "scopeType", required = false)
            String scopeType,
        @ApiParam(value = "资源范围ID", required = false)
        @PathVariable(value = "scopeId", required = false)
            String scopeId,
        @ApiParam(value = "步骤实例ID", name = "stepInstanceId", required = true)
        @PathVariable("stepInstanceId")
            Long stepInstanceId
    );

}
