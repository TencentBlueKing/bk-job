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

package com.tencent.bk.job.manage.api.inner.impl;

import com.tencent.bk.job.common.cc.model.AppRoleDTO;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.util.PrefConsts;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.api.inner.ServiceNotificationResource;
import com.tencent.bk.job.manage.model.dto.notify.NotifyEsbChannelDTO;
import com.tencent.bk.job.manage.model.inner.ServiceAppRoleDTO;
import com.tencent.bk.job.manage.model.inner.ServiceNotificationMessage;
import com.tencent.bk.job.manage.model.inner.ServiceNotifyChannelDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTemplateNotificationDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTriggerTemplateNotificationDTO;
import com.tencent.bk.job.manage.model.inner.ServiceUserNotificationDTO;
import com.tencent.bk.job.manage.service.NotifyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
public class ServiceNotificationResourceImpl implements ServiceNotificationResource {
    private final NotifyService notifyService;

    @Autowired
    public ServiceNotificationResourceImpl(NotifyService notifyService) {
        this.notifyService = notifyService;
    }

    @Override
    public InternalResponse<Integer> sendNotificationsToUsers(ServiceUserNotificationDTO serviceUserNotificationDTO) {
        log.info(String.format("Input:%s", JsonUtils.toJson(serviceUserNotificationDTO)));
        return InternalResponse.buildSuccessResp(notifyService.sendNotificationsToUsers(serviceUserNotificationDTO));
    }

    @Override
    public InternalResponse<Integer> sendNotificationsToAdministrators(
        ServiceNotificationMessage serviceNotificationMessage
    ) {
        log.info(String.format("Input:%s", JsonUtils.toJson(serviceNotificationMessage)));
        return InternalResponse.buildSuccessResp(
            notifyService.sendNotificationsToAdministrators(serviceNotificationMessage)
        );
    }

    @Override
    public InternalResponse<Integer> triggerTemplateNotification(
        ServiceTriggerTemplateNotificationDTO triggerTemplateNotification
    ) {
        log.info(String.format("Input:%s", JsonUtils.toJson(triggerTemplateNotification)));
        StopWatch watch = new StopWatch();
        watch.start("triggerTemplateNotification");
        Integer result = notifyService.triggerTemplateNotification(triggerTemplateNotification);
        watch.stop();
        if (watch.getTotalTimeMillis() > 3000) {
            log.warn(PrefConsts.TAG_PREF_SLOW + "triggerTemplateNotification:" + watch.prettyPrint());
        } else if (watch.getTotalTimeMillis() > 1000) {
            log.info(watch.prettyPrint());
        }
        return InternalResponse.buildSuccessResp(result);
    }

    @Override
    public InternalResponse<Integer> sendTemplateNotification(ServiceTemplateNotificationDTO templateNotificationDTO) {
        log.info(String.format("Input:%s", JsonUtils.toJson(templateNotificationDTO)));
        return InternalResponse.buildSuccessResp(notifyService.sendTemplateNotification(templateNotificationDTO));
    }

    @Override
    public InternalResponse<List<ServiceAppRoleDTO>> getNotifyRoles(String lang) {
        log.info(String.format("Input:%s", lang));
        List<AppRoleDTO> roles = notifyService.listRoles();
        List<ServiceAppRoleDTO> result = roles.stream().map(role -> new ServiceAppRoleDTO(role.getId(),
            role.getName())).collect(Collectors.toList());
        return InternalResponse.buildSuccessResp(result);
    }

    @Override
    public InternalResponse<List<ServiceNotifyChannelDTO>> getNotifyChannels(String lang) {
        log.info(String.format("Input:%s", lang));
        List<NotifyEsbChannelDTO> channels = notifyService.listAllNotifyChannel();
        List<ServiceNotifyChannelDTO> result =
            channels.stream().map(channel ->
                new ServiceNotifyChannelDTO(channel.getType(), channel.getLabel())).collect(Collectors.toList());
        return InternalResponse.buildSuccessResp(result);
    }
}
