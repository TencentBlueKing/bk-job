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
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.manage.model.inner.ServiceNotificationDTO;
import com.tencent.bk.job.manage.model.web.request.notify.NotifyPoliciesCreateUpdateReq;
import com.tencent.bk.job.manage.model.web.vo.notify.PageTemplateVO;
import com.tencent.bk.job.manage.model.web.vo.notify.RoleVO;
import com.tencent.bk.job.manage.model.web.vo.notify.TriggerPolicyVO;
import com.tencent.bk.job.manage.model.web.vo.notify.UserVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Hidden;

import java.util.List;

@Tag(name = "job-manage:web:Notify")
@RequestMapping("/web/notify")
@RestController
@WebAPI
public interface WebNotifyResource {

    @Operation(summary = "获取业务通知策略列表")
    @GetMapping("/scope/{scopeType}/{scopeId}/policies/listDefault")
    Response<List<TriggerPolicyVO>> listAppDefaultNotifyPolicies(
        @Parameter(description = "用户名，网关自动传入", required = true)
        @RequestHeader("username")
            String username,
        @Hidden
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @Parameter(description = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @Parameter(description = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId
    );


    @Operation(summary = "保存业务下默认通知策略")
    @PostMapping("/scope/{scopeType}/{scopeId}/saveAppDefaultPolicies")
    Response<Long> saveAppDefaultNotifyPolicies(
        @Parameter(description = "用户名，网关自动传入", required = true)
        @RequestHeader("username")
            String username,
        @Hidden
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @Parameter(description = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @Parameter(description = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @Parameter(description = "创建或更新请求体", required = true)
        @RequestBody
            NotifyPoliciesCreateUpdateReq createUpdateReq
    );


    @Operation(summary = "页面模板")
    @GetMapping("/pageTemplate")
    Response<PageTemplateVO> getPageTemplate(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username
    );

    @Operation(summary = "角色列表")
    @GetMapping("/roles/list")
    Response<List<RoleVO>> listRoles(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username
    );

    @Operation(summary = "后台自测调试用接口：发送消息通知")
    @PostMapping("/notifications/send")
    Response<?> sendNotification(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @RequestBody
            ServiceNotificationDTO notification
    );
}
