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

package com.tencent.bk.job.crontab.service.impl;

import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.crontab.client.ServiceNotificationResourceClient;
import com.tencent.bk.job.crontab.model.dto.CronJobInfoDTO;
import com.tencent.bk.job.crontab.service.NotifyService;
import com.tencent.bk.job.manage.model.inner.ServiceTemplateNotificationDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @since 29/4/2020 15:52
 */
@Slf4j
@Service
public class NotifyServiceImpl implements NotifyService {

    private final ServiceNotificationResourceClient notificationClient;

    @Autowired
    public NotifyServiceImpl(ServiceNotificationResourceClient notificationClient) {
        this.notificationClient = notificationClient;
    }

    @Override
    public Integer sendCronJobNotification(CronJobInfoDTO cronJobInfo) {
        InternalResponse<Integer> sendNotifyResponse =
            notificationClient.sendTemplateNotification(CronJobInfoDTO.buildNotifyInfo(cronJobInfo));
        if (sendNotifyResponse != null) {
            if (sendNotifyResponse.isSuccess()) {
                if (sendNotifyResponse.getData() != null) {
                    return sendNotifyResponse.getData();
                } else {
                    log.warn("Send notification get empty response!|{}", sendNotifyResponse);
                }
            } else {
                log.warn("Error while send notification!|{}", sendNotifyResponse);
            }
        } else {
            log.warn("Send notification get no response!");
        }
        return -1;
    }

    @Override
    public Integer sendCronJobFailedNotification(Integer errorCode, String errorMessage, CronJobInfoDTO cronJobInfo) {
        ServiceTemplateNotificationDTO templateNotificationRequest = CronJobInfoDTO.buildFailedNotifyInfo(cronJobInfo);
        if (errorCode != null) {
            templateNotificationRequest.getVariablesMap().put("task.error_code", errorCode.toString());
        } else {
            templateNotificationRequest.getVariablesMap().put("task.error_code", "NaN");
        }
        if (errorMessage != null) {
            templateNotificationRequest.getVariablesMap().put("task.error_message", errorMessage);
        } else {
            templateNotificationRequest.getVariablesMap().put("task.error_message", "Null");
        }

        InternalResponse<Integer> sendNotifyResponse =
            notificationClient.sendTemplateNotification(templateNotificationRequest);
        if (sendNotifyResponse != null) {
            if (sendNotifyResponse.isSuccess()) {
                if (sendNotifyResponse.getData() != null) {
                    return sendNotifyResponse.getData();
                } else {
                    log.warn("Send notification get empty response!|{}", sendNotifyResponse);
                }
            } else {
                log.warn("Error while send notification!|{}", sendNotifyResponse);
            }
        } else {
            log.warn("Send notification get no response!");
        }
        return -1;
    }
}
