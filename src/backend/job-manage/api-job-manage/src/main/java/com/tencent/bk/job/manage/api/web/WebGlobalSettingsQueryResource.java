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
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.manage.model.web.vo.globalsetting.AccountNameRulesWithDefaultVO;
import com.tencent.bk.job.manage.model.web.vo.globalsetting.NotifyChannelWithIconVO;
import com.tencent.bk.job.manage.model.web.vo.globalsetting.PlatformInfoVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Api(tags = {"job-manage:web:GlobalSettings"})
@RequestMapping("/web/queryGlobalSettings")
@RestController
@WebAPI
public interface WebGlobalSettingsQueryResource {

    @ApiOperation(value = "获取通知渠道列表及生效状态", produces = "application/json")
    @GetMapping("/notify/listChannels")
    Response<List<NotifyChannelWithIconVO>> listNotifyChannel(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username
    );


    @ApiOperation(value = "获取账号命名规则", produces = "application/json")
    @GetMapping("/account/nameRules")
    Response<AccountNameRulesWithDefaultVO> getAccountNameRules(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username
    );

    @ApiOperation(value = "判断用户是否为超级管理员", produces = "application/json")
    @GetMapping("/isAdmin")
    Response<Boolean> isAdmin(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username
    );


    @ApiOperation(value = "获取CMDB服务跳转地址", produces = "application/json")
    @GetMapping("/cmdbServerUrl")
    Response<String> getCMDBServerUrl(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username
    );


    @ApiOperation(value = "获取申请业务权限跳转地址", produces = "application/json")
    @GetMapping("/applyBusinessUrl")
    Response<String> getApplyBusinessUrl(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiParam(value = "资源范围类型")
        @RequestParam(value = "scopeType", required = false)
            String scopeType,
        @ApiParam(value = "资源范围ID")
        @RequestParam(value = "scopeId", required = false)
            String scopeId
    );


    @ApiOperation(value = "获取CMDB业务首页地址", produces = "application/json")
    @GetMapping("/scope/{scopeType}/{scopeId}/cmdbAppIndexUrl")
    Response<String> getCMDBAppIndexUrl(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiParam(value = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @ApiParam(value = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId
    );


    @ApiOperation(value = "获取渲染后的平台设置", produces = "application/json")
    @GetMapping("/platformInfo")
    Response<PlatformInfoVO> getRenderedPlatformInfo();


    @ApiOperation(value = "获取文档中心根路径", produces = "application/json")
    @GetMapping("/docCenterBaseUrl")
    Response<String> getDocCenterBaseUrl(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username
    );


    @ApiOperation(value = "周边系统跳转路径", produces = "application/json")
    @GetMapping("/relatedSystemUrls")
    Response<Map<String, String>> getRelatedSystemUrls(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username
    );


    @ApiOperation(value = "作业平台公开配置", produces = "application/json")
    @GetMapping("/jobConfig")
    Response<Map<String, Object>> getJobConfig(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username
    );
}
