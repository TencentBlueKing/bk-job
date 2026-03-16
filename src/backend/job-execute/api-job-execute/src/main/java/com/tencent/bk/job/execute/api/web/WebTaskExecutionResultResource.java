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
import com.tencent.bk.job.execute.model.web.vo.IpScriptLogContentVO;
import com.tencent.bk.job.execute.model.web.vo.StepExecutionDetailV2VO;
import com.tencent.bk.job.execute.model.web.vo.StepExecutionDetailVO;
import com.tencent.bk.job.execute.model.web.vo.StepExecutionRecordVO;
import com.tencent.bk.job.execute.model.web.vo.TaskExecuteResultVO;
import com.tencent.bk.job.execute.model.web.vo.TaskInstanceVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Hidden;

import jakarta.ws.rs.QueryParam;
import java.util.List;

/**
 * 作业执行情况API-前端调用
 */
@Tag(name = "job-execute:web:Task_Execution_Result")
@RequestMapping("/web/execution/scope/{scopeType}/{scopeId}")
@RestController
@WebAPI
public interface WebTaskExecutionResultResource {
    @Operation(summary = "获取作业执行历史列表", produces = "application/json")
    @GetMapping(value = {"/task-execution-history/list"})
    Response<PageData<TaskInstanceVO>> getTaskHistoryList(
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
        @Parameter(description = "任务名称", name = "taskName")
        @RequestParam(value = "taskName", required = false)
            String taskName,
        @Parameter(description = "任务ID", name = "taskInstanceId")
        @RequestParam(value = "taskInstanceId", required = false)
            Long taskInstanceId,
        @Parameter(description = "任务状态", name = "status")
        @RequestParam(value = "status", required = false)
            Integer status,
        @Parameter(description = "执行人", name = "operator")
        @RequestParam(value = "operator", required = false)
            String operator,
        @Parameter(description = "任务类型", name = "taskType")
        @RequestParam(value = "taskType", required = false)
            Integer taskType,
        @Parameter(description = "时区, 配合startTime和endTime使用", name = "timezone")
        @RequestParam(value = "timezone", required = false)
            String timezone,
        @Parameter(description = "开始时间", name = "startTime")
        @RequestParam(value = "startTime", required = false)
            String startTime,
        @Parameter(description = "结束时间", name = "endTime")
        @RequestParam(value = "endTime", required = false)
            String endTime,
        @Parameter(description = "时间范围，单位天;如果使用timeRange参数,将忽略startTime/endTime。最大值:30", name = "timeRange")
        @RequestParam(value = "timeRange", required = false)
            Integer timeRange,
        @Parameter(description = "耗时类型", name = "totalTimeType")
        @RequestParam(value = "totalTimeType", required = false)
            TaskTotalTimeTypeEnum totalTimeType,
        @Parameter(description = "分页-开始，默认值：0")
        @RequestParam(value = "start", required = false)
            Integer start,
        @Parameter(description = "分页-每页大小，默认值：10")
        @RequestParam(value = "pageSize", required = false)
            Integer pageSize,
        @Parameter(description = "分页-是否计算总数；默认值：true。计算总数可能会影响 API 性能, 必要场景才可使用")
        @RequestParam(value = "countPageTotal", required = false)
            Boolean countPageTotal,
        @Parameter(description = "定时任务ID")
        @RequestParam(value = "cronTaskId", required = false)
            Long cronTaskId,
        @Parameter(description = "执行方式,1-页面执行,2-API调用,3-定时任务。如果传多个用,分隔，比如:1,2", name = "startupModes")
        @RequestParam(value = "startupModes", required = false)
            String startupModes,
        @Parameter(description = "ip", name = "ip")
        @RequestParam(value = "ip", required = false)
            String ip
    );

    @GetMapping(value = {"/task-execution-result/{taskInstanceId}"})
    Response<TaskExecuteResultVO> getTaskExecutionResult(
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
        @Parameter(description = "作业实例ID", name = "taskInstanceId", required = true)
        @PathVariable("taskInstanceId")
            Long taskInstanceId
    );

