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
import com.tencent.bk.job.common.constant.JobCommonHeaders;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.dto.notify.CustomNotifyDTO;
import com.tencent.bk.job.manage.model.inner.ServiceSpecificResourceNotifyPolicyDTO;
import com.tencent.bk.job.manage.model.inner.ServiceAppRoleDTO;
import com.tencent.bk.job.manage.model.inner.ServiceNotifyChannelDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTemplateNotificationDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTriggerTemplateNotificationDTO;
import com.tentent.bk.job.common.api.feign.annotation.SmartFeignClient;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@Api(tags = {"job-manage:service:Notification"})
@SmartFeignClient(value = "job-manage", contextId = "notificationResource")
@InternalAPI
public interface ServiceNotificationResource {

    @ApiOperation(value = "触发模板消息通知", produces = "application/json")
    @PostMapping("/service/notification/triggerTemplateNotification")
    InternalResponse<Integer> triggerTemplateNotification(
        @RequestBody ServiceTriggerTemplateNotificationDTO triggerTemplateNotification
    );

    @ApiOperation(value = "根据模板发送消息通知", produces = "application/json")
    @PostMapping("/service/notification/sendTemplateNotification")
    InternalResponse<Integer> sendTemplateNotification(
        @ApiParam("根据模板发送消息通知")
        @RequestBody ServiceTemplateNotificationDTO templateNotificationDTO
    );

    @ApiOperation(value = "获取通知角色列表", produces = "application/json")
    @GetMapping("/service/notification/getNotifyRoles")
    InternalResponse<List<ServiceAppRoleDTO>> getNotifyRoles(
        @ApiParam("租户ID")
        @RequestHeader(value = JobCommonHeaders.BK_TENANT_ID, required = false)
        String tenantId,
        @ApiParam("语言")
        @RequestHeader("lang")
        String lang
    );

    @ApiOperation(value = "获取通知渠道", produces = "application/json")
    @GetMapping("/service/notification/getNotifyChannels")
    InternalResponse<List<ServiceNotifyChannelDTO>> getNotifyChannels(
        @ApiParam("语言")
        @RequestHeader("lang")
        String lang,
        @ApiParam("租户ID")
        @RequestHeader("tenant_id")
        String tenantId
    );

    @ApiOperation(value = "创建或更新特定资源消息通知策略", produces = "application/json")
    @PostMapping("/service/notification/notifyPolicy")
    InternalResponse<Boolean> createOrUpdateSpecificResourceNotifyPolicy(
        @ApiParam("操作人")
        @RequestHeader("username")
        String username,
        @ApiParam("业务id")
        @RequestHeader("appId")
        Long appId,
        @ApiParam("消息通知策略")
        @RequestBody
        ServiceSpecificResourceNotifyPolicyDTO serviceNotifyPolicyDTO
    );

    @ApiOperation(value = "删除特定资源通知策略", produces = "application/json")
    @DeleteMapping("/service/notification/resourceNotifyPolicy/resourceType/{resourceType}/resourceId/{resourceId}")
    InternalResponse<Integer> deleteSpecificResourceNotifyPolicy(
        @ApiParam("业务id")
        @RequestHeader("appId")
        Long appId,
        @ApiParam("资源类型")
        @PathVariable("resourceType")
        Integer resourceType,
        @ApiParam("资源id")
        @PathVariable("resourceId")
        String resourceId
    );

    @ApiOperation(value = "获取特定资源通知策略", produces = "application/json")
    @GetMapping("/service/notification/resourceNotifyPolicy/resourceType/{resourceType}/resourceId/{resourceId}"
        + "/triggerType/{triggerType}")
    InternalResponse<CustomNotifyDTO> getSpecificResourceNotifyPolicy(
        @ApiParam("业务id")
        @RequestHeader("appId")
        Long appId,
        @ApiParam("资源类型")
        @PathVariable("resourceType")
        Integer resourceType,
        @ApiParam("资源id")
        @PathVariable("resourceId")
        String resourceId,
        @ApiParam("触发类型")
        @PathVariable("triggerType")
        Integer triggerType
    );

}
