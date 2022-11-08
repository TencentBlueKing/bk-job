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

package com.tencent.bk.job.manage.api.web;

import com.tencent.bk.job.common.annotation.WebAPI;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.manage.model.web.request.AccountCreateUpdateReq;
import com.tencent.bk.job.manage.model.web.vo.AccountVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.validation.annotation.Validated;
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
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;

/**
 * 账号 Web API
 */
@Api(tags = {"job-manage:web:App_Account"})
@RequestMapping("/web/account")
@WebAPI
public interface WebAppAccountResource {

    @ApiOperation(value = "新增账号", produces = "application/json")
    @PostMapping(value = "/scope/{scopeType}/{scopeId}/account")
    Response<Long> saveAccount(
        @ApiParam(value = "用户名，网关自动传入", required = true)
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
        @ApiParam(value = "创建账号请求")
        @RequestBody @Validated
            AccountCreateUpdateReq accountCreateUpdateReq
    );

    @ApiOperation(value = "更新账号", produces = "application/json")
    @PutMapping(value = "/scope/{scopeType}/{scopeId}/account")
    Response updateAccount(
        @ApiParam(value = "用户名，网关自动传入", required = true)
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
        @ApiParam(value = "更新账号请求")
        @RequestBody @Validated
            AccountCreateUpdateReq accountCreateUpdateReq
    );

    @ApiOperation(value = "根据条件获取业务下的所有账号", produces = "application/json")
    @GetMapping({"/scope/{scopeType}/{scopeId}/accounts/page"})
    Response<PageData<AccountVO>> listAppAccounts(
        @ApiParam(value = "用户名，网关自动传入", required = true)
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
        @ApiParam(value = "账号ID：精确搜索（若传入则自动屏蔽其他条件）")
        @RequestParam(value = "id", required = false)
            Long id,
        @ApiParam(value = "账号名称：模糊搜索")
        @RequestParam(value = "account", required = false)
            String name,
        @ApiParam("账号别名：模糊搜索")
        @RequestParam(value = "alias", required = false)
            String alias,
        @ApiParam("账号用途")
        @RequestParam(value = "category", required = false)
            Integer category,
        @ApiParam("账号类型")
        @RequestParam(value = "type", required = false)
            Integer type,
        @ApiParam("创建人：模糊搜索")
        @RequestParam(value = "creator", required = false)
            String creator,
        @ApiParam("更新人：模糊搜索")
        @RequestParam(value = "lastModifyUser", required = false)
            String lastModifyUser,
        @ApiParam("分页-开始")
        @RequestParam(value = "start", required = false)
            Integer start,
        @ApiParam("分页-每页大小,如果不分页那么传入-1")
        @RequestParam(value = "pageSize", required = false)
            Integer pageSize,
        @ApiParam("排序字段,别名:alias,名称:account,更新时间:lastModifyTime")
        @RequestParam(value = "orderField", required = false)
            String orderField,
        @ApiParam("排序顺序,0:降序;1:升序")
        @RequestParam(value = "order", required = false)
            Integer order,
        @ApiParam("关键字：支持账号ID、账号别名、账号名称、创建人、更新人的模糊搜索")
        @RequestParam(value = "keyword", required = false)
            String keyword
    );

    @ApiOperation(value = "删除账号", produces = "application/json")
    @DeleteMapping(value = "/scope/{scopeType}/{scopeId}/account/{accountId}")
    Response deleteAccount(
        @ApiParam(value = "用户名，网关自动传入", required = true)
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
        @ApiParam(value = "账号ID", required = true)
        @PathVariable("accountId")
            Long accountId
    );

    @ApiOperation(value = "获取账号详情", produces = "application/json")
    @GetMapping(value = {"/scope/{scopeType}/{scopeId}/account/{accountId}"})
    Response<AccountVO> getAccountById(
        @ApiParam(value = "用户名，网关自动传入", required = true)
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
        @ApiParam(value = "账号ID", required = true)
        @PathVariable("accountId")
            Long accountId
    );

    @ApiOperation(value = "获取业务下的账号列表，返回简单的账号信息", produces = "application/json")
    @GetMapping({"/scope/{scopeType}/{scopeId}/accounts"})
    Response<List<AccountVO>> listAccounts(
        @ApiParam(value = "用户名，网关自动传入", required = true)
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
        @ApiParam(value = "账号用途,1-系统账号，2-DB账号,不传表示所有用途", required = false)
        @RequestParam(value = "category", required = false)
            Integer category
    );

}