    @Operation(summary = "获取作业步骤执行信息（废弃）", produces = "application/json")
    @GetMapping(value = {
        "/step-execution-result/{stepInstanceId}/{executeCount}",
        "/step-execution-result/{stepInstanceId}"
    })
    @Deprecated
    @CompatibleImplementation(name = "execute_object", deprecatedVersion = "3.9.x", type = CompatibleType.DEPLOY,
        explain = "发布完成后可以删除")
    Response<StepExecutionDetailVO> getStepExecutionResult(
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
        @Parameter(description = "步骤实例ID", name = "stepInstanceId", required = true)
        @PathVariable("stepInstanceId")
            Long stepInstanceId,
        @Parameter(description = "执行次数，首次传0", name = "executeCount", required = true)
        @PathVariable(value = "executeCount", required = false) Integer executeCount,
        @Parameter(description = "滚动执行批次。如果不传表示返回最新批次，传入0表示返回全部批次", name = "batch")
        @RequestParam(value = "batch", required = false) Integer batch,
        @Parameter(description = "任务执行结果", name = "resultType")
        @RequestParam(value = "resultType", required = false)
            Integer resultType,
        @Parameter(description = "用户脚本输出的结果分组tag", name = "tag")
        @RequestParam(value = "tag", required = false)
            String tag,
        @Parameter(description = "结果分组下返回的ip最大数", name = "maxIpsPerResultGroup")
        @RequestParam(value = "maxIpsPerResultGroup", required = false)
            Integer maxIpsPerResultGroup,
        @Parameter(description = "日志搜索关键词", name = "keyword", required = true)
        @RequestParam(value = "keyword", required = false)
            String keyword,
        @Parameter(description = "过滤ip,支持模糊匹配", name = "searchIp")
        @RequestParam(value = "searchIp", required = false)
            String searchIp,
        @Parameter(description = "排序字段，当前支持totalTime|cloudAreaId|exitCode", name = "orderField")
        @RequestParam(value = "orderField", required = false)
            String orderField,
        @Parameter(description = "排序顺序,0:降序;1:升序", name = "order")
        @RequestParam(value = "order", required = false)
            Integer order
    );

    @Operation(summary = "获取作业步骤执行信息", produces = "application/json")
    @GetMapping(value = {
        "/taskInstance/{taskInstanceId}/stepInstance/{stepInstanceId}/stepExecutionResult"
    })
    Response<StepExecutionDetailV2VO> getStepExecutionResult(
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
        @Parameter(description = "作业实例ID", name = "taskInstanceId", required = true)
        @PathVariable("taskInstanceId")
            Long taskInstanceId,
        @Parameter(description = "步骤实例ID", name = "stepInstanceId", required = true)
        @PathVariable("stepInstanceId")
            Long stepInstanceId,
        @Parameter(description = "执行次数,如果不传表示获取最近一次的执行结果", name = "executeCount")
        @RequestParam(value = "executeCount", required = false) Integer executeCount,
        @Parameter(description = "滚动执行批次。如果不传表示返回最新批次，传入0表示返回全部批次", name = "batch")
        @RequestParam(value = "batch", required = false) Integer batch,
        @Parameter(description = "任务执行结果", name = "resultType")
        @RequestParam(value = "resultType", required = false)
            Integer resultType,
        @Parameter(description = "用户脚本输出的结果分组tag", name = "tag")
        @RequestParam(value = "tag", required = false)
            String tag,
        @Parameter(description = "结果分组下返回的最大任务数", name = "maxExecuteObjectPerResultGroup")
        @RequestParam(value = "maxTasksPerResultGroup", required = false)
            Integer maxTasksPerResultGroup,
        @Parameter(description = "日志搜索关键词", name = "keyword", required = true)
        @RequestParam(value = "keyword", required = false)
            String keyword,
        @Parameter(description = "过滤ip,支持模糊匹配", name = "searchIp")
        @RequestParam(value = "searchIp", required = false)
            String searchIp,
        @Parameter(description = "排序字段，当前支持totalTime|cloudAreaId|exitCode", name = "orderField")
        @RequestParam(value = "orderField", required = false)
            String orderField,
        @Parameter(description = "排序顺序,0:降序;1:升序", name = "order")
        @RequestParam(value = "order", required = false)
            Integer order
    );

