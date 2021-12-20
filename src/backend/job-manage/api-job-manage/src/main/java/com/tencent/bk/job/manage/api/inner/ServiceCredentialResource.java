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

package com.tencent.bk.job.manage.api.inner;

import com.tencent.bk.job.common.annotation.InternalAPI;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.manage.model.inner.resp.ServiceBasicCredentialDTO;
import com.tencent.bk.job.manage.model.inner.resp.ServiceCredentialDTO;
import com.tencent.bk.job.manage.model.web.request.CredentialCreateUpdateReq;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = {"job-manage:service:Credential"})
@RequestMapping("/service/credentials/app/{appId}")
@RestController
@InternalAPI
public interface ServiceCredentialResource {

    @ApiOperation(value = "获取凭据详情", produces = "application/json")
    @GetMapping("/ids/{id}")
    InternalResponse<ServiceCredentialDTO> getCredentialById(
        @ApiParam(value = "业务ID", required = true)
        @PathVariable("appId") Long appId,
        @ApiParam("凭据ID")
        @PathVariable("id")
            String id
    );

    @ApiOperation(value = "新建凭据", produces = "application/json")
    @PostMapping("/create")
    InternalResponse<ServiceBasicCredentialDTO> createCredential(
        @ApiParam(value = "用户名，网关自动传入", required = true)
        @RequestHeader("username")
            String username,
        @ApiParam(value = "业务ID", required = true)
        @PathVariable("appId") Long appId,
        @ApiParam(value = "创建或更新请求体", required = true)
        @RequestBody
            CredentialCreateUpdateReq createUpdateReq
    );

    @ApiOperation(value = "更新凭据", produces = "application/json")
    @PutMapping("/update")
    InternalResponse<ServiceBasicCredentialDTO> updateCredential(
        @ApiParam(value = "用户名，网关自动传入", required = true)
        @RequestHeader("username")
            String username,
        @ApiParam(value = "业务ID", required = true)
        @PathVariable("appId") Long appId,
        @ApiParam(value = "创建或更新请求体", required = true)
        @RequestBody
            CredentialCreateUpdateReq createUpdateReq
    );
}
