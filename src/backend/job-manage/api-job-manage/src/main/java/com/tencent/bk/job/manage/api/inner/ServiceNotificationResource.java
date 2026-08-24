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
import com.tencent.bk.job.manage.model.inner.ServiceAppRoleDTO;
import com.tencent.bk.job.manage.model.inner.ServiceNotificationMessage;
import com.tencent.bk.job.manage.model.inner.ServiceNotifyChannelDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTemplateNotificationDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTriggerTemplateNotificationDTO;
import com.tencent.bk.job.manage.model.inner.ServiceUserNotificationDTO;
import com.tentent.bk.job.common.api.feign.annotation.SmartFeignClient;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@Tag(name = "job-manage:service:Notification")
@SmartFeignClient(value = "job-manage", contextId = "notificationResource")
@InternalAPI
public interface ServiceNotificationResource {

    @Operation(summary = "发送通知给用户（渠道在配置文件中配置，默认所有渠道）")
    @PostMapping("/service/notification/sendNotificationsToUsers")
    InternalResponse<Integer> sendNotificationsToUsers(
        @Parameter(description = "通知接受者与消息内容")
        @RequestBody ServiceUserNotificationDTO serviceUserNotificationDTO
    );

    @Operation(summary = "发送通知给管理员（渠道在配置文件中配置，默认所有渠道）")
    @PostMapping("/service/notification/sendNotificationsToAdministrators")
    InternalResponse<Integer> sendNotificationsToAdministrators(
        @Parameter(description = "消息内容")
        @RequestBody ServiceNotificationMessage serviceNotificationMessage
    );

    @Operation(summary = "触发模板消息通知")
    @PostMapping("/service/notification/triggerTemplateNotification")
    InternalResponse<Integer> triggerTemplateNotification(
        @RequestBody ServiceTriggerTemplateNotificationDTO triggerTemplateNotification
    );

    @Operation(summary = "根据模板发送消息通知")
    @PostMapping("/service/notification/sendTemplateNotification")
    InternalResponse<Integer> sendTemplateNotification(
        @Parameter(description = "根据模板发送消息通知")
        @RequestBody ServiceTemplateNotificationDTO templateNotificationDTO
    );

    @Operation(summary = "获取通知角色列表")
    @GetMapping("/service/notification/getNotifyRoles")
    InternalResponse<List<ServiceAppRoleDTO>> getNotifyRoles(
        @Parameter(description = "语言")
        @RequestHeader("lang") String lang
    );

    @Operation(summary = "获取通知渠道")
    @GetMapping("/service/notification/getNotifyChannels")
    InternalResponse<List<ServiceNotifyChannelDTO>> getNotifyChannels(
        @Parameter(description = "语言")
        @RequestHeader("lang") String lang
    );
}