    @Operation(summary = "获取主机对应的脚本日志内容（废弃）", produces = "application/json")
    @GetMapping(value = {"/step-execution-result/log-content/{stepInstanceId}/{executeCount}/host/{hostId}"})
    @Deprecated
    @CompatibleImplementation(name = "execute_object", deprecatedVersion = "3.9.x", type = CompatibleType.DEPLOY,
        explain = "发布完成后可以删除")
    Response<IpScriptLogContentVO> getScriptLogContentByHost(
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
        @Parameter(description = "步骤实例ID", name = "stepInstanceId", required = true)
        @PathVariable("stepInstanceId")
            Long stepInstanceId,
        @Parameter(description = "执行次数，首次传0", name = "executeCount", required = true)
        @PathVariable("executeCount")
            Integer executeCount,
        @Parameter(description = "主机ID,优先级比ip参数高", name = "hostId")
        @PathVariable(value = "hostId", required = false) Long hostId,
        @Parameter(description = "滚动批次，非滚动步骤不需要传入", name = "batch")
        @RequestParam(value = "batch", required = false) Integer batch
    );

    @Operation(summary = "获取执行对象对应的脚本日志内容", produces = "application/json")
    @GetMapping(value = {"/taskInstance/{taskInstanceId}/stepInstance/{stepInstanceId}/executeObject" +
        "/{executeObjectType}/{executeObjectResourceId}/scriptLog"})
    Response<ExecuteObjectScriptLogVO> getScriptLogContentByExecuteObject(
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
        @Parameter(description = "作业实例ID", name = "taskInstanceId", required = true)
        @PathVariable("taskInstanceId")
            Long taskInstanceId,
        @Parameter(description = "步骤实例ID", name = "stepInstanceId", required = true)
        @PathVariable("stepInstanceId")
            Long stepInstanceId,
        @Parameter(description = "执行对象类型", name = "executeObjectType", required = true)
        @PathVariable(value = "executeObjectType")
            Integer executeObjectType,
        @Parameter(description = "执行对象资源 ID", name = "executeObjectType", required = true)
        @PathVariable(value = "executeObjectResourceId")
            Long executeObjectResourceId,
        @Parameter(description = "执行次数,如果不传表示获取最近一次的执行结果", name = "executeCount")
        @RequestParam(value = "executeCount", required = false)
            Integer executeCount,
        @Parameter(description = "滚动批次，非滚动步骤不需要传入", name = "batch")
        @RequestParam(value = "batch", required = false)
            Integer batch
    );

    @Operation(summary = "获取文件分发步骤执行对象对应的日志", produces = "application/json")
    @GetMapping(value = {"/taskInstance/{taskInstanceId}/stepInstance/{stepInstanceId}/executeObject" +
        "/{executeObjectType}/{executeObjectResourceId}/fileLog"})
    Response<ExecuteObjectFileLogVO> getFileLogContentByExecuteObject(
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
        @Parameter(description = "作业实例ID", name = "taskInstanceId", required = true)
        @PathVariable("taskInstanceId")
            Long taskInstanceId,
        @Parameter(description = "步骤实例ID", name = "stepInstanceId", required = true)
        @PathVariable("stepInstanceId")
            Long stepInstanceId,
        @Parameter(description = "执行对象类型", name = "executeObjectType", required = true)
        @PathVariable(value = "executeObjectType")
            Integer executeObjectType,
        @Parameter(description = "执行对象资源 ID", name = "executeObjectType", required = true)
        @PathVariable(value = "executeObjectResourceId")
            Long executeObjectResourceId,
        @Parameter(description = "执行次数,如果不传表示获取最近一次的执行结果", name = "executeCount")
        @RequestParam(value = "executeCount", required = false)
            Integer executeCount,
        @Parameter(description = "滚动批次，非滚动步骤不需要传入", name = "batch")
        @RequestParam(value = "batch", required = false)
            Integer batch,
        @Parameter(description = "文件任务上传下载标识,0-上传,1-下载", name = "mode", required = true)
        @RequestParam(value = "mode")
            Integer mode
    );

