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

package com.tencent.bk.job.ticket.api.web;

import com.tencent.bk.job.common.model.ServiceResponse;
import com.tencent.bk.job.common.model.permission.AuthResultVO;
import com.tencent.bk.job.ticket.model.web.req.OperationPermissionReq;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;

@Api(tags = {"job-ticket:web:Permission"})
@RequestMapping("/web/permission/")
@RestController
public interface WebPermissionResource {
    /**
     * 获取权限申请URL
     *
     * @param username 用户名
     * @param req      操作鉴权请求
     * @return 权限申请URL
     */
    @ApiOperation(value = "获取权限申请URL", produces = "application/json")
    @PostMapping("/apply-url")
    ServiceResponse<String> getApplyUrl(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam("权限检查请求")
        @RequestBody OperationPermissionReq req
    );

    /**
     * 检查操作权限
     *
     * @param username 用户名
     * @param req      操作鉴权请求
     * @return
     */
    @ApiOperation(value = "检查操作权限", produces = "application/json")
    @PostMapping("/check")
    ServiceResponse<AuthResultVO> checkOperationPermission(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam("权限检查请求")
        @RequestBody OperationPermissionReq req
    );

    /**
     * 检查操作权限
     *
     * @param username               用户名
     * @param appId                  业务ID
     * @param operation              操作ID
     * @param resourceId             资源ID
     * @param returnPermissionDetail 是否返回详细的权限信息
     * @return
     */
    @ApiOperation(value = "检查操作权限", produces = "application/json")
    @GetMapping("/check")
    ServiceResponse<AuthResultVO> checkOperationPermission(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam(value = "业务ID", required = false)
        @RequestParam(value = "appId", required = false) Long appId,
        @ApiParam("操作ID,取值为: [ticket/create,ticket/edit," + "ticket/delete,ticket/use]")
        @RequestParam(value = "operation") String operation,
        @ApiParam(value = "资源ID,比如凭证ID;对于部分不需要资源ID的操作(新建),不需要传参", required = false)
        @RequestParam(value = "resourceId", required = false) String resourceId,
        @ApiParam(value = "是否返回详细的权限信息(依赖的权限，申请URL)。默认为false", required = false)
        @RequestParam(value = "returnPermissionDetail", required = false) Boolean returnPermissionDetail
    );

}
