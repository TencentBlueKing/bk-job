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
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.manage.model.inner.ServiceNotificationDTO;
import com.tencent.bk.job.manage.model.web.request.notify.NotifyPoliciesCreateUpdateReq;
import com.tencent.bk.job.manage.model.web.vo.notify.PageTemplateVO;
import com.tencent.bk.job.manage.model.web.vo.notify.RoleVO;
import com.tencent.bk.job.manage.model.web.vo.notify.TriggerPolicyVO;
import com.tencent.bk.job.manage.model.web.vo.notify.UserVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;

@Api(tags = {"job-manage:web:Notify"})
@RequestMapping("/web/notify")
@RestController
@WebAPI
public interface WebNotifyResource {

    @ApiOperation(value = "获取业务通知策略列表", produces = "application/json")
    @GetMapping("/scope/{scopeType}/{scopeId}/policies/listDefault")
    Response<List<TriggerPolicyVO>> listAppDefaultNotifyPolicies(
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
            String scopeId
    );


    @ApiOperation(value = "保存业务下默认通知策略", produces = "application/json")
    @PostMapping("/scope/{scopeType}/{scopeId}/saveAppDefaultPolicies")
    Response<Long> saveAppDefaultNotifyPolicies(
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
        @ApiParam(value = "创建或更新请求体", required = true)
        @RequestBody
            NotifyPoliciesCreateUpdateReq createUpdateReq
    );


    @ApiOperation(value = "页面模板", produces = "application/json")
    @GetMapping("/pageTemplate")
    Response<PageTemplateVO> getPageTemplate(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username
    );

    @ApiOperation(value = "角色列表", produces = "application/json")
    @GetMapping("/roles/list")
    Response<List<RoleVO>> listRoles(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username
    );

    @ApiOperation(value = "根据用户英文名前缀拉取用户列表（不包括黑名单内用户）", produces = "application/json")
    @GetMapping("/users/list")
    Response<List<UserVO>> listUsers(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiParam("英文名前缀（逗号分隔）")
        @RequestParam(value = "prefixStr", required = false)
            String prefixStr,
        @ApiParam("起始位置")
        @RequestParam(value = "offset", required = false)
            Long offset,
        @ApiParam("拉取数量")
        @RequestParam(value = "limit", required = false)
            Long limit
    );

    @ApiOperation(value = "后台自测调试用接口：发送消息通知", produces = "application/json")
    @PostMapping("/notifications/send")
    Response<?> sendNotification(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @RequestBody
            ServiceNotificationDTO notification
    );
}