    @Operation(summary = "获取文件分发步骤文件任务ID对应的执行日志", produces = "application/json")
    @PostMapping(value = {"/taskInstance/{taskInstanceId}/stepInstance/{stepInstanceId}/fileLog/queryByIds"})
    Response<List<FileDistributionDetailV2VO>> getFileLogContentByFileTaskIds(
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
        @Parameter(description = "作业实例ID", name = "taskInstanceId", required = true)
        @PathVariable("taskInstanceId")
            Long taskInstanceId,
        @Parameter(description = "步骤实例ID", name = "stepInstanceId", required = true)
        @PathVariable("stepInstanceId")
            Long stepInstanceId,
        @Parameter(description = "执行次数,如果不传表示获取最近一次的执行结果", name = "executeCount")
        @RequestParam(value = "executeCount", required = false)
            Integer executeCount,
        @Parameter(description = "滚动批次，非滚动步骤不需要传入", name = "batch")
        @RequestParam(value = "batch", required = false)
            Integer batch,
        @Parameter(description = "文件任务ID列表", name = "taskIds", required = true)
        @RequestBody
            List<String> taskIds
    );

    @Operation(summary = "获取执行步骤-主机对应的变量列表（废弃）", produces = "application/json")
    @GetMapping(value = {"/step-execution-result/step/{stepInstanceId}/variables"})
    @Deprecated
    @CompatibleImplementation(name = "execute_object", deprecatedVersion = "3.9.x", type = CompatibleType.DEPLOY,
        explain = "发布完成后可以删除")
    Response<List<ExecuteVariableVO>> getStepVariableByHost(
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
        @Parameter(description = "步骤实例ID", name = "stepInstanceId", required = true)
        @PathVariable("stepInstanceId")
            Long stepInstanceId,
        @Parameter(description = "hostId", name = "主机ID")
        @QueryParam(value = "hostId")
            Long hostId,
        @Parameter(description = "ip", name = "云区域ID:IPv4，为了兼容历史数据的查询保留；如果返回的任务中包含ip，那么需要传入")
        @QueryParam(value = "ip")
            String ip
    );

    @Operation(summary = "获取执行步骤-执行对象对应的变量列表", produces = "application/json")
    @GetMapping(value = {"/taskInstance/{taskInstanceId}/stepInstance/{stepInstanceId}/executeObject" +
        "/{executeObjectType}/{executeObjectResourceId}/variables"})
    Response<List<ExecuteVariableVO>> getStepVariableByExecuteObject(
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
        @Parameter(description = "作业实例ID", name = "taskInstanceId", required = true)
        @PathVariable("taskInstanceId")
            Long taskInstanceId,
        @Parameter(description = "步骤实例ID", name = "stepInstanceId", required = true)
        @PathVariable("stepInstanceId")
            Long stepInstanceId,
        @Parameter(description = "执行对象类型", name = "executeObjectType", required = true)
        @PathVariable(value = "executeObjectType")
            Integer executeObjectType,
        @Parameter(description = "执行对象资源 ID", name = "executeObjectType", required = true)
        @PathVariable(value = "executeObjectResourceId")
            Long executeObjectResourceId
    );

