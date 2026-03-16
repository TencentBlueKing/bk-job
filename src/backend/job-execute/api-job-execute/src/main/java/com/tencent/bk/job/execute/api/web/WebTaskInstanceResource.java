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
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.execute.model.web.vo.ExecuteStepVO;
import com.tencent.bk.job.execute.model.web.vo.ExecuteVariableVO;
import com.tencent.bk.job.execute.model.web.vo.TaskInstanceDetailVO;
import com.tencent.bk.job.execute.model.web.vo.TaskInstanceVO;
import com.tencent.bk.job.execute.model.web.vo.TaskOperationLogVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Hidden;

import java.util.List;

/**
 * 作业实例，步骤实例API-前端调用
 */
@Tag(name = "job-execute:web:Task_Step_Instance")
@RequestMapping("/web/execution/")
@RestController
@WebAPI
public interface WebTaskInstanceResource {
    @Deprecated
    @CompatibleImplementation(name = "dao_add_task_instance_id", deprecatedVersion = "3.11.x",
        type = CompatibleType.DEPLOY, explain = "发布完成后可以删除")
    @Operation(summary = "获取作业步骤实例详情", produces = "application/json")
    @GetMapping(value = {"/scope/{scopeType}/{scopeId}/task-instance/step_instance/{stepInstanceId}"})
    Response<ExecuteStepVO> getStepInstanceDetail(
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
        Long stepInstanceId
    );

    @Operation(summary = "获取作业步骤实例详情", produces = "application/json")
    @GetMapping(value = {
        "/scope/{scopeType}/{scopeId}/taskInstance/{taskInstanceId}/stepInstance/{stepInstanceId}/detail"})
    Response<ExecuteStepVO> getStepInstanceDetailV2(
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
        Long stepInstanceId
    );

    @Operation(summary = "获取作业实例全局参数", produces = "application/json")
    @GetMapping(value = {"/scope/{scopeType}/{scopeId}/task-instance/task-variables/{taskInstanceId}"})
    Response<List<ExecuteVariableVO>> getTaskInstanceVariables(
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

    @Operation(summary = "获取作业操作日志", produces = "application/json")
    @GetMapping(value = {"/scope/{scopeType}/{scopeId}/task-instance/operation-log/{taskInstanceId}"})
    Response<List<TaskOperationLogVO>> getTaskInstanceOperationLog(
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

    @Operation(summary = "获取作业实例详情,包括步骤列表和全局变量列表", produces = "application/json")
    @GetMapping(value = {"/scope/{scopeType}/{scopeId}/task-instance/{taskInstanceId}"})
    Response<TaskInstanceDetailVO> getTaskInstanceDetail(
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

    @Operation(summary = "获取作业实例基本信息", produces = "application/json")
    @GetMapping("/task_instance/{taskInstanceId}")
    Response<TaskInstanceVO> getTaskInstanceBasic(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username") String username,
        @Parameter(description = "作业实例ID", name = "taskInstanceId", required = true)
        @PathVariable("taskInstanceId")
        Long taskInstanceId
    );
}
