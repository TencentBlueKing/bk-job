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
import com.tencent.bk.job.execute.model.web.vo.LogExportJobInfoVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.HttpServletResponse;

/**
 * 作业执行日志API-前端调用
 */
@Api(tags = {"job-execute:web:Task_Execution_Log"})
@RequestMapping("/web/execution")
@RestController
@WebAPI
public interface WebTaskLogResource {

    @ApiOperation(value = "请求下载执行日志文件", produces = "application/json")
    @GetMapping("/scope/{scopeType}/{scopeId}/step-execution-result/{stepInstanceId}/log-file")
    Response<LogExportJobInfoVO> requestDownloadLogFile(
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
        @ApiParam(value = "步骤实例 ID", name = "stepInstanceId", required = true)
        @PathVariable("stepInstanceId")
            Long stepInstanceId,
        @ApiParam(value = "hostId", name = "hostId")
        @RequestParam(value = "hostId", required = false)
            Long hostId,
        @ApiParam(value = "ip", name = "ip")
        @RequestParam(value = "ip", required = false)
            String cloudIp,
        @ApiParam(value = "重新打包", name = "repackage")
        @RequestParam(value = "repackage", required = false)
            Boolean repackage
    );

    @ApiOperation(value = "下载执行日志文件", produces = "application/json")
    @GetMapping("/scope/{scopeType}/{scopeId}/step-execution-result/{stepInstanceId}/log-file/download")
    ResponseEntity<StreamingResponseBody> downloadLogFile(
        HttpServletResponse response,
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
        @ApiParam(value = "步骤实例 ID", name = "stepInstanceId", required = true)
        @PathVariable("stepInstanceId")
            Long stepInstanceId,
        @ApiParam(value = "hostId", name = "hostId")
        @RequestParam(value = "hostId", required = false)
            Long hostId,
        @ApiParam(value = "ip", name = "ip")
        @RequestParam(value = "ip", required = false)
            String cloudIp
    );
}