    @Operation(summary = "获取执行结果分组下的主机列表（废弃）", produces = "application/json")
    @GetMapping(value = {"/step-execution-result/hosts/{stepInstanceId}/{executeCount}"})
    @Deprecated
    @CompatibleImplementation(name = "execute_object", deprecatedVersion = "3.9.x", type = CompatibleType.DEPLOY,
        explain = "发布完成后可以删除")
    Response<List<HostDTO>> getHostsByResultType(
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
        @Parameter(description = "步骤实例ID", name = "stepInstanceId", required = true)
        @PathVariable("stepInstanceId")
            Long stepInstanceId,
        @Parameter(description = "执行次数，首次传0", name = "executeCount", required = true)
        @PathVariable("executeCount") Integer executeCount,
        @Parameter(description = "滚动执行批次，该步骤为滚动步骤时并且用户指定了批次的场景下需要传入该参数", name = "batch")
        @RequestParam(value = "batch", required = false) Integer batch,
        @Parameter(description = "任务执行结果", name = "resultType", required = true)
        @RequestParam(value = "resultType")
            Integer resultType,
        @Parameter(description = "用户脚本输出的结果分组tag", name = "tag")
        @RequestParam(value = "tag", required = false)
            String tag,
        @Parameter(description = "关键字", name = "keyword")
        @RequestParam(value = "keyword", required = false)
            String keyword
    );

    @Operation(summary = "获取执行结果分组下的执行对象列表", produces = "application/json")
    @GetMapping(value = {"/taskInstance/{taskInstanceId}/stepInstance/{stepInstanceId}/{executeCount}/executeObjects"})
    Response<List<ExecuteObjectVO>> getExecuteObjectsByResultType(
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
        @Parameter(description = "作业实例ID", name = "taskInstanceId", required = true)
        @PathVariable("taskInstanceId")
            Long taskInstanceId,
        @Parameter(description = "步骤实例ID", name = "stepInstanceId", required = true)
        @PathVariable("stepInstanceId")
            Long stepInstanceId,
        @Parameter(description = "执行次数", name = "executeCount", required = true)
        @PathVariable("executeCount") Integer executeCount,
        @Parameter(description = "滚动执行批次，该步骤为滚动步骤时并且用户指定了批次的场景下需要传入该参数", name = "batch")
        @RequestParam(value = "batch", required = false) Integer batch,
        @Parameter(description = "任务执行结果", name = "resultType", required = true)
        @RequestParam(value = "resultType")
            Integer resultType,
        @Parameter(description = "用户脚本输出的结果分组tag", name = "tag")
        @RequestParam(value = "tag", required = false)
            String tag,
        @Parameter(description = "关键字", name = "keyword")
        @RequestParam(value = "keyword", required = false)
            String keyword
    );

    @Operation(summary = "获取步骤执行历史", produces = "application/json")
    @GetMapping(value = {"/step-execution-history/{stepInstanceId}"})
    @Deprecated
    @CompatibleImplementation(name = "dao_add_task_instance_id", deprecatedVersion = "3.11.x",
        type = CompatibleType.DEPLOY, explain = "发布完成后可以删除")
    Response<List<StepExecutionRecordVO>> listStepExecutionHistory(
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
        @Parameter(description = "步骤实例ID", name = "stepInstanceId", required = true)
        @PathVariable("stepInstanceId")
            Long stepInstanceId,
        @Parameter(description = "滚动批次，非滚动步骤不需要传入", name = "batch")
        @RequestParam(value = "batch", required = false)
            Integer batch
    );


    @Operation(summary = "获取步骤执行历史", produces = "application/json")
    @GetMapping(value = {"/taskInstance/{taskInstanceId}/stepInstance/{stepInstanceId}/stepExecutionHistory"})
    Response<List<StepExecutionRecordVO>> listStepExecutionHistoryV2(
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
        @Parameter(description = "作业实例ID", name = "taskInstanceId", required = true)
        @PathVariable("taskInstanceId")
            Long taskInstanceId,
        @Parameter(description = "步骤实例ID", name = "stepInstanceId", required = true)
        @PathVariable("stepInstanceId")
            Long stepInstanceId,
        @Parameter(description = "滚动批次，非滚动步骤不需要传入，0表示获取所有批次的数据，不传表示获取当前批次数据", name = "batch")
        @RequestParam(value = "batch", required = false)
            Integer batch
    );

}
