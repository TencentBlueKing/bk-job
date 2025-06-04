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

import com.tencent.bk.job.crontab.dao.CronJobDAO;
import com.tencent.bk.job.crontab.model.dto.CronJobInfoDTO;
import com.tencent.bk.job.crontab.service.CustomNotifyPolicyService;
import com.tencent.bk.job.manage.api.common.constants.notify.ExecuteStatusEnum;
import com.tencent.bk.job.manage.api.common.constants.notify.ResourceTypeEnum;
import com.tencent.bk.job.manage.api.common.constants.notify.TriggerTypeEnum;
import com.tencent.bk.job.manage.api.inner.ServiceNotificationResource;
import com.tencent.bk.job.manage.model.inner.ServiceSpecificResourceNotifyPolicyDTO;
import com.tencent.bk.job.manage.model.web.request.notify.ResourceStatusChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CustomNotifyPolicyServiceImpl implements CustomNotifyPolicyService {

    private final ThreadPoolExecutor asyncCustomNotifyPolicyExecutor;
    private final CronJobDAO cronJobDAO;
    private final ServiceNotificationResource notificationResource;

    @Autowired
    public CustomNotifyPolicyServiceImpl(
        @Qualifier("asyncCustomNotifyPolicyExecutor")
        ThreadPoolExecutor asyncCustomNotifyPolicyExecutor,
        CronJobDAO cronJobDAO,
        ServiceNotificationResource notificationResource
    ) {
        this.asyncCustomNotifyPolicyExecutor = asyncCustomNotifyPolicyExecutor;
        this.cronJobDAO = cronJobDAO;
        this.notificationResource = notificationResource;
    }

    @Override
    public void createOrUpdateCronJobCustomNotifyPolicy(Long cronJobId) {
        SyncCustomNotifyPolicyTask task = new SyncCustomNotifyPolicyTask(cronJobId);
        asyncCustomNotifyPolicyExecutor.execute(task);
    }

    @Override
    public void deleteCronJobCustomNotifyPolicy(Long appId, Long cronJobId) {
        DeleteCustomNotifyPolicyTask task = new DeleteCustomNotifyPolicyTask(appId, cronJobId);
        asyncCustomNotifyPolicyExecutor.execute(task);
    }

    class SyncCustomNotifyPolicyTask implements Runnable {

        private final Long cronJobId;

        SyncCustomNotifyPolicyTask(Long cronJobId) {
            this.cronJobId = cronJobId;
        }

        @Override
        public void run() {
            CronJobInfoDTO cronJobInfoDTO = cronJobDAO.getCronJobById(cronJobId);
            if (cronJobInfoDTO == null) {
                log.error("[asyncCustomNotifyPolicy]aim to async custom notify "
                    + "policy with id:{} fail, cron job does not exist", cronJobId);
                return;
            }

            ServiceSpecificResourceNotifyPolicyDTO specificResourceNotifyPolicy =
                new ServiceSpecificResourceNotifyPolicyDTO();
            specificResourceNotifyPolicy.setAppId(cronJobInfoDTO.getAppId());
            specificResourceNotifyPolicy.setTriggerType(TriggerTypeEnum.TIMER_TASK);
            specificResourceNotifyPolicy.setResourceType(ResourceTypeEnum.CRON.getType());
            specificResourceNotifyPolicy.setResourceId(cronJobInfoDTO.getId());
            if (cronJobInfoDTO.getCustomCronJobNotifyDTO() != null) {
                specificResourceNotifyPolicy.setRoleList(cronJobInfoDTO.getCustomCronJobNotifyDTO().getRoleList());
                specificResourceNotifyPolicy.setExtraObserverList(
                    cronJobInfoDTO.getCustomCronJobNotifyDTO().getExtraObserverList()
                );
                specificResourceNotifyPolicy.setResourceStatusChannelList(
                    cronJobInfoDTO.getCustomCronJobNotifyDTO().getCustomNotifyChannel().stream()
                        .map(cronJobStatusNotifyChannel -> new ResourceStatusChannel(
                            ExecuteStatusEnum.get(cronJobStatusNotifyChannel.getExecuteStatus().getValue()),
                            cronJobStatusNotifyChannel.getChannelList()
                        )
                    ).collect(Collectors.toList())
                );
            }
            notificationResource.createOrUpdateSpecificResourceNotifyPolicy(
                cronJobInfoDTO.getLastModifyUser(),
                cronJobInfoDTO.getAppId(),
                specificResourceNotifyPolicy);
        }
    }

    class DeleteCustomNotifyPolicyTask implements Runnable {

        private final Long appId;
        private final Long cronJobId;

        DeleteCustomNotifyPolicyTask(Long appId, Long cronJobId) {
            this.appId = appId;
            this.cronJobId = cronJobId;
        }

        @Override
        public void run() {
            log.info("try to delete custom notify policy by async with cron task id:{}", cronJobId);
            notificationResource.deleteSpecificResourceNotifyPolicy(appId, ResourceTypeEnum.CRON.getType(),
                String.valueOf(cronJobId));
        }
    }
}
