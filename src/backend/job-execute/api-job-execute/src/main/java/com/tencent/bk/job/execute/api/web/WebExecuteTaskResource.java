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
import com.tencent.bk.job.execute.model.web.request.RedoTaskRequest;
import com.tencent.bk.job.execute.model.web.request.WebFastExecuteScriptRequest;
import com.tencent.bk.job.execute.model.web.request.WebFastPushFileRequest;
import com.tencent.bk.job.execute.model.web.request.WebStepOperation;
import com.tencent.bk.job.execute.model.web.request.WebTaskExecuteRequest;
import com.tencent.bk.job.execute.model.web.vo.StepExecuteVO;
import com.tencent.bk.job.execute.model.web.vo.StepOperationVO;
import com.tencent.bk.job.execute.model.web.vo.TaskExecuteVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Hidden;

/**
 * 作业执行API-前端调用
 */
@Tag(name = "job-execute:web:Task_Execute")
@RequestMapping("/web/execution/scope/{scopeType}/{scopeId}")
@RestController
@WebAPI
public interface WebExecuteTaskResource {

    @Operation(summary = "执行作业")
    @PostMapping(value = {"/task-execution"})
    Response<TaskExecuteVO> executeTask(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @Parameter(hidden = true)
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @Parameter(description = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @Parameter(description = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @Parameter(description = "执行作业请求报文", name = "webTaskExecuteRequest", required = true)
        @RequestBody
            WebTaskExecuteRequest request
    );

    @Operation(summary = "重新执行作业")
    @PostMapping(value = {"/task-execution/redo-task"})
    Response<TaskExecuteVO> redoTask(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @Parameter(hidden = true)
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @Parameter(description = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @Parameter(description = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @Parameter(description = "重新执行作业请求报文", name = "redoTaskRequest", required = true)
        @RequestBody
            RedoTaskRequest request
    );

    @Operation(summary = "快速执行脚本")
    @PostMapping(value = {"/fast-execute-script"})
    Response<StepExecuteVO> fastExecuteScript(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @Parameter(hidden = true)
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @Parameter(description = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @Parameter(description = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @Parameter(description = "快速执行脚本请求报文", name = "webScriptExecuteRequest", required = true)
        @RequestBody
        @Validated
            WebFastExecuteScriptRequest request
    );

    @Operation(summary = "快速分发文件")
    @PostMapping(value = {"/fast-push-file"})
    Response<StepExecuteVO> fastPushFile(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @Parameter(hidden = true)
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @Parameter(description = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @Parameter(description = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @Parameter(description = "快速分发文件请求报文", name = "webPushFileRequest", required = true)
        @RequestBody
        @Validated
            WebFastPushFileRequest request
    );

    @Operation(summary = "执行作业步骤操作")
    @PostMapping(value = {"/do-step-operation/stepInstanceId/{stepInstanceId}"})
    @Deprecated
    @CompatibleImplementation(name = "dao_add_task_instance_id", deprecatedVersion = "3.11.x",
        type = CompatibleType.DEPLOY, explain = "发布完成后可以删除")
    Response<StepOperationVO> doStepOperation(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
        String username,
        @Parameter(hidden = true)
        @RequestAttribute(value = "appResourceScope")
        AppResourceScope appResourceScope,
        @Parameter(description = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
        String scopeType,
        @Parameter(description = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
        String scopeId,
        @Parameter(description = "步骤实例ID", required = true, example = "1")
        @PathVariable("stepInstanceId")
        Long stepInstanceId,
        @Parameter(description = "步骤实例操作请求报文", name = "operation", required = true)
        @RequestBody
        WebStepOperation operation
    );

    @Operation(summary = "执行作业步骤操作")
    @PostMapping(value = {"/taskInstance/{taskInstanceId}/stepInstance/{stepInstanceId}/operate"})
    Response<StepOperationVO> doStepOperationV2(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @Parameter(hidden = true)
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @Parameter(description = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @Parameter(description = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @Parameter(description = "作业实例ID", required = true, example = "1")
        @PathVariable("taskInstanceId")
            Long taskInstanceId,
        @Parameter(description = "步骤实例ID", required = true, example = "1")
        @PathVariable("stepInstanceId")
            Long stepInstanceId,
        @Parameter(description = "步骤实例操作请求报文", name = "operation", required = true)
        @RequestBody
            WebStepOperation operation
    );

    @Operation(summary = "终止作业")
    @PostMapping(value = {"/taskInstance/{taskInstanceId}/terminate"})
    Response terminateJob(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @Parameter(hidden = true)
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @Parameter(description = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @Parameter(description = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @Parameter(description = "作业实例ID", required = true, example = "1")
        @PathVariable("taskInstanceId")
            Long taskInstanceId
    );

}
