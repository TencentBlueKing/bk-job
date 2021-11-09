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
import com.tencent.bk.job.manage.model.inner.ServiceAppRoleDTO;
import com.tencent.bk.job.manage.model.inner.ServiceNotificationMessage;
import com.tencent.bk.job.manage.model.inner.ServiceNotifyChannelDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTemplateNotificationDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTriggerTemplateNotificationDTO;
import com.tencent.bk.job.manage.model.inner.ServiceUserNotificationDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Api(tags = {"job-manage:service:Notification"})
@RequestMapping("/service/notification")
@RestController
@InternalAPI
public interface ServiceNotificationResource {

    @ApiOperation(value = "发送通知给用户（渠道在配置文件中配置，默认所有渠道）", produces = "application/json")
    @PostMapping("/sendNotificationsToUsers")
    InternalResponse<Integer> sendNotificationsToUsers(
        @ApiParam("通知接受者与消息内容")
        @RequestBody ServiceUserNotificationDTO serviceUserNotificationDTO
    );

    @ApiOperation(value = "发送通知给管理员（渠道在配置文件中配置，默认所有渠道）", produces = "application/json")
    @PostMapping("/sendNotificationsToAdministrators")
    InternalResponse<Integer> sendNotificationsToAdministrators(
        @ApiParam("消息内容")
        @RequestBody ServiceNotificationMessage serviceNotificationMessage
    );

    @ApiOperation(value = "触发模板消息通知", produces = "application/json")
    @PostMapping("/triggerTemplateNotification")
    InternalResponse<Integer> triggerTemplateNotification(
        @RequestBody ServiceTriggerTemplateNotificationDTO triggerTemplateNotification
    );

    @ApiOperation(value = "根据模板发送消息通知", produces = "application/json")
    @PostMapping("/sendTemplateNotification")
    InternalResponse<Integer> sendTemplateNotification(
        @ApiParam("根据模板发送消息通知")
        @RequestBody ServiceTemplateNotificationDTO templateNotificationDTO
    );

    @ApiOperation(value = "获取通知角色列表", produces = "application/json")
    @GetMapping("/getNotifyRoles")
    InternalResponse<List<ServiceAppRoleDTO>> getNotifyRoles(
        @ApiParam("语言")
        @RequestHeader("lang") String lang
    );

    @ApiOperation(value = "获取通知渠道", produces = "application/json")
    @GetMapping("/getNotifyChannels")
    InternalResponse<List<ServiceNotifyChannelDTO>> getNotifyChannels(
        @ApiParam("语言")
        @RequestHeader("lang") String lang
    );
}
