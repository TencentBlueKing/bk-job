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

import com.tencent.bk.job.common.annotation.CompatibleImplementation;
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

import javax.ws.rs.QueryParam;
import java.util.List;

/**
 * 作业执行情况API-前端调用
 */
@Api(tags = {"job-execute:web:Task_Execution_Result"})
@RequestMapping("/web/execution/scope/{scopeType}/{scopeId}")
@RestController
@WebAPI
public interface WebTaskExecutionResultResource {
    @ApiOperation(value = "获取作业执行历史列表", produces = "application/json")
    @GetMapping(value = {"/task-execution-history/list"})
    Response<PageData<TaskInstanceVO>> getTaskHistoryList(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiIgnore
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @ApiParam(value = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @ApiParam(value = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @ApiParam(value = "任务名称", name = "taskName")
        @RequestParam(value = "taskName", required = false)
            String taskName,
        @ApiParam(value = "任务ID", name = "taskInstanceId")
        @RequestParam(value = "taskInstanceId", required = false)
            Long taskInstanceId,
        @ApiParam(value = "任务状态", name = "status")
        @RequestParam(value = "status", required = false)
            Integer status,
        @ApiParam(value = "执行人", name = "operator")
        @RequestParam(value = "operator", required = false)
            String operator,
        @ApiParam(value = "任务类型", name = "taskType")
        @RequestParam(value = "taskType", required = false)
            Integer taskType,
        @ApiParam(value = "开始时间", name = "startTime")
        @RequestParam(value = "startTime", required = false)
            String startTime,
        @ApiParam(value = "结束时间", name = "endTime")
        @RequestParam(value = "endTime", required = false)
            String endTime,
        @ApiParam(value = "时间范围，单位天;如果使用timeRange参数,将忽略startTime/endTime。最大值:30", name = "timeRange")
        @RequestParam(value = "timeRange", required = false)
            Integer timeRange,
        @ApiParam(value = "耗时类型", name = "totalTimeType")
        @RequestParam(value = "totalTimeType", required = false)
            TaskTotalTimeTypeEnum totalTimeType,
        @ApiParam(value = "分页-开始")
        @RequestParam(value = "start", required = false)
            Integer start,
        @ApiParam(value = "分页-每页大小")
        @RequestParam(value = "pageSize", required = false)
            Integer pageSize,
        @ApiParam(value = "定时任务ID")
        @RequestParam(value = "cronTaskId", required = false)
            Long cronTaskId,
        @ApiParam(value = "执行方式,1-页面执行,2-API调用,3-定时任务。如果传多个用,分隔，比如:1,2", name = "startupModes")
        @RequestParam(value = "startupModes", required = false)
            String startupModes,
        @ApiParam(value = "ip", name = "ip")
        @RequestParam(value = "ip", required = false)
            String ip
    );

    @GetMapping(value = {"/task-execution-result/{taskInstanceId}"})
    Response<TaskExecuteResultVO> getTaskExecutionResult(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiIgnore
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @ApiParam(value = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @ApiParam(value = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @ApiParam(value = "作业实例ID", name = "taskInstanceId", required = true)
        @PathVariable("taskInstanceId")
            Long taskInstanceId
    );

    @ApiOperation(value = "获取作业步骤执行信息", produces = "application/json")
    @GetMapping(value = {
        "/step-execution-result/{stepInstanceId}/{executeCount}",
        "/step-execution-result/{stepInstanceId}"
    })
    Response<StepExecutionDetailVO> getStepExecutionResult(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiIgnore
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @ApiParam(value = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @ApiParam(value = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @ApiParam(value = "步骤实例ID", name = "stepInstanceId", required = true)
        @PathVariable("stepInstanceId")
            Long stepInstanceId,
        @ApiParam(value = "执行次数，首次传0", name = "executeCount", required = true)
        @PathVariable(value = "executeCount", required = false) Integer executeCount,
        @ApiParam(value = "滚动执行批次。如果不传表示返回最新批次，传入0表示返回全部批次", name = "batch")
        @RequestParam(value = "batch", required = false) Integer batch,
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
    @GetMapping(value = {"/step-execution-result/taskInstanceId/{taskInstanceId}"})
    Response<StepExecutionDetailVO> getFastTaskStepExecutionResult(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiIgnore
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @ApiParam(value = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @ApiParam(value = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @ApiParam(value = "任务实例ID", name = "taskInstanceId", required = true)
        @PathVariable("taskInstanceId") Long taskInstanceId,
        @ApiParam(value = "滚动执行批次", name = "batch")
        @RequestParam(value = "batch", required = false) Integer batch,
        @ApiParam(value = "任务执行结果", name = "resultType")
        @RequestParam(value = "resultType", required = false)
            Integer resultType,
        @ApiParam(value = "用户脚本输出的结果分组tag", name = "tag")
        @RequestParam(value = "tag", required = false)
            String tag,
        @ApiParam(value = "结果分组下返回的ip最大数", name = "maxIpsPerResultGroup")
        @RequestParam(value = "maxIpsPerResultGroup", required = false)
            Integer maxIpsPerResultGroup,
        @ApiParam(value = "排序字段，当前支持totalTime|cloudAreaId|exitCode", name = "orderField")
        @RequestParam(value = "orderField", required = false)
            String orderField,
        @ApiParam(value = "排序顺序,0:降序;1:升序", name = "searchIp")
        @RequestParam(value = "order", required = false)
            Integer order
    );

    @CompatibleImplementation(name = "ipv6", explain = "考虑到历史记录只有ip数据，所以需要同时兼容hostId/ip两种方式",
        deprecatedVersion = "3.7.x")
    @ApiOperation(value = "获取主机对应的脚本日志内容", produces = "application/json")
    @GetMapping(value = {"/step-execution-result/log-content/{stepInstanceId}/{executeCount}/{ip}",
        "/step-execution-result/log-content/{stepInstanceId}/{executeCount}/host/{hostId}"})
    Response<IpScriptLogContentVO> getScriptLogContentByHost(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiIgnore
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @ApiParam(value = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @ApiParam(value = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @ApiParam(value = "步骤实例ID", name = "stepInstanceId", required = true)
        @PathVariable("stepInstanceId")
            Long stepInstanceId,
        @ApiParam(value = "执行次数，首次传0", name = "executeCount", required = true)
        @PathVariable("executeCount")
            Integer executeCount,
        @ApiParam(value = "主机ip(云区域ID:ip)，兼容历史版本数据，后续会删除；建议使用hostId", name = "ip")
        @PathVariable(value = "ip", required = false) String ip,
        @ApiParam(value = "主机ID,优先级比ip参数高", name = "hostId")
        @PathVariable(value = "hostId", required = false) Long hostId,
        @ApiParam(value = "滚动批次，非滚动步骤不需要传入", name = "batch")
        @RequestParam(value = "batch", required = false) Integer batch
    );

    @CompatibleImplementation(name = "ipv6", explain = "考虑到历史记录只有ip数据，所以需要同时兼容hostId/ip两种方式",
        deprecatedVersion = "3.7.x")
    @ApiOperation(value = "获取文件分发步骤主机对应的日志", produces = "application/json")
    @GetMapping(value = {"/step-execution-result/log-content/file/{stepInstanceId}/{executeCount}/{ip}",
        "/step-execution-result/log-content/file/{stepInstanceId}/{executeCount}/host/{hostId}"})
    Response<IpFileLogContentVO> getFileLogContentByHost(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiIgnore
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @ApiParam(value = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @ApiParam(value = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @ApiParam(value = "步骤实例ID", name = "stepInstanceId", required = true)
        @PathVariable("stepInstanceId")
            Long stepInstanceId,
        @ApiParam(value = "执行次数，首次传0", name = "executeCount", required = true)
        @PathVariable("executeCount")
            Integer executeCount,
        @ApiParam(value = "主机ip(云区域ID:ip)，兼容历史版本数据，后续会删除；建议使用hostId", name = "ip")
        @PathVariable(value = "ip", required = false) String ip,
        @ApiParam(value = "主机ID,优先级比ip参数高", name = "hostId")
        @PathVariable(value = "hostId", required = false) Long hostId,
        @ApiParam(value = "文件任务上传下载标识,upload-上传,download-下载", name = "mode", required = true)
        @RequestParam(value = "mode")
            String mode,
        @ApiParam(value = "滚动批次，非滚动步骤不需要传入", name = "batch")
        @RequestParam(value = "batch", required = false)
            Integer batch
    );

    @ApiOperation(value = "获取文件分发步骤文件任务ID对应的执行日志", produces = "application/json")
    @PostMapping(value = {"/step-execution-result/log-content/file/{stepInstanceId}/{executeCount}/query-by-ids"})
    Response<List<FileDistributionDetailVO>> getFileLogContentByFileTaskIds(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiIgnore
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @ApiParam(value = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @ApiParam(value = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @ApiParam(value = "步骤实例ID", name = "stepInstanceId", required = true)
        @PathVariable("stepInstanceId")
            Long stepInstanceId,
        @ApiParam(value = "执行次数，首次传0", name = "executeCount", required = true)
        @PathVariable("executeCount")
            Integer executeCount,
        @ApiParam(value = "滚动批次，非滚动步骤不需要传入", name = "batch")
        @RequestParam(value = "batch", required = false)
            Integer batch,
        @ApiParam(value = "文件任务ID列表", name = "taskIds", required = true)
        @RequestBody
            List<String> taskIds
    );

    @ApiOperation(value = "获取执行步骤-主机对应的变量列表", produces = "application/json")
    @GetMapping(value = {"/step-execution-result/variable/{stepInstanceId}/{ip}"})
    @CompatibleImplementation(name = "ipv6", explain = "兼容IPv6版本之前的使用并保存ip的执行历史数据，ipv6发布之后可删除")
    @Deprecated
    Response<List<ExecuteVariableVO>> getStepVariableByIp(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiIgnore
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @ApiParam(value = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @ApiParam(value = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @ApiParam(value = "步骤实例ID", name = "stepInstanceId", required = true)
        @PathVariable("stepInstanceId")
            Long stepInstanceId,
        @ApiParam(value = "ip", name = "ip", required = true)
        @PathVariable("ip")
            String ip
    );

    @ApiOperation(value = "获取执行步骤-主机对应的变量列表", produces = "application/json")
    @GetMapping(value = {"/step-execution-result/step/{stepInstanceId}/variables"})
    Response<List<ExecuteVariableVO>> getStepVariableByHost(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiIgnore
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @ApiParam(value = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @ApiParam(value = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @ApiParam(value = "步骤实例ID", name = "stepInstanceId", required = true)
        @PathVariable("stepInstanceId")
            Long stepInstanceId,
        @ApiParam(value = "hostId", name = "主机ID")
        @QueryParam(value = "hostId")
            Long hostId,
        @ApiParam(value = "ip", name = "云区域ID:IPv4，为了兼容历史数据的查询保留；如果返回的任务中包含ip，那么需要传入")
        @QueryParam(value = "ip")
            String ip
    );

    @ApiOperation(value = "获取执行结果分组下的主机列表", produces = "application/json")
    @GetMapping(value = {"/step-execution-result/hosts/{stepInstanceId}/{executeCount}"})
    Response<List<HostDTO>> getHostsByResultType(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiIgnore
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @ApiParam(value = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @ApiParam(value = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @ApiParam(value = "步骤实例ID", name = "stepInstanceId", required = true)
        @PathVariable("stepInstanceId")
            Long stepInstanceId,
        @ApiParam(value = "执行次数，首次传0", name = "executeCount", required = true)
        @PathVariable("executeCount") Integer executeCount,
        @ApiParam(value = "滚动执行批次，该步骤为滚动步骤时并且用户指定了批次的场景下需要传入该参数", name = "batch")
        @RequestParam(value = "batch", required = false) Integer batch,
        @ApiParam(value = "任务执行结果", name = "resultType", required = true)
        @RequestParam(value = "resultType")
            Integer resultType,
        @ApiParam(value = "用户脚本输出的结果分组tag", name = "tag")
        @RequestParam(value = "tag", required = false)
            String tag,
        @ApiParam(value = "关键字", name = "keyword")
        @RequestParam(value = "keyword", required = false)
            String keyword
    );

    @ApiOperation(value = "获取步骤执行历史", produces = "application/json")
    @GetMapping(value = {"/step-execution-history/{stepInstanceId}"})
    Response<List<StepExecutionRecordVO>> listStepExecutionHistory(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiIgnore
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @ApiParam(value = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @ApiParam(value = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @ApiParam(value = "步骤实例ID", name = "stepInstanceId", required = true)
        @PathVariable("stepInstanceId")
            Long stepInstanceId,
        @ApiParam(value = "滚动批次，非滚动步骤不需要传入", name = "batch")
        @RequestParam(value = "batch", required = false)
            Integer batch
    );

}
