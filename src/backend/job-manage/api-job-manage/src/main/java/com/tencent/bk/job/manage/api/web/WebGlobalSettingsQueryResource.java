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

import com.tencent.bk.job.common.annotation.CompatibleImplementation;
import com.tencent.bk.job.common.annotation.WebAPI;
import com.tencent.bk.job.common.constant.CompatibleType;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.manage.model.web.vo.globalsetting.AccountNameRulesWithDefaultVO;
import com.tencent.bk.job.manage.model.web.vo.globalsetting.NotifyChannelWithIconVO;
import com.tencent.bk.job.manage.model.web.vo.globalsetting.PlatformInfoVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "job-manage:web:GlobalSettings")
@RequestMapping("/web/queryGlobalSettings")
@RestController
@WebAPI
public interface WebGlobalSettingsQueryResource {

    @Operation(summary = "获取通知渠道列表及生效状态")
    @GetMapping("/notify/listChannels")
    Response<List<NotifyChannelWithIconVO>> listNotifyChannel(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username
    );


    @Operation(summary = "获取账号命名规则")
    @GetMapping("/account/nameRules")
    Response<AccountNameRulesWithDefaultVO> getAccountNameRules(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username
    );

    @Operation(summary = "判断用户是否为超级管理员")
    @GetMapping("/isAdmin")
    Response<Boolean> isAdmin(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username
    );


    @Operation(summary = "获取CMDB服务跳转地址")
    @GetMapping("/cmdbServerUrl")
    Response<String> getCMDBServerUrl(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username
    );


    @Operation(summary = "获取申请业务权限跳转地址")
    @GetMapping("/applyBusinessUrl")
    Response<String> getApplyBusinessUrl(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @Parameter(description = "资源范围类型")
        @RequestParam(value = "scopeType", required = false)
            String scopeType,
        @Parameter(description = "资源范围ID")
        @RequestParam(value = "scopeId", required = false)
            String scopeId
    );


    @Operation(summary = "获取CMDB业务首页地址")
    @GetMapping("/scope/{scopeType}/{scopeId}/cmdbAppIndexUrl")
    Response<String> getCMDBAppIndexUrl(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @Parameter(description = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @Parameter(description = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId
    );


    @CompatibleImplementation(name = "platform_info", deprecatedVersion = "3.11.x", type = CompatibleType.DEPLOY,
        explain = "发布完成后可以删除")
    @Operation(summary = "获取渲染后的平台设置")
    @GetMapping("/platformInfo")
    Response<PlatformInfoVO> getRenderedPlatformInfo();


    @Operation(summary = "获取文档中心根路径")
    @GetMapping("/docCenterBaseUrl")
    Response<String> getDocCenterBaseUrl(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username
    );


    @Operation(summary = "周边系统跳转路径")
    @GetMapping("/relatedSystemUrls")
    Response<Map<String, String>> getRelatedSystemUrls(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username
    );


    @Operation(summary = "作业平台公开配置")
    @GetMapping("/jobConfig")
    Response<Map<String, Object>> getJobConfig(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username
    );

    @Operation(summary = "获取配置的默认时区")
    @GetMapping("/defaultTimezoneConfig")
    Response<Map<String, String>> getDefaultDisplayTimezone(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username
    );
}
