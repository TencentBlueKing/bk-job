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

package com.tencent.bk.job.execute.api.web;

import com.tencent.bk.job.common.annotation.CompatibleImplementation;
import com.tencent.bk.job.common.annotation.WebAPI;
import com.tencent.bk.job.common.constant.CompatibleType;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.execute.common.constants.TaskTotalTimeTypeEnum;
import com.tencent.bk.job.execute.model.web.vo.ExecuteObjectFileLogVO;
import com.tencent.bk.job.execute.model.web.vo.ExecuteObjectScriptLogVO;
import com.tencent.bk.job.execute.model.web.vo.ExecuteObjectVO;
import com.tencent.bk.job.execute.model.web.vo.ExecuteVariableVO;
import com.tencent.bk.job.execute.model.web.vo.FileDistributionDetailV2VO;
import com.tencent.bk.job.execute.model.web.vo.FileDistributionDetailVO;
import com.tencent.bk.job.execute.model.web.vo.IpFileLogContentVO;
import com.tencent.bk.job.execute.model.web.vo.IpScriptLogContentVO;
import com.tencent.bk.job.execute.model.web.vo.StepExecutionDetailV2VO;
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
        @ApiParam(value = "分页-开始，默认值：0")
        @RequestParam(value = "start", required = false)
            Integer start,
        @ApiParam(value = "分页-每页大小，默认值：10")
        @RequestParam(value = "pageSize", required = false)
            Integer pageSize,
        @ApiParam(value = "分页-是否计算总数；默认值：true。计算总数可能会影响 API 性能, 必要场景才可使用")
        @RequestParam(value = "countPageTotal", required = false)
            Boolean countPageTotal,
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

    @ApiOperation(value = "获取作业步骤执行信息（废弃）", produces = "application/json")
    @GetMapping(value = {
        "/step-execution-result/{stepInstanceId}/{executeCount}",
        "/step-execution-result/{stepInstanceId}"
    })
    @Deprecated
    @CompatibleImplementation(name = "execute_object", deprecatedVersion = "3.9.x", type = CompatibleType.DEPLOY,
        explain = "发布完成后可以删除")
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

    @ApiOperation(value = "获取作业步骤执行信息", produces = "application/json")
    @GetMapping(value = {
        "/taskInstance/{taskInstanceId}/stepInstance/{stepInstanceId}/stepExecutionResult"
    })
    Response<StepExecutionDetailV2VO> getStepExecutionResult(
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
            Long taskInstanceId,
        @ApiParam(value = "步骤实例ID", name = "stepInstanceId", required = true)
        @PathVariable("stepInstanceId")
            Long stepInstanceId,
        @ApiParam(value = "执行次数,如果不传表示获取最近一次的执行结果", name = "executeCount")
        @RequestParam(value = "executeCount", required = false) Integer executeCount,
        @ApiParam(value = "滚动执行批次。如果不传表示返回最新批次，传入0表示返回全部批次", name = "batch")
        @RequestParam(value = "batch", required = false) Integer batch,
        @ApiParam(value = "任务执行结果", name = "resultType")
        @RequestParam(value = "resultType", required = false)
            Integer resultType,
        @ApiParam(value = "用户脚本输出的结果分组tag", name = "tag")
        @RequestParam(value = "tag", required = false)
            String tag,
        @ApiParam(value = "结果分组下返回的最大任务数", name = "maxExecuteObjectPerResultGroup")
        @RequestParam(value = "maxTasksPerResultGroup", required = false)
            Integer maxTasksPerResultGroup,
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

    @ApiOperation(value = "获取主机对应的脚本日志内容（废弃）", produces = "application/json")
    @GetMapping(value = {"/step-execution-result/log-content/{stepInstanceId}/{executeCount}/host/{hostId}"})
    @Deprecated
    @CompatibleImplementation(name = "execute_object", deprecatedVersion = "3.9.x", type = CompatibleType.DEPLOY,
        explain = "发布完成后可以删除")
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
        @ApiParam(value = "主机ID,优先级比ip参数高", name = "hostId")
        @PathVariable(value = "hostId", required = false) Long hostId,
        @ApiParam(value = "滚动批次，非滚动步骤不需要传入", name = "batch")
        @RequestParam(value = "batch", required = false) Integer batch
    );

    @ApiOperation(value = "获取执行对象对应的脚本日志内容", produces = "application/json")
    @GetMapping(value = {"/taskInstance/{taskInstanceId}/stepInstance/{stepInstanceId}/executeObject" +
        "/{executeObjectType}/{executeObjectResourceId}/scriptLog"})
    Response<ExecuteObjectScriptLogVO> getScriptLogContentByExecuteObject(
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
            Long taskInstanceId,
        @ApiParam(value = "步骤实例ID", name = "stepInstanceId", required = true)
        @PathVariable("stepInstanceId")
            Long stepInstanceId,
        @ApiParam(value = "执行对象类型", name = "executeObjectType", required = true)
        @PathVariable(value = "executeObjectType")
            Integer executeObjectType,
        @ApiParam(value = "执行对象资源 ID", name = "executeObjectType", required = true)
        @PathVariable(value = "executeObjectResourceId")
            Long executeObjectResourceId,
        @ApiParam(value = "执行次数,如果不传表示获取最近一次的执行结果", name = "executeCount")
        @RequestParam(value = "executeCount", required = false)
            Integer executeCount,
        @ApiParam(value = "滚动批次，非滚动步骤不需要传入", name = "batch")
        @RequestParam(value = "batch", required = false)
            Integer batch
    );

    @ApiOperation(value = "获取文件分发步骤主机对应的日志(废弃）", produces = "application/json")
    @GetMapping(value = {"/step-execution-result/log-content/file/{stepInstanceId}/{executeCount}/host/{hostId}"})
    @Deprecated
    @CompatibleImplementation(name = "execute_object", deprecatedVersion = "3.9.x", type = CompatibleType.DEPLOY,
        explain = "发布完成后可以删除")
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
        @ApiParam(value = "主机ID", name = "hostId")
        @PathVariable(value = "hostId", required = false) Long hostId,
        @ApiParam(value = "文件任务上传下载标识,upload-上传,download-下载", name = "mode", required = true)
        @RequestParam(value = "mode")
            String mode,
        @ApiParam(value = "滚动批次，非滚动步骤不需要传入", name = "batch")
        @RequestParam(value = "batch", required = false)
            Integer batch
    );

    @ApiOperation(value = "获取文件分发步骤执行对象对应的日志", produces = "application/json")
    @GetMapping(value = {"/taskInstance/{taskInstanceId}/stepInstance/{stepInstanceId}/executeObject" +
        "/{executeObjectType}/{executeObjectResourceId}/fileLog"})
    Response<ExecuteObjectFileLogVO> getFileLogContentByExecuteObject(
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
            Long taskInstanceId,
        @ApiParam(value = "步骤实例ID", name = "stepInstanceId", required = true)
        @PathVariable("stepInstanceId")
            Long stepInstanceId,
        @ApiParam(value = "执行对象类型", name = "executeObjectType", required = true)
        @PathVariable(value = "executeObjectType")
            Integer executeObjectType,
        @ApiParam(value = "执行对象资源 ID", name = "executeObjectType", required = true)
        @PathVariable(value = "executeObjectResourceId")
            Long executeObjectResourceId,
        @ApiParam(value = "执行次数,如果不传表示获取最近一次的执行结果", name = "executeCount")
        @RequestParam(value = "executeCount", required = false)
            Integer executeCount,
        @ApiParam(value = "滚动批次，非滚动步骤不需要传入", name = "batch")
        @RequestParam(value = "batch", required = false)
            Integer batch,
        @ApiParam(value = "文件任务上传下载标识,0-上传,1-下载", name = "mode", required = true)
        @RequestParam(value = "mode")
            Integer mode
    );

    @ApiOperation(value = "获取文件分发步骤文件任务ID对应的执行日志（废弃）", produces = "application/json")
    @PostMapping(value = {"/step-execution-result/log-content/file/{stepInstanceId}/{executeCount}/query-by-ids"})
    @Deprecated
    @CompatibleImplementation(name = "execute_object", deprecatedVersion = "3.9.x", type = CompatibleType.DEPLOY,
        explain = "发布完成后可以删除")
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

    @ApiOperation(value = "获取文件分发步骤文件任务ID对应的执行日志", produces = "application/json")
    @PostMapping(value = {"/taskInstance/{taskInstanceId}/stepInstance/{stepInstanceId}/fileLog/queryByIds"})
    Response<List<FileDistributionDetailV2VO>> getFileLogContentByFileTaskIds(
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
            Long taskInstanceId,
        @ApiParam(value = "步骤实例ID", name = "stepInstanceId", required = true)
        @PathVariable("stepInstanceId")
            Long stepInstanceId,
        @ApiParam(value = "执行次数,如果不传表示获取最近一次的执行结果", name = "executeCount")
        @RequestParam(value = "executeCount", required = false)
            Integer executeCount,
        @ApiParam(value = "滚动批次，非滚动步骤不需要传入", name = "batch")
        @RequestParam(value = "batch", required = false)
            Integer batch,
        @ApiParam(value = "文件任务ID列表", name = "taskIds", required = true)
        @RequestBody
            List<String> taskIds
    );

    @ApiOperation(value = "获取执行步骤-主机对应的变量列表（废弃）", produces = "application/json")
    @GetMapping(value = {"/step-execution-result/step/{stepInstanceId}/variables"})
    @Deprecated
    @CompatibleImplementation(name = "execute_object", deprecatedVersion = "3.9.x", type = CompatibleType.DEPLOY,
        explain = "发布完成后可以删除")
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

    @ApiOperation(value = "获取执行步骤-执行对象对应的变量列表", produces = "application/json")
    @GetMapping(value = {"/taskInstance/{taskInstanceId}/stepInstance/{stepInstanceId}/executeObject" +
        "/{executeObjectType}/{executeObjectResourceId}/variables"})
    Response<List<ExecuteVariableVO>> getStepVariableByExecuteObject(
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
            Long taskInstanceId,
        @ApiParam(value = "步骤实例ID", name = "stepInstanceId", required = true)
        @PathVariable("stepInstanceId")
            Long stepInstanceId,
        @ApiParam(value = "执行对象类型", name = "executeObjectType", required = true)
        @PathVariable(value = "executeObjectType")
            Integer executeObjectType,
        @ApiParam(value = "执行对象资源 ID", name = "executeObjectType", required = true)
        @PathVariable(value = "executeObjectResourceId")
            Long executeObjectResourceId
    );

    @ApiOperation(value = "获取执行结果分组下的主机列表（废弃）", produces = "application/json")
    @GetMapping(value = {"/step-execution-result/hosts/{stepInstanceId}/{executeCount}"})
    @Deprecated
    @CompatibleImplementation(name = "execute_object", deprecatedVersion = "3.9.x", type = CompatibleType.DEPLOY,
        explain = "发布完成后可以删除")
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

    @ApiOperation(value = "获取执行结果分组下的执行对象列表", produces = "application/json")
    @GetMapping(value = {"/taskInstance/{taskInstanceId}/stepInstance/{stepInstanceId}/{executeCount}/executeObjects"})
    Response<List<ExecuteObjectVO>> getExecuteObjectsByResultType(
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
            Long taskInstanceId,
        @ApiParam(value = "步骤实例ID", name = "stepInstanceId", required = true)
        @PathVariable("stepInstanceId")
            Long stepInstanceId,
        @ApiParam(value = "执行次数", name = "executeCount", required = true)
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
