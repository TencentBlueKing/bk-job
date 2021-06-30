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

package com.tencent.bk.job.manage.api.op;

import com.tencent.bk.job.common.model.ServiceResponse;
import com.tencent.bk.job.manage.model.op.req.ConfigFlowControlReq;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Api(tags = {"job-manage:api:FlowControl-OP"})
@RequestMapping("/op/flowControl")
@RestController
public interface FlowControlOpResource {

    @ApiOperation(value = "查询当前流控配置", produces = "application/json")
    @GetMapping("/config")
    ServiceResponse<Map<String, Long>> getCurrentFlowControlConfig(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username
    );

    @ApiOperation(value = "更新流控配置", produces = "application/json")
    @PostMapping("/config")
    ServiceResponse<Integer> configFlowControl(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam(value = "流控配置参数", required = true) @RequestBody ConfigFlowControlReq req
    );

    @ApiOperation(value = "查询当前所有资源使用速率", produces = "application/json")
    @GetMapping("/currentRateMap")
    ServiceResponse<Map<String, Long>> getCurrentRateMap(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username
    );

    @ApiOperation(value = "查询当前某资源使用速率", produces = "application/json")
    @GetMapping("/currentRate")
    ServiceResponse<Long> getCurrentRate(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam(value = "资源Id") @RequestParam(value = "resourceId", required = true) String resourceId
    );

}
