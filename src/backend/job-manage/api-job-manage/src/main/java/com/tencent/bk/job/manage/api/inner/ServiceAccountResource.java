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

package com.tencent.bk.job.manage.api.inner;

import com.tencent.bk.job.common.annotation.InternalAPI;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.manage.model.inner.ServiceAccountDTO;
import com.tencent.bk.job.manage.model.web.request.AccountCreateUpdateReq;
import com.tentent.bk.job.common.api.feign.annotation.SmartFeignClient;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 账号服务
 */
@Api(tags = {"job-manage:service:Account_Management"})
@SmartFeignClient(value = "job-manage", contextId = "accountResource")
@InternalAPI
public interface ServiceAccountResource {
    /**
     * 根据账号id获取账号信息
     *
     * @param accountId
     * @return
     */
    @GetMapping("/service/account/{accountId}")
    @ApiOperation(value = "根据账号id获取账号信息", produces = "application/json")
    InternalResponse<ServiceAccountDTO>
    getAccountByAccountId(@ApiParam(value = "账号ID", required = true) @PathVariable("accountId") Long accountId);

    /**
     * 根据账号id获取账号信息
     *
     * @param appId
     * @param account
     * @return
     */
    @GetMapping("/service/account/app/{appId}/accounts/{account}")
    @ApiOperation(value = "根据账号名获取账号信息", produces = "application/json")
    InternalResponse<ServiceAccountDTO>
    getAccountByAccountName(
        @ApiParam(value = "业务ID", required = true) @PathVariable("appId") Long appId,
        @ApiParam(value = "账号名", required = true) @PathVariable("account") String account
    );

    /**
     * 根据业务和账号名称获取账号信息
     *
     * @param appId
     * @param category
     * @param alias
     * @return
     */
    @GetMapping("/service/account/app/{appId}/category/{category}/alias/{alias}")
    @ApiOperation(value = "根据账号别名获取业务下的账号信息", produces = "application/json")
    InternalResponse<ServiceAccountDTO> getAccountByCategoryAndAliasInApp(
        @ApiParam(value = "业务ID", required = true) @PathVariable("appId") Long appId,
        @ApiParam(value = "账号用途，1-系统账号，2-DB账号", required = true) @PathVariable("category") Integer category,
        @ApiParam(value = "账号名称", required = true) @PathVariable("alias") String alias);

    @PostMapping("/service/account/app/{appId}/saveOrGetAccount")
    InternalResponse<ServiceAccountDTO> saveOrGetAccount(
        @ApiParam("用户名，网关自动传入") @RequestHeader("username") String username,
        @ApiParam("创建时间") @RequestHeader(value = "X-Create-Time", required = false) Long createTime,
        @ApiParam("修改时间") @RequestHeader(value = "X-Update-Time", required = false) Long lastModifyTime,
        @ApiParam("最后修改人") @RequestHeader(value = "X-Update-User", required = false) String lastModifyUser,
        @PathVariable("appId") Long appId, @RequestBody AccountCreateUpdateReq accountCreateUpdateReq);

    @ApiOperation(value = "新增账号", produces = "application/json")
    @PostMapping(value = "/service/account/app/{appId}/account")
    Response<Long> saveAccount(
        @ApiParam(value = "用户名，网关自动传入", required = true)
        @RequestHeader("username")
            String username,
        @PathVariable("appId")
            Long appId,
        @ApiParam(value = "创建账号请求")
        @RequestBody
            AccountCreateUpdateReq accountCreateUpdateReq
    );

    @ApiOperation(value = "获取业务下的账号列表，返回简单的账号信息", produces = "application/json")
    @GetMapping("/service/account/account/app/{appId}/accounts")
    Response<List<ServiceAccountDTO>> listAccounts(
        @ApiParam(value = "业务ID", required = true)
        @PathVariable("appId")
            Long appId,
        @ApiParam(value = "账号用途,1-系统账号，2-DB账号,不传表示所有用途")
        @RequestParam(value = "category", required = false)
            Integer category
    );
}
