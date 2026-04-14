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

package com.tencent.bk.job.manage.api.web;

import com.tencent.bk.job.common.annotation.WebAPI;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.manage.model.web.request.CredentialCreateUpdateReq;
import com.tencent.bk.job.manage.model.web.vo.CredentialBasicVO;
import com.tencent.bk.job.manage.model.web.vo.CredentialVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "job-ticket:web:Credential")
@RequestMapping("/web/credentials/scope/{scopeType}/{scopeId}")
@RestController
@WebAPI
public interface WebCredentialResource {

    @Operation(summary = "获取凭证列表")
    @GetMapping("/list")
    Response<PageData<CredentialVO>> listCredentials(
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
        @Parameter(description = "搜索条件：凭证ID")
        @RequestParam(value = "id", required = false)
            String id,
        @Parameter(description = "搜索条件：凭证名称")
        @RequestParam(value = "name", required = false)
            String name,
        @Parameter(description = "搜索条件：描述")
        @RequestParam(value = "description", required = false)
            String description,
        @Parameter(description = "搜索条件：创建人")
        @RequestParam(value = "creator", required = false)
            String creator,
        @Parameter(description = "搜索条件：更新人")
        @RequestParam(value = "lastModifyUser", required = false)
            String lastModifyUser,
        @Parameter(description = "分页-开始")
        @RequestParam(value = "start", required = false)
            Integer start,
        @Parameter(description = "分页-每页大小")
        @RequestParam(value = "pageSize", required = false)
            Integer pageSize
    );

    @Operation(summary = "分页获取凭证基础信息")
    @GetMapping("/basicInfo/list")
    Response<PageData<CredentialBasicVO>> listCredentialBasicInfo(
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
        @Parameter(description = "分页-开始，不传默认为0")
        @RequestParam(value = "start", required = false)
            Integer start,
        @Parameter(description = "分页-每页大小，不传默认拉取全量数据")
        @RequestParam(value = "pageSize", required = false)
            Integer pageSize
    );

    @Operation(summary = "检查凭证名称是否可用")
    @GetMapping("/{credentialId}/check_name")
    Response<Boolean> checkCredentialName(
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
        @Parameter(description = "凭证ID，新建时填0", required = true)
        @PathVariable(value = "credentialId")
            String credentialId,
        @Parameter(description = "名称", required = true)
        @RequestParam(value = "name")
            String name
    );

    @Operation(summary = "新增凭证")
    @PostMapping
    Response<CredentialVO> createCredential(
        @Parameter(description = "用户名，网关自动传入", required = true)
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
        @Parameter(description = "创建请求体", required = true)
        @RequestBody
            CredentialCreateUpdateReq createUpdateReq
    );

    @Operation(summary = "更新凭证")
    @PutMapping("/{credentialId}")
    Response<CredentialVO> updateCredential(
        @Parameter(description = "用户名，网关自动传入", required = true)
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
        @Parameter(description = "资源范围ID", required = true)
        @PathVariable(value = "credentialId")
            String credentialId,
        @Parameter(description = "更新请求体", required = true)
        @RequestBody
            CredentialCreateUpdateReq createUpdateReq
    );


    @Operation(summary = "删除凭证")
    @DeleteMapping("/ids/{id}")
    Response<Integer> deleteCredentialById(
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
        @Parameter(description = "凭证ID")
        @PathVariable("id")
            String id
    );
}
