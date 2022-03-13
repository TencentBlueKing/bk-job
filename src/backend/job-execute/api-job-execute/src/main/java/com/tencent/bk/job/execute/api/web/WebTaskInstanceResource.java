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
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.execute.model.web.vo.ExecuteStepVO;
import com.tencent.bk.job.execute.model.web.vo.ExecuteVariableVO;
import com.tencent.bk.job.execute.model.web.vo.TaskInstanceDetailVO;
import com.tencent.bk.job.execute.model.web.vo.TaskInstanceVO;
import com.tencent.bk.job.execute.model.web.vo.TaskOperationLogVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;

/**
 * 作业实例，步骤实例API-前端调用
 */
@Api(tags = {"job-execute:web:Task_Step_Instance"})
@RequestMapping("/web/execution/")
@RestController
@WebAPI
public interface WebTaskInstanceResource {
    @ApiOperation(value = "获取作业步骤实例详情", produces = "application/json")
    @GetMapping(value = {"/app/{appId}/task-instance/step_instance/{stepInstanceId}",
        "/scope/{scopeType}/{scopeId}/task-instance/step_instance/{stepInstanceId}"})
    Response<ExecuteStepVO> getStepInstanceDetail(
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

    @ApiOperation(value = "获取作业实例全局参数", produces = "application/json")
    @GetMapping(value = {"/app/{appId}/task-instance/task-variables/{taskInstanceId}",
        "/scope/{scopeType}/{scopeId}/task-instance/task-variables/{taskInstanceId}"})
    Response<List<ExecuteVariableVO>> getTaskInstanceVariables(
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

    @ApiOperation(value = "获取作业操作日志", produces = "application/json")
    @GetMapping(value = {"/app/{appId}/task-instance/operation-log/{taskInstanceId}",
        "/scope/{scopeType}/{scopeId}/task-instance/operation-log/{taskInstanceId}"})
    Response<List<TaskOperationLogVO>> getTaskInstanceOperationLog(
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

    @ApiOperation(value = "获取作业实例详情,包括步骤列表和全局变量列表", produces = "application/json")
    @GetMapping(value = {"/app/{appId}/task-instance/{taskInstanceId}",
        "/scope/{scopeType}/{scopeId}/task-instance/{taskInstanceId}"})
    Response<TaskInstanceDetailVO> getTaskInstanceDetail(
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

    @ApiOperation(value = "获取作业实例基本信息", produces = "application/json")
    @GetMapping("/task_instance/{taskInstanceId}")
    Response<TaskInstanceVO> getTaskInstanceBasic(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam(value = "作业实例ID", name = "taskInstanceId", required = true)
        @PathVariable("taskInstanceId")
            Long taskInstanceId
    );
}
