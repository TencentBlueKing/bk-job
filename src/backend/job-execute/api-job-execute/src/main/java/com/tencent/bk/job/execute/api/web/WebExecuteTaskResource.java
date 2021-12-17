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
import com.tencent.bk.job.execute.model.web.request.RedoTaskRequest;
import com.tencent.bk.job.execute.model.web.request.WebFastExecuteScriptRequest;
import com.tencent.bk.job.execute.model.web.request.WebFastPushFileRequest;
import com.tencent.bk.job.execute.model.web.request.WebStepOperation;
import com.tencent.bk.job.execute.model.web.request.WebTaskExecuteRequest;
import com.tencent.bk.job.execute.model.web.vo.StepExecuteVO;
import com.tencent.bk.job.execute.model.web.vo.StepOperationVO;
import com.tencent.bk.job.execute.model.web.vo.TaskExecuteVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 作业执行API-前端调用
 *
 * @date 2019/09/18
 */
@Api(tags = {"job-execute:web:Task_Execute"})
@RequestMapping("/web/execution/")
@RestController
@WebAPI
public interface WebExecuteTaskResource {

    @ApiOperation(value = "执行作业", produces = "application/json")
    @PostMapping("/app/{appId}/task-execution")
    Response<TaskExecuteVO> executeTask(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam(value = "业务ID", required = true, example = "1")
        @PathVariable("appId") Long appId,
        @ApiParam(value = "执行作业请求报文", name = "webTaskExecuteRequest", required = true)
        @RequestBody WebTaskExecuteRequest request);

    @ApiOperation(value = "重新执行作业", produces = "application/json")
    @PostMapping("/app/{appId}/task-execution/redo-task")
    Response<TaskExecuteVO> redoTask(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam(value = "业务ID", required = true, example = "1")
        @PathVariable("appId") Long appId,
        @ApiParam(value = "重新执行作业请求报文", name = "redoTaskRequest", required = true)
        @RequestBody RedoTaskRequest request);

    @ApiOperation(value = "快速执行脚本", produces = "application/json")
    @PostMapping("/app/{appId}/fast-execute-script")
    Response<StepExecuteVO> fastExecuteScript(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam(value = "业务ID", required = true, example = "1")
        @PathVariable("appId") Long appId,
        @ApiParam(value = "快速执行脚本请求报文", name = "webScriptExecuteRequest", required = true)
        @RequestBody @Validated WebFastExecuteScriptRequest request);

    @ApiOperation(value = "快速分发文件", produces = "application/json")
    @PostMapping("/app/{appId}/fast-push-file")
    Response<StepExecuteVO> fastPushFile(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam(value = "业务ID", required = true, example = "1")
        @PathVariable("appId") Long appId,
        @ApiParam(value = "快速分发文件请求报文", name = "webPushFileRequest", required = true)
        @RequestBody WebFastPushFileRequest request);

    @ApiOperation(value = "执行作业步骤操作", produces = "application/json")
    @PostMapping("/app/{appId}/do-step-operation/stepInstanceId/{stepInstanceId}")
    Response<StepOperationVO> doStepOperation(@ApiParam("用户名，网关自动传入")
                                                     @RequestHeader("username") String username,
                                              @ApiParam(value = "业务ID", required = true, example = "1")
                                                     @PathVariable("appId") Long appId,
                                              @ApiParam(value = "步骤实例ID", required = true, example = "1")
                                                     @PathVariable("stepInstanceId") Long stepInstanceId,
                                              @ApiParam(value = "步骤实例操作请求报文", name = "operation", required =
                                                         true)
                                                     @RequestBody WebStepOperation operation);

    @ApiOperation(value = "终止作业", produces = "application/json")
    @PostMapping("/app/{appId}/taskInstance/{taskInstanceId}/terminate")
    Response terminateJob(@ApiParam("用户名，网关自动传入")
                                 @RequestHeader("username") String username,
                          @ApiParam(value = "业务ID", required = true, example = "1")
                                 @PathVariable("appId") Long appId,
                          @ApiParam(value = "作业实例ID", required = true, example = "1")
                                 @PathVariable("taskInstanceId") Long taskInstanceId);

}
